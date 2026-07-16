
for pidfile in *.pid; do
	[ -f "$pidfile" ] || continue
            kill "$(cat "$pidfile")" || true
done

rm -f *.pid
rm -f *.log
