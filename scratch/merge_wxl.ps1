$wxlPath = 'C:\Program Files (x86)\WiX Toolset v3.14\SDK\wixui\WixUI_pt-BR.wxl'
$destPath = 'c:\Users\dario\Desktop\Backup2\SistemaOS\wix_resources\MsiInstallerStrings_en.wxl'

[xml]$xml = Get-Content $wxlPath -Encoding UTF8
$xml.WixLocalization.Culture = 'en-us'

$ns = 'http://schemas.microsoft.com/wix/2006/localization'

$customItems = @(
    @{ Id = 'MainFeatureTitle'; Value = 'SistemaOS' },
    @{ Id = 'DowngradeErrorMessage'; Value = 'Uma versão mais recente do [ProductName] já está instalada. A instalação foi encerrada.' },
    @{ Id = 'DisallowUpgradeErrorMessage'; Value = 'Uma versão anterior do [ProductName] já está instalada. As atualizações estão desativadas.' },
    @{ Id = 'ShortcutPromptDlg_Title'; Value = 'Instalador do [ProductName]' },
    @{ Id = 'ShortcutPromptDlgTitle'; Value = '{\WixUI_Font_Title}Atalhos' },
    @{ Id = 'ShortcutPromptDlgBannerBitmap'; Value = 'WixUI_Bmp_Banner' },
    @{ Id = 'ShortcutPromptDlgDescription'; Value = 'Selecione onde deseja criar os atalhos do aplicativo.' },
    @{ Id = 'ShortcutPromptDlgDesktopShortcutControlLabel'; Value = 'Criar atalho na Área de Trabalho' },
    @{ Id = 'ShortcutPromptDlgStartMenuShortcutControlLabel'; Value = 'Criar atalho no Menu Iniciar' },
    @{ Id = 'InstallDirNotEmptyDlg_Title'; Value = 'Instalador do [ProductName]' },
    @{ Id = 'InstallDirNotEmptyDlgInstallDirExistMessage'; Value = 'A pasta [INSTALLDIR] já existe. A versão antiga será desinstalada e a nova versão será instalada nesta pasta. Deseja continuar?' },
    @{ Id = 'ContextMenuCommandLabel'; Value = 'Abrir com [ProductName]' }
)

foreach ($item in $customItems) {
    $elem = $xml.CreateElement('String', $ns)
    $elem.SetAttribute('Id', $item.Id)
    $elem.SetAttribute('Overridable', 'yes')
    $elem.InnerText = $item.Value
    [void]$xml.WixLocalization.AppendChild($elem)
}

$xml.Save($destPath)
Write-Host "MsiInstallerStrings_en.wxl generated successfully without empty namespace!"
