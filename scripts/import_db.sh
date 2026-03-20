#!/bin/zsh
set -euo pipefail

RAIZ_PROYECTO="$(cd "$(dirname "$0")/.." && pwd)"
ARCHIVO_SQL="${1:-$RAIZ_PROYECTO/backup_2026-03-20_11-35.sql}"
ARCHIVO_SQL_AUTENTICACION="$RAIZ_PROYECTO/sql/auth_schema.sql"
ARCHIVO_SQL_MIGRACION_AUTENTICACION="$RAIZ_PROYECTO/sql/migracion_autenticacion_es.sql"
NOMBRE_BASE_DATOS="${MYSQL_DATABASE:-linea_cano}"
USUARIO_BASE_DATOS="${MYSQL_USER:-root}"

if [ ! -f "$ARCHIVO_SQL" ]; then
  echo "No existe el fichero SQL: $ARCHIVO_SQL"
  exit 1
fi

echo "Importando $ARCHIVO_SQL en la base de datos $NOMBRE_BASE_DATOS con el usuario $USUARIO_BASE_DATOS"
mysql -u "$USUARIO_BASE_DATOS" -p -e "CREATE DATABASE IF NOT EXISTS \`$NOMBRE_BASE_DATOS\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -u "$USUARIO_BASE_DATOS" -p "$NOMBRE_BASE_DATOS" < "$ARCHIVO_SQL"

if [ -f "$ARCHIVO_SQL_MIGRACION_AUTENTICACION" ]; then
  echo "Aplicando migracion de autenticacion en espanol desde $ARCHIVO_SQL_MIGRACION_AUTENTICACION"
  mysql -u "$USUARIO_BASE_DATOS" -p "$NOMBRE_BASE_DATOS" < "$ARCHIVO_SQL_MIGRACION_AUTENTICACION" || true
fi

if [ -f "$ARCHIVO_SQL_AUTENTICACION" ]; then
  echo "Aplicando esquema de autenticacion desde $ARCHIVO_SQL_AUTENTICACION"
  mysql -u "$USUARIO_BASE_DATOS" -p "$NOMBRE_BASE_DATOS" < "$ARCHIVO_SQL_AUTENTICACION"
fi

echo "Importacion completada."
