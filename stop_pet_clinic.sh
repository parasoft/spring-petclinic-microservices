
for pidfile in *.pid; do
	[ -f "$pidfile" ] || continue
            kill "$(cat "$pidfile")" || true
done
