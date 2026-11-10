# ==============================================================================
# ESIBuy - Ejecucion y subida de analisis a SonarQube UCLM (seralu4.esi.uclm.es)
# ==============================================================================

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " ESIBuy - Analisis SonarQube (seralu4.esi.uclm.es:1521)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$sonarHost = "seralu4.esi.uclm.es"
$sonarPort = 1521
$sonarUrl = "http://seralu4.esi.uclm.es:1521"
$sonarToken = $sonarToken

Write-Host "Comprobando conexion con el servidor $sonarUrl..." -ForegroundColor Yellow

$connected = $false
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $iar = $tcp.BeginConnect($sonarHost, $sonarPort, $null, $null)
    $wait = $iar.AsyncWaitHandle.WaitOne(3000, $false)
    if ($wait -and $tcp.Connected) {
        $connected = $true
        $tcp.EndConnect($iar)
    }
    $tcp.Close()
}
catch {
    $connected = $false
}

if (-not $connected) {
    Write-Host ""
    Write-Host "[ERROR] No se puede alcanzar $sonarUrl" -ForegroundColor Red
    Write-Host "Para subir los analisis a SonarQube, necesitas estar dentro de la red UCLM:" -ForegroundColor Yellow
    Write-Host "  1. Si estas en el campus: conectate a eduroam o cable ESI." -ForegroundColor Gray
    Write-Host "  2. Si estas en casa: abre GlobalProtect y conectate a portal.vpn.uclm.es." -ForegroundColor Gray
    Write-Host ""
    exit 1
}

Write-Host "[OK] Conexion establecida con el servidor SonarQube." -ForegroundColor Green
Write-Host ""

# 1. Backend
Write-Host "----------------------------------------------------------" -ForegroundColor Cyan
Write-Host " [1/2] Analizando Backend (Java 21 / JaCoCo / SonarQube)..." -ForegroundColor Cyan
Write-Host "----------------------------------------------------------" -ForegroundColor Cyan
Push-Location "$PSScriptRoot\..\backend"
try {
    cmd.exe /c "mvnw.cmd clean verify sonar:sonar -Dsonar.host.url=$sonarUrl -Dsonar.token=$sonarToken -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] El analisis del backend ha fallado." -ForegroundColor Red
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}

# 2. Frontend
Write-Host ""
Write-Host "----------------------------------------------------------" -ForegroundColor Cyan
Write-Host " [2/2] Analizando Frontend (Angular / SonarScanner)..." -ForegroundColor Cyan
Write-Host "----------------------------------------------------------" -ForegroundColor Cyan
Push-Location "$PSScriptRoot\..\frontend"
try {
    cmd.exe /c "npx --yes sonar-scanner -Dsonar.host.url=$sonarUrl -Dsonar.token=$sonarToken"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] El analisis del frontend ha fallado." -ForegroundColor Red
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " ¡Analisis de Backend y Frontend completados con exito!" -ForegroundColor Green
Write-Host " Consulta los resultados en: $sonarUrl" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green


# ==============================================================================
# ESIBuy - Ejecucion y subida de analisis a SonarQube UCLM
# ==============================================================================

$ErrorActionPreference = "Stop"

$sonarHost = "seralu4.esi.uclm.es"
$sonarPort = 1521
$sonarUrl = "http://${sonarHost}:${sonarPort}"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " ESIBuy - Analisis SonarQube ($sonarUrl)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Comprobar que existe el token en el entorno
if ([string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
    Write-Host "[ERROR] La variable de entorno SONAR_TOKEN no esta configurada." -ForegroundColor Red
    Write-Host "Configura el token antes de ejecutar este script." -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] Token de SonarQube detectado." -ForegroundColor Green

# Comprobar conexion con SonarQube
Write-Host "Comprobando conexion con el servidor $sonarUrl..." -ForegroundColor Yellow

$connected = $false
$tcp = New-Object System.Net.Sockets.TcpClient

try {
    $iar = $tcp.BeginConnect($sonarHost, $sonarPort, $null, $null)
    $wait = $iar.AsyncWaitHandle.WaitOne(3000, $false)

    if ($wait -and $tcp.Connected) {
        $tcp.EndConnect($iar)
        $connected = $true
    }
}
catch {
    $connected = $false
}
finally {
    $tcp.Close()
}

if (-not $connected) {
    Write-Host ""
    Write-Host "[ERROR] No se puede alcanzar $sonarUrl" -ForegroundColor Red
    Write-Host "Comprueba que estas conectado a la red UCLM:" -ForegroundColor Yellow
    Write-Host "  1. En el campus: eduroam o cable ESI." -ForegroundColor Gray
    Write-Host "  2. Desde casa: GlobalProtect (portal.vpn.uclm.es)." -ForegroundColor Gray
    exit 1
}

Write-Host "[OK] Conexion establecida con SonarQube." -ForegroundColor Green

# 1. Backend
Write-Host ""
Write-Host "----------------------------------------------------------" -ForegroundColor Cyan
Write-Host " [1/2] Analizando Backend (Java 21 / JaCoCo / SonarQube)..." -ForegroundColor Cyan
Write-Host "----------------------------------------------------------" -ForegroundColor Cyan

Push-Location "$PSScriptRoot\..\backend"
try {
    & .\mvnw.cmd clean verify sonar:sonar `
        "-Dsonar.host.url=$sonarUrl" `
        "-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml"

    if ($LASTEXITCODE -ne 0) {
        throw "El analisis del backend ha fallado."
    }
}
finally {
    Pop-Location
}

# 2. Frontend
Write-Host ""
Write-Host "----------------------------------------------------------" -ForegroundColor Cyan
Write-Host " [2/2] Analizando Frontend (Angular / SonarScanner)..." -ForegroundColor Cyan
Write-Host "----------------------------------------------------------" -ForegroundColor Cyan

Push-Location "$PSScriptRoot\..\frontend"
try {
    & npx.cmd --yes sonar-scanner "-Dsonar.host.url=$sonarUrl"

    if ($LASTEXITCODE -ne 0) {
        throw "El analisis del frontend ha fallado."
    }
}
finally {
    Pop-Location
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " Analisis de Backend y Frontend completados con exito." -ForegroundColor Green
Write-Host " Consulta los resultados en: $sonarUrl" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
