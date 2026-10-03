#!/usr/bin/env bash
set -Eeuo pipefail

if [[ ${EUID} -ne 0 ]]; then
    echo "Execute este script com sudo." >&2
    exit 1
fi

ENV_FILE=/etc/inventory-med/api.env
if [[ ! -f ${ENV_FILE} ]]; then
    echo "Arquivo ${ENV_FILE} não encontrado." >&2
    exit 1
fi

sed -i 's/^BOOTSTRAP_ENABLED=.*/BOOTSTRAP_ENABLED="false"/' "${ENV_FILE}"
sed -i '/^BOOTSTRAP_SYSTEM_ADMIN_PASSWORD=/d' "${ENV_FILE}"
systemctl restart inventory-med-api
echo "Bootstrap desativado e API reiniciada."
