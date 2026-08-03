@echo off
if "%~1"=="" (
    echo Usage: docker-logs-since.bat ^<since-timestamp^> ^<output-file^> [container-name]
    echo Example: docker-logs-since.bat "2026-06-17T09:40:35" ctp_si_seq_3.txt ctp
    echo Note: Provide timestamp in LOCAL time without a Z suffix. The script converts to UTC automatically.
    exit /b 1
)
if "%~2"=="" (
    echo Usage: docker-logs-since.bat ^<since-timestamp^> ^<output-file^> [container-name]
    echo Example: docker-logs-since.bat "2026-06-17T09:40:35" ctp_si_seq_3.txt ctp
    exit /b 1
)

set LOCALTIME=%~1
set OUTFILE=%~2
set CONTAINER=%~3
if "%CONTAINER%"=="" set CONTAINER=ctp

rem Convert local time to UTC for docker --since and for the Where-Object filter
for /f "delims=" %%i in ('powershell -Command "[datetime]::Parse('%LOCALTIME%').ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss')"') do set SINCE_UTC=%%i
set DOCKER_SINCE=%SINCE_UTC%Z

set TMPFILE=%TEMP%\docker_logs_tmp_%RANDOM%.txt

echo Fetching logs for container "%CONTAINER%" since %LOCALTIME% (UTC: %DOCKER_SINCE%) ...
docker logs %CONTAINER% --since "%DOCKER_SINCE%" --timestamps > "%TMPFILE%" 2>&1
powershell -Command "Get-Content '%TMPFILE%' | Where-Object { $_ -ge '%SINCE_UTC%' } | ForEach-Object { $_ -replace '^\S+\s', '' } | Out-File '%OUTFILE%' -Encoding utf8"
del "%TMPFILE%"

echo Done. Output written to %OUTFILE%
