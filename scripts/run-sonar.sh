#!/usr/bin/env bash
set -e

SONAR_URL="http://seralu4.esi.uclm.es:1521"
SONAR_TOKEN="squ_11efbdd2b29294aaa690ecc31fd36bc8eb3dea53"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=========================================================="
echo " ESIBuy - Analisis SonarQube ($SONAR_URL)"
echo "=========================================================="

echo "Comprobando conexion con $SONAR_URL..."
if ! curl -s --connect-timeout 4 "$SONAR_URL/api/system/status" > /dev/null 2>&1; then
  echo ""
  echo "[ERROR] No se puede alcanzar $SONAR_URL"
  echo "Asegurate de estar conectado a eduroam/red ESI o con la VPN GlobalProtect."
  exit 1
fi

echo "[OK] Servidor SonarQube alcanzable."
echo ""
echo "----------------------------------------------------------"
echo " [1/2] Analizando Backend (Java 21 / JaCoCo / SonarQube)..."
echo "----------------------------------------------------------"
cd "$ROOT_DIR/backend"
./mvnw clean verify sonar:sonar \
  -Dsonar.host.url="$SONAR_URL" \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.coverage.jacoco.xmlReportPaths="target/site/jacoco/jacoco.xml"

echo ""
echo "----------------------------------------------------------"
echo " [2/2] Analizando Frontend (Angular / SonarScanner)..."
echo "----------------------------------------------------------"
cd "$ROOT_DIR/frontend"
npx --yes sonar-scanner -Dsonar.host.url="$SONAR_URL" -Dsonar.token="$SONAR_TOKEN"

echo ""
echo "=========================================================="
echo " Analisis completado con exito!"
echo " Consulta los resultados en: $SONAR_URL"
echo "=========================================================="
