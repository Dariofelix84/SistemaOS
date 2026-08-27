@echo off
cd /d "%~dp0"
if not exist "build\classes" mkdir "build\classes"
echo Compilando fontes Java...
javac -encoding UTF-8 -cp "dist\lib\*;src" -d build\classes src\br\com\dftech\dal\*.java src\br\com\dftech\screens\*.java src\br\com\dftech\utils\*.java
echo Copiando recursos e relatorios...
xcopy /E /I /Y src\br\com\dftech\icons build\classes\br\com\dftech\icons
xcopy /E /I /Y reports dist\reports
echo Gerando dist\Dftech.jar...
jar cfm dist\Dftech.jar manifest.mf -C build\classes .
echo Concluido com sucesso!
pause
