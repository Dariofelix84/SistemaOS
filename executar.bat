@echo off
cd /d "%~dp0"
start javaw --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -jar "dist\Dftech.jar"
