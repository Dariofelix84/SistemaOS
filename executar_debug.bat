@echo off
cd /d "%~dp0"
echo Iniciando aplicacao com console de debug...
java -cp "dist/lib/*;bin;src" br.com.dftech.screens.TelaLogin
echo.
echo Aplicacao encerrada. Pressione qualquer tecla...
pause
