#!/usr/bin/env bash
# Extracts Parasoft coverage agent jars and configures agent.properties for all
# four instrumented microservices. Safe to run multiple times (idempotent).
# Used by both Jenkinsfile.deploy and local developers.

set -o errexit
set -o errtrace
set -o nounset
set -o pipefail

# ─── Constants ────────────────────────────────────────────────────────────────

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_ROOT="$( cd "${SCRIPT_DIR}/.." && pwd )"
TEMPLATE="${REPO_ROOT}/jtest/coverage/agent.properties"
CTP_IMAGE="parasoft/ctp:latest"
CTP_JAR_PATH="/usr/local/parasoft/ctp/webapps/em/coverage/Java/jtest_agent"

# Maps "service-dir:CTP component name" — must match CTP environment configuration
SERVICES=(
    "spring-petclinic-api-gateway:UI + API Gateway"
    "spring-petclinic-customers-service:customers-service REST API"
    "spring-petclinic-vets-service:vets-service REST API"
    "spring-petclinic-visits-service:visits-service REST API"
)

# ─── Defaults ─────────────────────────────────────────────────────────────────

CTP_URL=""
CTP_ENV_ID=""
DTP_URL=""
BUILD_ID="baseline"
APP_NAME="spring-petclinic-microservices"
SKIP_JARS=false
USE_CTP_WS_URL=false
CI_DEBUG=false

# ─── Usage ────────────────────────────────────────────────────────────────────

usage() {
    cat <<EOF
Usage: $(basename "$0") [options]

Options:
  --ctp-url URL     CTP base URL (e.g. http://ctp:8080). Required with --env-id.
  --env-id  ID      CTP environment ID. Required with --ctp-url.
  --dtp-url URL     DTP base URL (e.g. http://dtp:8083).
  --build-id ID     Build identifier written to dtp.buildID (default: baseline).
  --app-name NAME   DTP project name (default: spring-petclinic-microservices).
  --skip-jars          Skip jar extraction from the CTP Docker image.
  --use-ctp-ws-url     Patch ctp.websocket.url from the CTP API response.
                       Use when CTP is on a remote host (e.g. Jenkins). Omit
                       for local Docker deployments where the template value is correct.
  --ci-debug           Enable verbose output including patched properties files.
  -h, --help        Show this help.

Environment variables (required when --ctp-url or --dtp-url are supplied):
  PARASOFT_USER     CTP/DTP username
  PARASOFT_PASS     CTP/DTP password

Examples:
  # Jars only — no CTP/DTP API calls (useful for initial local setup)
  ./scripts/setup-coverage.sh

  # Full setup with CTP subscription queues and DTP filter ID resolved automatically
  ./scripts/setup-coverage.sh \\
      --ctp-url http://ctp:8080 --env-id 4 \\
      --dtp-url http://dtp:8083 --build-id baseline

  # Jenkins usage (credentials injected via withCredentials)
  ./scripts/setup-coverage.sh \\
      --ctp-url "\${CTP_URL}" --env-id "\${CTP_ENV_ID}" \\
      --dtp-url "\${DTP_URL}" --build-id "\${BUILD_ID}" \\
      --app-name "\${app_name}"
EOF
    exit 0
}

# ─── Argument parsing ─────────────────────────────────────────────────────────

while [[ $# -gt 0 ]]; do
    case "$1" in
        --ctp-url)   CTP_URL="${2%/}";  shift 2 ;;
        --env-id)    CTP_ENV_ID="$2";   shift 2 ;;
        --dtp-url)   DTP_URL="${2%/}";  shift 2 ;;
        --build-id)  BUILD_ID="$2";     shift 2 ;;
        --app-name)  APP_NAME="$2";     shift 2 ;;
        --skip-jars)       SKIP_JARS=true;        shift ;;
        --use-ctp-ws-url)  USE_CTP_WS_URL=true;   shift ;;
        --ci-debug)        CI_DEBUG=true;          shift ;;
        -h|--help)   usage ;;
        *) echo "ERROR: Unknown option: $1" >&2; usage ;;
    esac
done

# ─── Validation ───────────────────────────────────────────────────────────────

if [[ -n "${CTP_URL}" && -z "${CTP_ENV_ID}" ]] || [[ -z "${CTP_URL}" && -n "${CTP_ENV_ID}" ]]; then
    echo "ERROR: --ctp-url and --env-id must be provided together." >&2
    exit 1
fi

if [[ -n "${CTP_URL}" || -n "${DTP_URL}" ]]; then
    if [[ -z "${PARASOFT_USER:-}" || -z "${PARASOFT_PASS:-}" ]]; then
        echo "ERROR: PARASOFT_USER and PARASOFT_PASS env vars must be set when using CTP/DTP APIs." >&2
        exit 1
    fi
fi

if [[ "${SKIP_JARS}" == "false" ]] && ! command -v docker &>/dev/null; then
    echo "ERROR: docker is required for jar extraction. Use --skip-jars to skip." >&2
    exit 1
fi

if [[ -n "${CTP_URL}" || -n "${DTP_URL}" ]] && ! command -v jq &>/dev/null; then
    echo "ERROR: jq is required for CTP/DTP API response parsing." >&2
    exit 1
fi

if [[ ! -f "${TEMPLATE}" ]]; then
    echo "ERROR: Template not found at ${TEMPLATE}." >&2
    exit 1
fi

# ─── Helpers ──────────────────────────────────────────────────────────────────

log()   { echo "[setup-coverage] $*"; }
debug() { [[ "${CI_DEBUG}" == "true" ]] && echo "[debug] $*" || true; }

# Wrapper that suppresses xtrace for curl (avoids leaking credentials in CI logs)
curl_api() {
    local url="$1"
    { set +x; } 2>/dev/null
    curl -sk -u "${PARASOFT_USER}:${PARASOFT_PASS}" -H 'Accept: application/json' "$url"
    [[ "${CI_DEBUG}" == "true" ]] && set -x || true
}

# Cross-platform sed -i (BSD/macOS requires an explicit backup extension)
sed_inplace() {
    if [[ "$(uname)" == "Darwin" ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

cd "${REPO_ROOT}"

# ─── Step 1: Extract jars from CTP Docker image ───────────────────────────────

CONTAINER=""
cleanup() { [[ -n "${CONTAINER}" ]] && docker rm "${CONTAINER}" >/dev/null 2>&1 || true; }
trap cleanup EXIT

if [[ "${SKIP_JARS}" == "false" ]]; then
    log "Pulling coverage agent jars from ${CTP_IMAGE}..."
    CONTAINER=$(docker create "${CTP_IMAGE}")
    for entry in "${SERVICES[@]}"; do
        service="${entry%%:*}"
        coverage_dir="${service}/src/test/resources/coverage"
        mkdir -p "${coverage_dir}"
        docker cp "${CONTAINER}:${CTP_JAR_PATH}/." "${coverage_dir}/"
        log "  Jars → ${coverage_dir}/"
    done
    docker rm "${CONTAINER}" >/dev/null 2>&1
    CONTAINER=""
fi

# ─── Step 2: Resolve CTP component settings ───────────────────────────────────

COMPONENTS_JSON=""
if [[ -n "${CTP_URL}" ]]; then
    log "Fetching CTP environment ${CTP_ENV_ID} components..."
    COMPONENTS_JSON=$(curl_api "${CTP_URL}/em/api/v3/environments/${CTP_ENV_ID}/components")
    debug "CTP components: ${COMPONENTS_JSON}"
fi

# ─── Step 3: Resolve DTP filter ID ────────────────────────────────────────────

FILTER_ID=""
if [[ -n "${DTP_URL}" ]]; then
    log "Resolving DTP filter ID for '${APP_NAME}'..."
    filter_json=$(curl_api "${DTP_URL}/grs/api/v1.12/filters?managedOnly=false&name=${APP_NAME}")
    debug "DTP filter response: ${filter_json}"
    FILTER_ID=$(echo "${filter_json}" | jq -r '.[0].id // empty')
    if [[ -z "${FILTER_ID}" ]]; then
        echo "ERROR: Could not resolve DTP filter ID for '${APP_NAME}'." >&2
        exit 1
    fi
    log "  Resolved DTP filter ID: ${FILTER_ID}"
fi

# ─── Step 4: Generate per-service agent.properties ────────────────────────────

for entry in "${SERVICES[@]}"; do
    service="${entry%%:*}"
    ctp_name="${entry#*:}"
    coverage_dir="${service}/src/test/resources/coverage"
    props="${coverage_dir}/agent.properties"

    log "Configuring ${service}..."
    mkdir -p "${coverage_dir}"
    cp "${TEMPLATE}" "${props}"

    # Patch ctp.subscription.queue from CTP API response.
    # ctp.websocket.url is only patched when --use-ctp-ws-url is set (e.g. Jenkins, where CTP
    # is on a remote host). For local Docker deployments the template value is correct as-is.
    if [[ -n "${COMPONENTS_JSON}" ]]; then
        ws_url=$(echo "${COMPONENTS_JSON}" | jq -r --arg n "${ctp_name}" \
            '.components[] | select(.name == $n) | .ctpWebsocketUrl // empty')
        sub_queue=$(echo "${COMPONENTS_JSON}" | jq -r --arg n "${ctp_name}" \
            '.components[] | select(.name == $n) | .ctpSubscriptionQueue // empty')
        if [[ -z "${ws_url}" || -z "${sub_queue}" ]]; then
            echo "ERROR: CTP component '${ctp_name}' not found in environment ${CTP_ENV_ID}." >&2
            exit 1
        fi
        [[ "${USE_CTP_WS_URL}" == "true" ]] && \
            sed_inplace "s|^ctp.websocket.url=.*|ctp.websocket.url=${ws_url}|" "${props}"
        sed_inplace "s|^ctp.subscription.queue=.*|ctp.subscription.queue=${sub_queue}|" "${props}"
    fi

    # Patch common DTP properties
    sed_inplace "s|^dtp.project=.*|dtp.project=${APP_NAME}|"                                     "${props}"
    sed_inplace "s|^dtp.buildID=.*|dtp.buildID=${APP_NAME}-${BUILD_ID}|"                         "${props}"
    sed_inplace "s|^dtp.coverageImages=.*|dtp.coverageImages=${APP_NAME};${APP_NAME}-FT|"         "${props}"
    [[ -n "${FILTER_ID}" ]] && \
        sed_inplace "s|^dtp.filterID=.*|dtp.filterID=${FILTER_ID}|" "${props}"

    if [[ "${CI_DEBUG}" == "true" ]]; then
        echo "--- ${props} ---"
        cat "${props}"
        echo "---"
    fi
    log "  ${props} written."
done

# ─── Step 5: Create runtime data directories for compose bind mounts ──────────

log "Creating runtime data directories..."
for dir in \
    spring-petclinic-api-gateway/src/test/resources/coverage/runtime_coverage_api_gateway1 \
    spring-petclinic-api-gateway/src/test/resources/coverage/runtime_coverage_api_gateway2 \
    spring-petclinic-customers-service/src/test/resources/coverage/runtime_coverage_customer1 \
    spring-petclinic-customers-service/src/test/resources/coverage/runtime_coverage_customer2 \
    spring-petclinic-vets-service/src/test/resources/coverage/runtime_coverage_vets1 \
    spring-petclinic-vets-service/src/test/resources/coverage/runtime_coverage_vets2 \
    spring-petclinic-visits-service/src/test/resources/coverage/runtime_coverage_visit1 \
    spring-petclinic-visits-service/src/test/resources/coverage/runtime_coverage_visit2; do
    mkdir -p "${dir}"
done

log "Coverage setup complete."
