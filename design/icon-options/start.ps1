$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Start-Process (Join-Path $root 'index.html')
