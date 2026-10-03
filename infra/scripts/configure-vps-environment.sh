#!/usr/bin/env bash
set -Eeuo pipefail

if [[ ${EUID} -ne 0 ]]; then
    echo "Execute este script com sudo." >&2
    exit 1
fi

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
INFRA_DIR=$(cd -- "${SCRIPT_DIR}/.." && pwd)
SQL_TEMPLATE="${INFRA_DIR}/sql/provision-core-database.sql"
ENV_FILE=/etc/inventory-med/api.env
PUBLIC_ORIGIN=${PUBLIC_ORIGIN:-http://179.236.237.36}

read -r -s -p "Senha atual do login sa do SQL Server: " SA_PASSWORD
echo
read -r -s -p "Senha inicial de Lucas Galante (mínimo 12 caracteres): " DOCTOR_PASSWORD
echo

if [[ ${#DOCTOR_PASSWORD} -lt 12 ]]; then
    echo "A senha do médico deve ter pelo menos 12 caracteres." >&2
    exit 1
fi
if [[ ! ${DOCTOR_PASSWORD} =~ ^[A-Za-z0-9@#%+=._-]+$ ]]; then
    echo "Use somente letras, números e os símbolos @ # % + = . _ - na senha inicial." >&2
    exit 1
fi

APP_PASSWORD="Im!$(openssl rand -hex 24)A9"
MIGRATION_PASSWORD="Im!$(openssl rand -hex 24)A9"
TEMP_SQL=$(mktemp /root/inventory-med-core.XXXXXX.sql)
trap 'rm -f "${TEMP_SQL}"; unset SA_PASSWORD SQLCMDPASSWORD APP_PASSWORD MIGRATION_PASSWORD DOCTOR_PASSWORD' EXIT
chmod 0600 "${TEMP_SQL}"

sed \
    -e "s/CHANGE_ME_APP/${APP_PASSWORD}/g" \
    -e "s/CHANGE_ME_MIGRATION/${MIGRATION_PASSWORD}/g" \
    "${SQL_TEMPLATE}" > "${TEMP_SQL}"

export SQLCMDPASSWORD=${SA_PASSWORD}
/opt/mssql-tools18/bin/sqlcmd \
    -S 127.0.0.1 \
    -U sa \
    -C \
    -b \
    -i "${TEMP_SQL}"
unset SQLCMDPASSWORD SA_PASSWORD

umask 077
cat > "${ENV_FILE}" <<EOF
SERVER_ADDRESS="127.0.0.1"
SERVER_PORT="8080"
DB_URL="jdbc:sqlserver://127.0.0.1:1433;databaseName=inventory_med_core;encrypt=true;trustServerCertificate=true"
DB_USERNAME="inventorymed_core_app"
DB_PASSWORD="${APP_PASSWORD}"
DB_MIGRATION_USERNAME="inventorymed_core_migrator"
DB_MIGRATION_PASSWORD="${MIGRATION_PASSWORD}"
DB_POOL_MAX_SIZE="10"
DB_POOL_MIN_IDLE="2"
SESSION_TIMEOUT="30m"
SESSION_COOKIE_SECURE="false"
ALLOWED_ORIGINS="${PUBLIC_ORIGIN}"
BOOTSTRAP_ENABLED="true"
BOOTSTRAP_DOCTOR_EMAIL="lucas.galante@inventorymed.local"
BOOTSTRAP_DOCTOR_PASSWORD="${DOCTOR_PASSWORD}"
EOF
chmod 0600 "${ENV_FILE}"
chown root:root "${ENV_FILE}"

echo "Banco central, logins restritos e arquivo de ambiente foram configurados."
echo "O bootstrap deve ser desativado após validar o primeiro acesso."
