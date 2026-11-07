# Installs the HeadlessMc launcher on Windows, run in PowerShell:
#
#   irm https://headlesshq.github.io/headlessmc/install.ps1 | iex
#
# Options, set as environment variables before running, e.g. $env:HEADLESSMC_VERSION = '3.0.0'
#   HEADLESSMC_VERSION      release tag to install, e.g. 3.0.0 (default: latest release)
#   HEADLESSMC_INSTALL_DIR  directory to install into (default: %LOCALAPPDATA%\headlessmc\bin)
#
# On Linux and macOS use install.sh instead.

& {
    $ErrorActionPreference = 'Stop'
    $ProgressPreference = 'SilentlyContinue' # Invoke-WebRequest is very slow in Windows PowerShell 5.1 otherwise
    # Windows PowerShell 5.1 might not offer TLS 1.2 by default, which GitHub requires
    [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

    $repo = 'headlesshq/headlessmc'
    $docs = 'https://headlesshq.github.io/headlessmc/getting-started/'
    # there is no ARM64 executable, Windows on ARM runs the x64 executable through emulation
    $asset = 'headlessmc-launcher-windows-x64.exe'

    if ($env:HEADLESSMC_VERSION) {
        $url = "https://github.com/$repo/releases/download/$env:HEADLESSMC_VERSION/$asset"
    } else {
        $url = "https://github.com/$repo/releases/latest/download/$asset"
    }
    if ($env:HEADLESSMC_INSTALL_DIR) {
        $dir = $env:HEADLESSMC_INSTALL_DIR
    } else {
        $dir = Join-Path $env:LOCALAPPDATA 'headlessmc\bin'
    }
    $exe = Join-Path $dir 'headlessmc.exe'
    $tmp = Join-Path $dir "headlessmc.exe.download"

    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Write-Host "Downloading $url"
    try {
        Invoke-WebRequest -Uri $url -OutFile $tmp -UseBasicParsing
        try {
            Move-Item -Force -Path $tmp -Destination $exe
        } catch {
            throw "Could not replace $exe, close all running HeadlessMc instances and try again. $_"
        }
    } finally {
        Remove-Item -Force -ErrorAction SilentlyContinue -Path $tmp
    }
    Write-Host "Installed headlessmc to $exe"

    # Add to the user PATH. Read and write the registry directly, to keep entries like %USERPROFILE%\bin unexpanded.
    $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey('Environment', $true)
    try {
        $userPath = [string]$key.GetValue('Path', '', [Microsoft.Win32.RegistryValueOptions]::DoNotExpandEnvironmentNames)
        $entries = $userPath -split ';' | Where-Object { $_ }
        if ($entries -notcontains $dir) {
            $key.SetValue('Path', (@($dir) + $entries) -join ';', [Microsoft.Win32.RegistryValueKind]::ExpandString)
            # setting any variable through .NET notifies other programs (e.g. Explorer) that the environment changed
            [Environment]::SetEnvironmentVariable('HEADLESSMC_INSTALLER', '1', 'User')
            [Environment]::SetEnvironmentVariable('HEADLESSMC_INSTALLER', $null, 'User')
            Write-Host "Added $dir to your PATH, restart your terminal to use it."
        }
    } finally {
        $key.Close()
    }
    if (($env:Path -split ';') -notcontains $dir) {
        $env:Path = "$dir;$env:Path"
    }

    Write-Host "Run 'headlessmc' to get started, see $docs"
}
