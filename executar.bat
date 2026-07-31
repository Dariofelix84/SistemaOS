@echo off
cd /d "%~dp0"
start javaw -cp "dist/lib/*;bin;src" br.com.dftech.screens.TelaLogin
