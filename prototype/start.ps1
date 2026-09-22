$prototypeRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$prototypeFile = Join-Path $prototypeRoot 'index.html'
Start-Process -FilePath $prototypeFile
