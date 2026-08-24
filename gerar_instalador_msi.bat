@echo off
setlocal
cd /d "%~dp0"

echo [1/4] Adicionando WiX Toolset ao PATH...
set "PATH=C:\Program Files (x86)\WiX Toolset v3.14\bin;C:\Program Files (x86)\WiX Toolset v3.11\bin;%PATH%"

echo [2/4] Compilando fontes Java e gerando dist\Dftech.jar...
if not exist "build\classes" mkdir "build\classes"
javac -encoding UTF-8 -cp "dist\lib\*;src" -d build\classes src\br\com\dftech\dal\*.java src\br\com\dftech\screens\*.java
if %errorlevel% neq 0 (
    echo Erro ao compilar os fontes Java.
    exit /b %errorlevel%
)

xcopy /E /I /Y src\br\com\dftech\icons build\classes\br\com\dftech\icons
xcopy /E /I /Y reports dist\reports
jar cfm dist\Dftech.jar manifest.mf -C build\classes .

echo [3/4] Criando pasta de destino na Area de Trabalho...
set "OUTPUT_DIR=%USERPROFILE%\Desktop\Instalador_SistemaOS"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo [4/4] Executando jpackage para gerar o instalador .MSI...
jpackage ^
  --type msi ^
  --name "SistemaOS" ^
  --app-version "1.0.0" ^
  --vendor "DFtech" ^
  --dest "%OUTPUT_DIR%" ^
  --input "dist" ^
  --main-jar "Dftech.jar" ^
  --main-class "br.com.dftech.screens.TelaLogin" ^
  --icon "x.ico" ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --java-options "--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"

if %errorlevel% equ 0 (
    echo.
    echo ========================================================
    echo  Instalador MSI gerado com sucesso!
    echo  Local do instalador: %OUTPUT_DIR%
    echo ========================================================
) else (
    echo.
    echo Ocorreu um erro durante a geracao do instalador.
)
