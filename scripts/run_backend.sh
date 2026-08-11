#!/bin/zsh
set -euo pipefail

RAIZ_PROYECTO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RAIZ_PROYECTO"

if ! command -v mvn >/dev/null 2>&1; then
  echo "No se ha encontrado Maven. Instala Maven 3.9 o posterior."
  exit 1
fi

if [ ! -f "config/application.properties" ]; then
  cp "config/application.properties.example" "config/application.properties"
  echo "Se ha creado config/application.properties a partir del ejemplo."
  echo "Configura la conexion a MySQL y vuelve a ejecutar este script."
  exit 1
fi

mvn compile exec:java
