@echo off
cd /d "%~dp0"
title Instalador - Sistema OS (DFtech)
chcp 65001 > nul
cls

echo ====================================================
echo             SISTEMA OS - INSTALADOR (DFtech)        
echo ====================================================
echo.

:: 1. Verificar se o Java est? instalado
echo [1/4] Verificando requisitos do sistema...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [AVISO] O Java JRE ou JDK nao foi detectado no seu sistema!
    echo Para que o sistema funcione, certifique-se de instalar o Java 8 ou superior.
    echo.
) else (
    echo [OK] Java instalado detectado.
)
echo.

:: 2. Criar a pasta de instala??o e extrair arquivos do sistema
echo [2/4] Instalando arquivos do sistema em C:\SistemaOS...
if not exist "C:\SistemaOS" (
    mkdir "C:\SistemaOS"
)
powershell -NoProfile -Command "Expand-Archive -Path 'arquivos.zip' -DestinationPath 'C:\SistemaOS' -Force" >nul
if %errorlevel% neq 0 (
    echo [ERRO] Ocorreu um erro ao extrair os arquivos para C:\SistemaOS.
    echo Tente executar este instalador como Administrador - clique com o botao direito e selecione Executar como Administrador.
    pause
    exit /b
)
echo [OK] Arquivos instalados com sucesso.
echo.

:: 3. Configurar os templates de relat?rios no workspace
echo [3/4] Configurando os templates de relat?rios (JaspersoftWorkspace)...
if not exist "C:\Users\dario\JaspersoftWorkspace" (
    mkdir "C:\Users\dario\JaspersoftWorkspace"
)
if not exist "C:\Users\dario\JaspersoftWorkspace\relatorio_clientes" (
    mkdir "C:\Users\dario\JaspersoftWorkspace\relatorio_clientes"
)
if not exist "C:\Users\dario\JaspersoftWorkspace\relatorio_os" (
    mkdir "C:\Users\dario\JaspersoftWorkspace\relatorio_os"
)
if not exist "C:\Users\dario\JaspersoftWorkspace\relatorio_servicos" (
    mkdir "C:\Users\dario\JaspersoftWorkspace\relatorio_servicos"
)
copy /y "C:\SistemaOS\reports\relatorio_clientes\*.*" "C:\Users\dario\JaspersoftWorkspace\relatorio_clientes" >nul
copy /y "C:\SistemaOS\reports\relatorio_os\*.*" "C:\Users\dario\JaspersoftWorkspace\relatorio_os" >nul
copy /y "C:\SistemaOS\reports\relatorio_servicos\*.*" "C:\Users\dario\JaspersoftWorkspace\relatorio_servicos" >nul
echo [OK] Relat?rios configurados com sucesso.
echo.

:: 4. Criar atalho na ?rea de Trabalho
echo [4/4] Criando atalho na ?rea de Trabalho...
powershell -NoProfile -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut($env:USERPROFILE + '\Desktop\SistemaOS.lnk'); $Shortcut.TargetPath = 'C:\SistemaOS\executar.bat'; $Shortcut.WorkingDirectory = 'C:\SistemaOS'; $Shortcut.IconLocation = 'C:\SistemaOS\x.ico'; $Shortcut.Save()" >nul 2>&1
if %errorlevel% neq 0 (
    rem Fallback sem icone caso falhe por algum motivo
    powershell -NoProfile -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut($env:USERPROFILE + '\Desktop\SistemaOS.lnk'); $Shortcut.TargetPath = 'C:\SistemaOS\executar.bat'; $Shortcut.WorkingDirectory = 'C:\SistemaOS'; $Shortcut.Save()" >nul
)
echo [OK] Atalho "SistemaOS" criado na ?rea de Trabalho.
echo.

echo ====================================================
echo          INSTALA??O CONCLU?DA COM SUCESSO!
echo ====================================================
echo.
echo O sistema foi instalado na pasta: C:\SistemaOS
echo Um atalho foi adicionado ? sua ?rea de Trabalho (Desktop).
echo.
echo Pressione qualquer tecla para fechar...
pause >nul
