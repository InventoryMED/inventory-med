#!/usr/bin/env bash
set -Eeuo pipefail

ENV_FILE=/etc/inventory-med/api.env
API_SERVICE=inventory-med-api
HEALTH_URL=http://127.0.0.1:8080/api/v1/actuator/health

if [[ ${EUID} -ne 0 ]]; then
    echo "Execute este script com sudo." >&2
    exit 1
fi
if [[ ! -f ${ENV_FILE} ]]; then
    echo "Arquivo ${ENV_FILE} não encontrado." >&2
    exit 1
fi

for command_name in awk curl install sqlcmd; do
    if ! command -v "${command_name}" >/dev/null 2>&1; then
        echo "Comando obrigatório não encontrado: ${command_name}" >&2
        exit 1
    fi
done

ORIGINAL_ENV=$(mktemp /root/inventory-med-api.env.recovery.XXXXXX)
WORK_ENV=$(mktemp /root/inventory-med-api.env.work.XXXXXX)
ENVIRONMENT_CHANGED=false

cleanup() {
    local exit_status=$?
    trap - EXIT
    unset ADMIN_EMAIL ADMIN_PASSWORD ADMIN_PASSWORD_CONFIRM SQLCMDPASSWORD
    rm -f "${WORK_ENV}"

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

sql_scalar() {
    local query=$1
    SQLCMDPASSWORD="${DB_MIGRATION_PASSWORD}" \
        /opt/mssql-tools18/bin/sqlcmd \
        -S 127.0.0.1 \
        -U "${DB_MIGRATION_USERNAME}" \
        -d inventory_med_core \
        -C -b -h -1 -W \
        -Q "${query}" |
        tr -d '\r' |
        xargs
}

ADMINISTRATOR_COUNT=$(sql_scalar \
    "SET NOCOUNT ON; SELECT COUNT(*) FROM dbo.system_user_role WHERE role = 'ADMIN_SISTEMA';")
if [[ ${ADMINISTRATOR_COUNT} != 0 ]]; then
    echo "A recuperação foi cancelada porque já existe um ADMIN_SISTEMA." >&2
    exit 1
fi

read -r -p "E-mail do administrador geral: " ADMIN_EMAIL </dev/tty
read -r -s -p "Senha temporária do administrador geral: " ADMIN_PASSWORD </dev/tty
echo >/dev/tty
read -r -s -p "Repita a senha temporária: " ADMIN_PASSWORD_CONFIRM </dev/tty
echo >/dev/tty

if [[ ! ${ADMIN_EMAIL} =~ ^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+$ ]]; then
    echo "O e-mail informado não é válido." >&2
    exit 1
fi
if [[ ${ADMIN_PASSWORD} != "${ADMIN_PASSWORD_CONFIRM}" ]]; then
    echo "As senhas informadas são diferentes." >&2
    exit 1
fi
if [[ ${#ADMIN_PASSWORD} -lt 12 ]] ||
   [[ ! ${ADMIN_PASSWORD} =~ [A-Z] ]] ||
   [[ ! ${ADMIN_PASSWORD} =~ [a-z] ]] ||
   [[ ! ${ADMIN_PASSWORD} =~ [0-9] ]] ||
   [[ ! ${ADMIN_PASSWORD} =~ [@#%+=._-] ]]; then
    echo "A senha deve ter 12 caracteres, maiúscula, minúscula, número e símbolo." >&2
    exit 1
fi
if [[ ! ${ADMIN_PASSWORD} =~ ^[A-Za-z0-9@#%+=._-]+$ ]]; then
    echo "Use somente letras, números e os símbolos @ # % + = . _ - na senha temporária." >&2
    exit 1
fi

EXISTING_EMAIL_COUNT=$(sql_scalar \
    "SET NOCOUNT ON; SELECT COUNT(*) FROM dbo.app_user WHERE email = LOWER('${ADMIN_EMAIL}');")
if [[ ${EXISTING_EMAIL_COUNT} != 0 ]]; then
    echo "Esse e-mail já pertence a outro usuário. Informe um e-mail exclusivo." >&2
    exit 1
fi

write_bootstrap_environment() {
    local enabled=$1
    local include_password=$2

    awk '
        !/^BOOTSTRAP_ENABLED=/ &&
        !/^BOOTSTRAP_SYSTEM_ADMIN_EMAIL=/ &&
        !/^BOOTSTRAP_SYSTEM_ADMIN_PASSWORD=/
    ' "${ENV_FILE}" > "${WORK_ENV}"
    printf 'BOOTSTRAP_ENABLED="%s"\n' "${enabled}" >> "${WORK_ENV}"
    printf 'BOOTSTRAP_SYSTEM_ADMIN_EMAIL="%s"\n' "${ADMIN_EMAIL}" >> "${WORK_ENV}"
    if [[ ${include_password} == true ]]; then
        printf 'BOOTSTRAP_SYSTEM_ADMIN_PASSWORD="%s"\n' "${ADMIN_PASSWORD}" >> "${WORK_ENV}"
    fi
    install -m 0600 -o root -g root "${WORK_ENV}" "${ENV_FILE}"
}

wait_for_health() {
    for _attempt in {1..30}; do
        if curl --fail --silent "${HEALTH_URL}" | grep -q '"status":"UP"'; then
            return 0
        fi
        sleep 1
    done
    echo "A API não ficou saudável dentro do tempo esperado." >&2
    return 1
}

write_bootstrap_environment true true
ENVIRONMENT_CHANGED=true
systemctl restart "${API_SERVICE}"
wait_for_health

CREATED_ADMINISTRATOR_COUNT=$(sql_scalar \
    "SET NOCOUNT ON; SELECT COUNT(*) FROM dbo.app_user u INNER JOIN dbo.system_user_role r ON r.user_id = u.id WHERE u.email = LOWER('${ADMIN_EMAIL}') AND u.active = 1 AND r.role = 'ADMIN_SISTEMA';")
if [[ ${CREATED_ADMINISTRATOR_COUNT} != 1 ]]; then
    echo "A conta administrativa não foi criada como esperado." >&2
    exit 1
fi

write_bootstrap_environment false false
systemctl restart "${API_SERVICE}"
wait_for_health

ENVIRONMENT_CHANGED=false
unset ADMIN_PASSWORD ADMIN_PASSWORD_CONFIRM
echo "Administrador geral criado, bootstrap desativado e senha temporária removida da configuração."
echo "Acesse o sistema com o e-mail informado e troque a senha no primeiro login."
