#!/usr/bin/env bash
set -Eeuo pipefail

ENV_FILE=/etc/inventory-med/api.env
API_SERVICE=inventory-med-api
HEALTH_URL=http://127.0.0.1:8080/api/v1/actuator/health
PROVISIONER_LOGIN=inventorymed_tenant_provisioner

if [[ ${EUID} -ne 0 ]]; then
    echo "Execute este script com sudo." >&2
    exit 1
fi
if [[ ! -f ${ENV_FILE} ]]; then
    echo "Arquivo ${ENV_FILE} não encontrado." >&2
    exit 1
fi

for command_name in awk curl install openssl sqlcmd; do
    if ! command -v "${command_name}" >/dev/null 2>&1; then
        echo "Comando obrigatório não encontrado: ${command_name}" >&2
        exit 1
    fi
done

ORIGINAL_ENV=$(mktemp /root/inventory-med-api.env.provisioning.XXXXXX)
WORK_ENV=$(mktemp /root/inventory-med-api.env.work.XXXXXX)
TEMP_SQL=$(mktemp /root/inventory-med-provisioner.XXXXXX.sql)
ENVIRONMENT_CHANGED=false

cleanup() {
    local exit_status=$?
    trap - EXIT
    unset SA_PASSWORD SQLCMDPASSWORD TENANT_PROVISIONER_PASSWORD
    unset TENANT_CREDENTIAL_ENCRYPTION_KEY
    rm -f "${WORK_ENV}" "${TEMP_SQL}"

    if [[ ${exit_status} -ne 0 && ${ENVIRONMENT_CHANGED} == true ]]; then
        echo "Falha detectada; restaurando a configuração anterior da API." >&2
        install -m 0600 -o root -g root "${ORIGINAL_ENV}" "${ENV_FILE}"
        systemctl restart "${API_SERVICE}" 2>/dev/null || true
    fi

    rm -f "${ORIGINAL_ENV}"
    exit "${exit_status}"
}
trap cleanup EXIT

cp --preserve=mode,ownership,timestamps "${ENV_FILE}" "${ORIGINAL_ENV}"

set -a
# O arquivo pertence a root, possui modo 600 e segue o formato NAME="VALUE".
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

required_environment=(DB_MIGRATION_USERNAME DB_MIGRATION_PASSWORD)
for variable_name in "${required_environment[@]}"; do
    if [[ -z ${!variable_name:-} ]]; then
        echo "Configuração obrigatória ausente: ${variable_name}" >&2
        exit 1
    fi
done

if [[ ${TENANT_PROVISIONING_ENABLED:-false} == true ]]; then
    echo "O provisionamento hospitalar já está habilitado." >&2
    exit 1
fi

sql_scalar() {
    local username=$1
    local password=$2
    local database=$3
    local query=$4
    SQLCMDPASSWORD="${password}" \
        /opt/mssql-tools18/bin/sqlcmd \
        -S 127.0.0.1 \
        -U "${username}" \
        -d "${database}" \
        -C -b -h -1 -W \
        -Q "${query}" |
        tr -d '\r' |
        xargs
}

STORED_TENANT_CREDENTIALS=$(sql_scalar \
    "${DB_MIGRATION_USERNAME}" \
    "${DB_MIGRATION_PASSWORD}" \
    inventory_med_core \
    "SET NOCOUNT ON; SELECT COUNT(*) FROM dbo.hospital_database_secret;")
if [[ ${STORED_TENANT_CREDENTIALS} != 0 ]]; then
    echo "A ativação foi cancelada porque já existem credenciais hospitalares armazenadas." >&2
    echo "Esse cenário exige recuperação específica para preservar a chave existente." >&2
    exit 1
fi

read -r -s -p "Senha atual do login sa do SQL Server: " SA_PASSWORD </dev/tty
echo >/dev/tty
if [[ -z ${SA_PASSWORD} ]]; then
    echo "A senha de sa não pode ficar vazia." >&2
    exit 1
fi

TENANT_PROVISIONER_PASSWORD="Im!$(openssl rand -hex 24)A9"
TENANT_CREDENTIAL_ENCRYPTION_KEY=$(openssl rand -base64 32 | tr -d '\n')

cat > "${TEMP_SQL}" <<'SQL'
USE [master];
GO

IF SUSER_ID(N'inventorymed_tenant_provisioner') IS NULL
BEGIN
    DECLARE @createLogin NVARCHAR(MAX) =
        N'CREATE LOGIN [inventorymed_tenant_provisioner] WITH PASSWORD = ' +
        QUOTENAME(N'CHANGE_ME_TENANT_PROVISIONER', '''') +
        N', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF;';
    EXEC sys.sp_executesql @createLogin;
END;
ELSE
BEGIN
    DECLARE @alterLogin NVARCHAR(MAX) =
        N'ALTER LOGIN [inventorymed_tenant_provisioner] WITH PASSWORD = ' +
        QUOTENAME(N'CHANGE_ME_TENANT_PROVISIONER', '''') + N';';
    EXEC sys.sp_executesql @alterLogin;
END;
GO

GRANT CREATE ANY DATABASE TO [inventorymed_tenant_provisioner];
GRANT ALTER ANY LOGIN TO [inventorymed_tenant_provisioner];
GO
SQL
chmod 0600 "${TEMP_SQL}"
sed -i \
    "s/CHANGE_ME_TENANT_PROVISIONER/${TENANT_PROVISIONER_PASSWORD}/g" \
    "${TEMP_SQL}"

export SQLCMDPASSWORD=${SA_PASSWORD}
/opt/mssql-tools18/bin/sqlcmd \
    -S 127.0.0.1 \
    -U sa \
    -C -b \
    -i "${TEMP_SQL}"
unset SA_PASSWORD SQLCMDPASSWORD
rm -f "${TEMP_SQL}"

PROVISIONER_PERMISSIONS=$(sql_scalar \
    "${PROVISIONER_LOGIN}" \
    "${TENANT_PROVISIONER_PASSWORD}" \
    master \
    "SET NOCOUNT ON; SELECT CONCAT(HAS_PERMS_BY_NAME(NULL, NULL, 'CREATE ANY DATABASE'), ':', HAS_PERMS_BY_NAME(NULL, NULL, 'ALTER ANY LOGIN'));")
if [[ ${PROVISIONER_PERMISSIONS} != "1:1" ]]; then
    echo "O login técnico não recebeu as permissões esperadas." >&2
    exit 1
fi

awk '
    !/^TENANT_PROVISIONING_ENABLED=/ &&
    !/^DB_PROVISIONING_URL=/ &&
    !/^DB_PROVISIONING_USERNAME=/ &&
    !/^DB_PROVISIONING_PASSWORD=/ &&
    !/^TENANT_CREDENTIAL_ENCRYPTION_KEY=/
' "${ENV_FILE}" > "${WORK_ENV}"
cat >> "${WORK_ENV}" <<EOF
TENANT_PROVISIONING_ENABLED="true"
DB_PROVISIONING_URL="jdbc:sqlserver://127.0.0.1:1433;databaseName=master;encrypt=true;trustServerCertificate=true"
DB_PROVISIONING_USERNAME="${PROVISIONER_LOGIN}"
DB_PROVISIONING_PASSWORD="${TENANT_PROVISIONER_PASSWORD}"
TENANT_CREDENTIAL_ENCRYPTION_KEY="${TENANT_CREDENTIAL_ENCRYPTION_KEY}"
EOF
install -m 0600 -o root -g root "${WORK_ENV}" "${ENV_FILE}"
ENVIRONMENT_CHANGED=true

systemctl restart "${API_SERVICE}"
for _attempt in {1..30}; do
    if curl --fail --silent "${HEALTH_URL}" | grep -q '"status":"UP"'; then
        ENVIRONMENT_CHANGED=false
        unset TENANT_PROVISIONER_PASSWORD TENANT_CREDENTIAL_ENCRYPTION_KEY
        echo "Provisionamento hospitalar habilitado e API saudável."
        echo "Atualize o painel administrativo antes de criar os bancos hospitalares."
        exit 0
    fi
    sleep 1
done

echo "A API não ficou saudável dentro do tempo esperado." >&2
exit 1
