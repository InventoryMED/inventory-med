#!/usr/bin/env bash
set -Eeuo pipefail

if [[ ${EUID} -ne 0 ]]; then
    echo "Execute este script com sudo." >&2
    exit 1
fi

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
INFRA_DIR=$(cd -- "${SCRIPT_DIR}/.." && pwd)

apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y nginx git curl ca-certificates rsync openssl

if ! command -v java >/dev/null 2>&1; then
    if apt-cache show msopenjdk-21 >/dev/null 2>&1; then
        DEBIAN_FRONTEND=noninteractive apt-get install -y msopenjdk-21
    elif apt-cache show openjdk-21-jre-headless >/dev/null 2>&1; then
        DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-21-jre-headless
    else
        echo "Java 21 não está disponível nos repositórios configurados." >&2
        exit 1
    fi
fi

JAVA_MAJOR=$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')
if [[ ${JAVA_MAJOR} != "21" ]]; then
    echo "A versão ativa do Java precisa ser 21; encontrada: ${JAVA_MAJOR:-desconhecida}." >&2
    exit 1
fi

if ! id inventorymed >/dev/null 2>&1; then
    useradd --system --home-dir /opt/inventory-med --shell /usr/sbin/nologin inventorymed
fi

install -d -m 0750 -o root -g inventorymed /opt/inventory-med/api
install -d -m 0755 -o root -g root /var/www/inventory-med/releases
install -d -m 0700 -o root -g root /etc/inventory-med

install -m 0644 -o root -g root \
    "${INFRA_DIR}/systemd/inventory-med-api.service" \
    /etc/systemd/system/inventory-med-api.service
install -m 0644 -o root -g root \
    "${INFRA_DIR}/nginx/inventory-med.conf" \
    /etc/nginx/sites-available/inventory-med.conf

ln -sfn /etc/nginx/sites-available/inventory-med.conf /etc/nginx/sites-enabled/inventory-med.conf
rm -f /etc/nginx/sites-enabled/default

systemctl daemon-reload
systemctl enable nginx
nginx -t
systemctl reload nginx

if command -v ufw >/dev/null 2>&1; then
    ufw allow 80/tcp comment 'Inventory MED HTTP homologacao'
fi

echo "Provisionamento-base concluído. Java 21, Nginx, usuário e diretórios estão prontos."
