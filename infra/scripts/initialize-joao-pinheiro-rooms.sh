#!/usr/bin/env bash
set -Eeuo pipefail

API_BASE_URL=${API_BASE_URL:-http://127.0.0.1:8080/api/v1}
TARGET_HOSPITALS=(
    "HOSPITAL DE JOÃO PINHEIRO"
    "UPA DE JOÃO PINHEIRO"
)
ROOM_NUMBERS=(101 102 103 104 105)
BED_CODES=(A B C D)
CARE_UNIT_CODE=CLINICA_MEDICA
CARE_UNIT_NAME="CLÍNICA MÉDICA"
USER_AGENT="InventoryMED-StructureInitializer/1.0"

for command_name in curl python3; do
    if ! command -v "${command_name}" >/dev/null 2>&1; then
        echo "Comando obrigatório não encontrado: ${command_name}" >&2
        exit 1
    fi
done

TEMP_DIR=$(mktemp -d)
COOKIE_JAR="${TEMP_DIR}/cookies.txt"
LOGGED_IN=false

csrf_token() {
    local response_file="${TEMP_DIR}/csrf.json"
    local status
    status=$(curl --silent --show-error \
        --output "${response_file}" \
        --write-out '%{http_code}' \
        --cookie "${COOKIE_JAR}" \
        --cookie-jar "${COOKIE_JAR}" \
        --user-agent "${USER_AGENT}" \
        "${API_BASE_URL}/auth/csrf")
    if [[ ! ${status} =~ ^2 ]]; then
        echo "Não foi possível obter a proteção CSRF da API (HTTP ${status})." >&2
        return 1
    fi

    awk '$6 == "XSRF-TOKEN" { value = $7 } END { print value }' "${COOKIE_JAR}" |
        python3 -c 'import sys, urllib.parse; print(urllib.parse.unquote(sys.stdin.read().strip()))'
}

api_get() {
    local path=$1
    local output_file=$2
    local status
    status=$(curl --silent --show-error \
        --output "${output_file}" \
        --write-out '%{http_code}' \
        --cookie "${COOKIE_JAR}" \
        --cookie-jar "${COOKIE_JAR}" \
        --user-agent "${USER_AGENT}" \
        "${API_BASE_URL}${path}")
    if [[ ! ${status} =~ ^2 ]]; then
        echo "A API recusou GET ${path} (HTTP ${status})." >&2
        api_error "${output_file}"
        return 1
    fi
}

api_post() {
    local path=$1
    local payload=$2
    local output_file=$3
    local token status
    token=$(csrf_token)
    if [[ -z ${token} ]]; then
        echo "Token CSRF não encontrado." >&2
        return 1
    fi
    status=$(printf '%s' "${payload}" |
        curl --silent --show-error \
            --output "${output_file}" \
            --write-out '%{http_code}' \
            --request POST \
            --cookie "${COOKIE_JAR}" \
            --cookie-jar "${COOKIE_JAR}" \
            --header "Content-Type: application/json" \
            --header "X-XSRF-TOKEN: ${token}" \
            --user-agent "${USER_AGENT}" \
            --data-binary @- \
            "${API_BASE_URL}${path}")
    if [[ ! ${status} =~ ^2 ]]; then
        echo "A API recusou POST ${path} (HTTP ${status})." >&2
        api_error "${output_file}"
        return 1
    fi
}

api_error() {
    local response_file=$1
    python3 - "${response_file}" <<'PY' || true
import json
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
try:
    payload = json.loads(path.read_text(encoding="utf-8"))
    message = payload.get("message") or payload.get("error")
    if message:
        print(f"Motivo informado pela API: {message}", file=sys.stderr)
except Exception:
    pass
PY
}

close_session() {
    if [[ ${LOGGED_IN} != true ]]; then
        return
    fi
    local token
    token=$(csrf_token 2>/dev/null || true)
    if [[ -n ${token} ]]; then
        curl --silent --show-error \
            --output /dev/null \
            --request POST \
            --cookie "${COOKIE_JAR}" \
            --cookie-jar "${COOKIE_JAR}" \
            --header "X-XSRF-TOKEN: ${token}" \
            --user-agent "${USER_AGENT}" \
            "${API_BASE_URL}/auth/logout" || true
    fi
}

cleanup() {
    local exit_status=$?
    trap - EXIT
    close_session
    unset ADMIN_EMAIL ADMIN_PASSWORD LOGIN_PAYLOAD
    rm -rf "${TEMP_DIR}"
    exit "${exit_status}"
}
trap cleanup EXIT

read -r -p "E-mail do administrador geral: " ADMIN_EMAIL </dev/tty
read -r -s -p "Senha atual do administrador geral: " ADMIN_PASSWORD </dev/tty
echo >/dev/tty

LOGIN_PAYLOAD=$(ADMIN_EMAIL="${ADMIN_EMAIL}" ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
    python3 -c 'import json, os; print(json.dumps({"email": os.environ["ADMIN_EMAIL"], "password": os.environ["ADMIN_PASSWORD"]}))')
unset ADMIN_PASSWORD

LOGIN_RESPONSE="${TEMP_DIR}/login.json"
api_post "/auth/login" "${LOGIN_PAYLOAD}" "${LOGIN_RESPONSE}"
unset LOGIN_PAYLOAD
LOGGED_IN=true

python3 - "${LOGIN_RESPONSE}" <<'PY'
import json
import sys

payload = json.load(open(sys.argv[1], encoding="utf-8"))
user = payload.get("user") or {}
if "ADMIN_SISTEMA" not in (user.get("systemRoles") or []):
    raise SystemExit("A conta informada não possui o perfil ADMIN_SISTEMA.")
if user.get("mustChangePassword"):
    raise SystemExit("Troque a senha inicial no navegador antes de executar este procedimento.")
PY

HOSPITALS_RESPONSE="${TEMP_DIR}/hospitals.json"
api_get "/administration/hospitals" "${HOSPITALS_RESPONSE}"

for hospital_name in "${TARGET_HOSPITALS[@]}"; do
    mapfile -t hospital_record < <(python3 - "${HOSPITALS_RESPONSE}" "${hospital_name}" <<'PY'
import json
import sys

hospitals = json.load(open(sys.argv[1], encoding="utf-8"))
target = sys.argv[2].casefold()
hospital = next((item for item in hospitals if item["name"].casefold() == target), None)
if hospital:
    print(hospital["id"])
    print(hospital["status"])
PY
    )
    if [[ ${#hospital_record[@]} -ne 2 ]]; then
        echo "Hospital não encontrado: ${hospital_name}" >&2
        exit 1
    fi

    hospital_id=${hospital_record[0]}
    hospital_status=${hospital_record[1]}
    if [[ ${hospital_status} == PROVISIONING_FAILED ]]; then
        echo "Provisionando o banco exclusivo de ${hospital_name}..."
        PROVISION_RESPONSE="${TEMP_DIR}/provision-${hospital_id}.json"
        api_post "/administration/hospitals/${hospital_id}/provision" '{}' "${PROVISION_RESPONSE}"
        hospital_status=$(python3 -c \
            'import json, sys; print(json.load(open(sys.argv[1], encoding="utf-8"))["status"])' \
            "${PROVISION_RESPONSE}")
    fi
    if [[ ${hospital_status} != ACTIVE ]]; then
        echo "${hospital_name} não está ativo; situação atual: ${hospital_status}." >&2
        exit 1
    fi

    echo "Preparando a estrutura de ${hospital_name}..."
    STRUCTURE_RESPONSE="${TEMP_DIR}/structure-${hospital_id}.json"
    api_get "/administration/hospitals/${hospital_id}/structure" "${STRUCTURE_RESPONSE}"
    care_unit_id=$(python3 - "${STRUCTURE_RESPONSE}" "${CARE_UNIT_CODE}" <<'PY'
import json
import sys

structure = json.load(open(sys.argv[1], encoding="utf-8"))
target = sys.argv[2].casefold()
unit = next((item for item in structure.get("careUnits", []) if item["code"].casefold() == target), None)
if unit:
    print(unit["id"])
PY
    )
    if [[ -z ${care_unit_id} ]]; then
        UNIT_RESPONSE="${TEMP_DIR}/unit-${hospital_id}.json"
        api_post \
            "/administration/hospitals/${hospital_id}/structure/care-units" \
            "{\"name\":\"${CARE_UNIT_NAME}\",\"code\":\"${CARE_UNIT_CODE}\",\"displayOrder\":10,\"active\":true}" \
            "${UNIT_RESPONSE}"
        care_unit_id=$(python3 -c \
            'import json, sys; print(json.load(open(sys.argv[1], encoding="utf-8"))["id"])' \
            "${UNIT_RESPONSE}")
        echo "  Unidade ${CARE_UNIT_NAME} criada."
    fi

    for room_number in "${ROOM_NUMBERS[@]}"; do
        room_code="Q${room_number}"
        room_name="QUARTO ${room_number}"
        api_get "/administration/hospitals/${hospital_id}/structure" "${STRUCTURE_RESPONSE}"
        mapfile -t room_record < <(python3 - \
            "${STRUCTURE_RESPONSE}" "${care_unit_id}" "${room_code}" <<'PY'
import json
import sys

structure = json.load(open(sys.argv[1], encoding="utf-8"))
unit_id, room_code = sys.argv[2], sys.argv[3].casefold()
unit = next((item for item in structure.get("careUnits", []) if item["id"] == unit_id), None)
room = next((item for item in (unit or {}).get("rooms", []) if item["code"].casefold() == room_code), None)
if room:
    print(room["id"])
    for bed in room.get("beds", []):
        print(bed["code"])
PY
        )

        if [[ ${#room_record[@]} -eq 0 ]]; then
            ROOM_RESPONSE="${TEMP_DIR}/room-${hospital_id}-${room_number}.json"
            api_post \
                "/administration/hospitals/${hospital_id}/structure/rooms" \
                "{\"careUnitId\":\"${care_unit_id}\",\"name\":\"${room_name}\",\"code\":\"${room_code}\",\"floorName\":null,\"displayOrder\":${room_number},\"active\":true}" \
                "${ROOM_RESPONSE}"
            room_id=$(python3 -c \
                'import json, sys; print(json.load(open(sys.argv[1], encoding="utf-8"))["id"])' \
                "${ROOM_RESPONSE}")
            existing_beds=()
            echo "  ${room_name} criado."
        else
            room_id=${room_record[0]}
            existing_beds=("${room_record[@]:1}")
        fi

        bed_order=0
        for bed_code in "${BED_CODES[@]}"; do
            bed_order=$((bed_order + 10))
            if printf '%s\n' "${existing_beds[@]:-}" | grep -Fxq "${bed_code}"; then
                continue
            fi
            BED_RESPONSE="${TEMP_DIR}/bed-${hospital_id}-${room_number}-${bed_code}.json"
            api_post \
                "/administration/hospitals/${hospital_id}/structure/beds" \
                "{\"roomId\":\"${room_id}\",\"code\":\"${bed_code}\",\"status\":\"AVAILABLE\",\"displayOrder\":${bed_order},\"active\":true}" \
                "${BED_RESPONSE}"
            echo "    LEITO ${bed_code} criado."
        done
    done

    api_get "/administration/hospitals/${hospital_id}/structure" "${STRUCTURE_RESPONSE}"
    python3 - "${STRUCTURE_RESPONSE}" "${care_unit_id}" <<'PY'
import json
import sys

structure = json.load(open(sys.argv[1], encoding="utf-8"))
unit = next(item for item in structure["careUnits"] if item["id"] == sys.argv[2])
target_rooms = [room for room in unit["rooms"] if room["code"] in {"Q101", "Q102", "Q103", "Q104", "Q105"}]
if len(target_rooms) != 5 or any({bed["code"] for bed in room["beds"]} < {"A", "B", "C", "D"} for room in target_rooms):
    raise SystemExit("A validação final da estrutura hospitalar falhou.")
print(f"{len(target_rooms)} quartos e {sum(len(room['beds']) for room in target_rooms)} leitos validados.")
PY
done

echo "Estrutura concluída nos dois hospitais: 10 quartos e 40 leitos disponíveis."
