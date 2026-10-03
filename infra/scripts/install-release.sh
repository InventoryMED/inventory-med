#!/usr/bin/env bash
set -Eeuo pipefail

if [[ ${EUID} -ne 0 ]]; then
    echo "Execute este script com sudo." >&2
    exit 1
fi
if [[ $# -ne 2 ]]; then
    echo "Uso: install-release.sh DIRETORIO_DO_PACOTE IDENTIFICADOR_DA_VERSAO" >&2
    exit 1
fi

PACKAGE_DIR=$(realpath "$1")
RELEASE_ID=$2
API_SOURCE="${PACKAGE_DIR}/inventory-med-api.jar"
FRONTEND_SOURCE="${PACKAGE_DIR}/frontend"
API_TARGET="/opt/inventory-med/api/inventory-med-api-${RELEASE_ID}.jar"
FRONTEND_TARGET="/var/www/inventory-med/releases/${RELEASE_ID}"

if [[ ! ${RELEASE_ID} =~ ^[0-9a-f]{7,40}$ ]]; then
    echo "O identificador deve ser um commit Git hexadecimal." >&2
    exit 1
fi
if [[ ! -f ${API_SOURCE} || ! -f ${FRONTEND_SOURCE}/index.html ]]; then
    echo "Pacote inválido: JAR ou index.html ausente." >&2
    exit 1
fi
if [[ ! -f /etc/inventory-med/api.env ]]; then
    echo "Configure /etc/inventory-med/api.env antes de instalar a versão." >&2
    exit 1
fi

PREVIOUS_API=$(readlink -f /opt/inventory-med/api/inventory-med-api.jar 2>/dev/null || true)
PREVIOUS_FRONTEND=$(readlink -f /var/www/inventory-med/current 2>/dev/null || true)

# A função é chamada indiretamente pelo trap de erro abaixo.
# shellcheck disable=SC2329
rollback() {
    echo "Falha detectada; restaurando a versão anterior." >&2
    if [[ -n ${PREVIOUS_API} && -f ${PREVIOUS_API} ]]; then
        ln -sfn "${PREVIOUS_API}" /opt/inventory-med/api/inventory-med-api.jar
    else
        rm -f /opt/inventory-med/api/inventory-med-api.jar
    fi
    if [[ -n ${PREVIOUS_FRONTEND} && -d ${PREVIOUS_FRONTEND} ]]; then
        ln -sfn "${PREVIOUS_FRONTEND}" /var/www/inventory-med/current
    else
        rm -f /var/www/inventory-med/current
    fi
    systemctl restart inventory-med-api 2>/dev/null || true
    systemctl reload nginx 2>/dev/null || true
}
trap rollback ERR

install -m 0640 -o root -g inventorymed "${API_SOURCE}" "${API_TARGET}"
install -d -m 0755 -o root -g root "${FRONTEND_TARGET}"
rsync -a --delete "${FRONTEND_SOURCE}/" "${FRONTEND_TARGET}/"
find "${FRONTEND_TARGET}" -type d -exec chmod 0755 {} +
find "${FRONTEND_TARGET}" -type f -exec chmod 0644 {} +
chown -R root:root "${FRONTEND_TARGET}"

ln -sfn "${API_TARGET}" /opt/inventory-med/api/inventory-med-api.jar
ln -sfn "${FRONTEND_TARGET}" /var/www/inventory-med/current

systemctl enable inventory-med-api
systemctl restart inventory-med-api
nginx -t
systemctl reload nginx

for _attempt in {1..30}; do
    if curl --fail --silent http://127.0.0.1:8080/api/v1/actuator/health | grep -q '"status":"UP"'; then
        trap - ERR
        echo "Versão ${RELEASE_ID} instalada e saudável."
        exit 0
    fi
    sleep 1
done

echo "A API não ficou saudável dentro do tempo esperado." >&2
exit 1
