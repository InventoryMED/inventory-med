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
read -r -p "E-mail do primeiro administrador geral [admin@inventorymed.local]: " SYSTEM_ADMIN_EMAIL
SYSTEM_ADMIN_EMAIL=${SYSTEM_ADMIN_EMAIL:-admin@inventorymed.local}
read -r -s -p "Senha temporária do primeiro administrador geral: " SYSTEM_ADMIN_PASSWORD
echo

if [[ ! ${SYSTEM_ADMIN_EMAIL} =~ ^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+$ ]]; then
    echo "O e-mail informado não é válido." >&2
    exit 1
fi
if [[ ${#SYSTEM_ADMIN_PASSWORD} -lt 12 ]] ||
   [[ ! ${SYSTEM_ADMIN_PASSWORD} =~ [A-Z] ]] ||
   [[ ! ${SYSTEM_ADMIN_PASSWORD} =~ [a-z] ]] ||
   [[ ! ${SYSTEM_ADMIN_PASSWORD} =~ [0-9] ]] ||
   [[ ! ${SYSTEM_ADMIN_PASSWORD} =~ [@#%+=._-] ]]; then
    echo "A senha deve ter 12 caracteres, maiúscula, minúscula, número e símbolo." >&2
    exit 1
fi
if [[ ! ${SYSTEM_ADMIN_PASSWORD} =~ ^[A-Za-z0-9@#%+=._-]+$ ]]; then
    echo "Use somente letras, números e os símbolos @ # % + = . _ - na senha temporária." >&2
    exit 1
fi

APP_PASSWORD="Im!$(openssl rand -hex 24)A9"
MIGRATION_PASSWORD="Im!$(openssl rand -hex 24)A9"
TENANT_PROVISIONER_PASSWORD="Im!$(openssl rand -hex 24)A9"
TENANT_CREDENTIAL_ENCRYPTION_KEY=$(openssl rand -base64 32 | tr -d '\n')
TEMP_SQL=$(mktemp /root/inventory-med-core.XXXXXX.sql)
trap 'rm -f "${TEMP_SQL}"; unset SA_PASSWORD SQLCMDPASSWORD APP_PASSWORD MIGRATION_PASSWORD TENANT_PROVISIONER_PASSWORD TENANT_CREDENTIAL_ENCRYPTION_KEY SYSTEM_ADMIN_PASSWORD' EXIT
chmod 0600 "${TEMP_SQL}"

sed \
    -e "s/CHANGE_ME_APP/${APP_PASSWORD}/g" \
    -e "s/CHANGE_ME_MIGRATION/${MIGRATION_PASSWORD}/g" \
    -e "s/CHANGE_ME_TENANT_PROVISIONER/${TENANT_PROVISIONER_PASSWORD}/g" \
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
BOOTSTRAP_SYSTEM_ADMIN_EMAIL="${SYSTEM_ADMIN_EMAIL}"
BOOTSTRAP_SYSTEM_ADMIN_PASSWORD="${SYSTEM_ADMIN_PASSWORD}"
TENANT_PROVISIONING_ENABLED="true"
DB_PROVISIONING_URL="jdbc:sqlserver://127.0.0.1:1433;databaseName=master;encrypt=true;trustServerCertificate=true"
DB_PROVISIONING_USERNAME="inventorymed_tenant_provisioner"
DB_PROVISIONING_PASSWORD="${TENANT_PROVISIONER_PASSWORD}"
TENANT_CREDENTIAL_ENCRYPTION_KEY="${TENANT_CREDENTIAL_ENCRYPTION_KEY}"
EOF
chmod 0600 "${ENV_FILE}"
chown root:root "${ENV_FILE}"

echo "Banco central, logins técnicos e arquivo de ambiente foram configurados."
echo "O bootstrap deve ser desativado após validar o primeiro acesso."
