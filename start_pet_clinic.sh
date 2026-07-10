set -euo pipefail

start_service() {
    local module="$1"
    local port="$2"

    echo "Starting $module on port $port..."

    nohup mvn \
        -pl "$module" \
        spring-boot:run \
        -Dspring-boot.run.arguments='--CONFIG_SERVER_URL=http://localhost:8288' \
        -Dspring-boot.run.jvmArguments="-Dserver.port=$port -Deureka.client.serviceUrl.defaultZone=http://localhost:8888/eureka" \
        > "${module}.log" 2>&1 &

    echo $! > "${module}.pid"
}

start_service spring-petclinic-config-server      8288
sleep 15
start_service spring-petclinic-discovery-server   8888
sleep 15
start_service spring-petclinic-api-gateway        8099
start_service spring-petclinic-customers-service  8081
start_service spring-petclinic-vets-service       8082
start_service spring-petclinic-visits-service     8083
