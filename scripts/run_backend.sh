#!/bin/zsh
set -euo pipefail

RAIZ_PROYECTO="$(cd "$(dirname "$0")/.." && pwd)"
CARPETA_COMPILACION="$RAIZ_PROYECTO/out"
JAR_MYSQL="${MYSQL_CONNECTOR_JAR:-/Users/adri/Java/mysql-connector-j-9.6.0.jar}"

rm -rf "$CARPETA_COMPILACION"
mkdir -p "$CARPETA_COMPILACION"

if [ ! -f "$JAR_MYSQL" ]; then
  echo "No se ha encontrado el driver MySQL: $JAR_MYSQL"
  echo "Exporta MYSQL_CONNECTOR_JAR con la ruta correcta antes de ejecutar este script."
  exit 1
fi

if [ ! -f "$RAIZ_PROYECTO/config/application.properties" ]; then
  cp "$RAIZ_PROYECTO/config/application.properties.example" "$RAIZ_PROYECTO/config/application.properties"
  echo "Se ha creado config/application.properties a partir del ejemplo."
  echo "Revisa usuario, contrasena y URL de la base de datos antes de continuar."
fi

javac -cp "$JAR_MYSQL" -d "$CARPETA_COMPILACION" $(find "$RAIZ_PROYECTO/src" -name "*.java")
java -cp "$CARPETA_COMPILACION:$JAR_MYSQL" com.lineacano.servidor.Aplicacion
