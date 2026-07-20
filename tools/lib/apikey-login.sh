# Implements the APIKEY function from `.aliases`; see the HOWTO comment there for usage.
#
# Must be sourced (done on demand by the APIKEY function), so that it can export
# HSADMINNG_API_KEY (and HSADMINNG_API_KEY_NAME) into the calling shell.
# The git-ignored, GPG-encrypted `.apikeys.gpg` properties-file itself is maintained
# by tools/lib/apikeys-file.py, in a section per Keycloak environment.

if [ "${BASH_SOURCE[0]}" == "$0" ]; then
    echo "ERROR: ${0} must be sourced, e.g. via the APIKEY function from .aliases" >&2
    exit 1
fi

function _apikeyLogin() {
    local issuer="${HSADMINNG_KEYCLOAK_ISSUER:-https://login.ng.hostsharing.net/realms/hs}"
    if [[ "${1:-}" == "--help" || "${1:-}" == "-h" || "${1:-}" == "-?" ]]; then
        source tools/lib/unindented.sh
        unindented <<'HELP'
            usage: APIKEY [--reset] <name> | APIKEY --remove <name> | APIKEY --transient | APIKEY --list | APIKEY --show

            Switches the HTTP function to an API-key instead of a JWT, by exporting it as
            HSADMINNG_API_KEY and unsetting the JWT bearer. Use LOGIN to switch back to a JWT
            and LOGOUT to drop the active identity.

            APIKEY <name>           uses the API-key stored under that name, asking for it just
                                    once, then storing it in the git-ignored `.apikeys.gpg` file
            APIKEY                  switches back to the last given API-key name, e.g. after a LOGIN
            APIKEY --reset <name>   asks for the API-key again and replaces the stored one
            APIKEY --remove <name>  removes the stored API-key
            APIKEY --transient      asks for an API-key which is NOT stored and lives only in this
                                    shell; deliberately prompted: an argument would end up in the
                                    shell history
            APIKEY --list           lists the key-names stored for the current Keycloak environment
            APIKEY --show           prints the decrypted `.apikeys.gpg` content of all Keycloak
                                    environments -- including the clear-text API-keys!
            APIKEY --help|-h|-?     shows this help

            Key-names must not start with '-'. API-keys are only ever stored GPG-encrypted, in
            `.apikeys.gpg` with name=key entries in a section per Keycloak environment
            ($HSADMINNG_KEYCLOAK_ISSUER), encrypted to $HSADMINNG_APIKEYS_GPG_RECIPIENT or, if
            that is not set, to your default GPG key -- see README.md for the GPG setup.
HELP
        return
    fi
    if [ "${1:-}" == "--list" ]; then
        echo "API-key names stored in .apikeys.gpg for [$issuer]:" >&2
        tools/lib/apikeys-file.py list "$issuer"
        return
    fi
    if [ "${1:-}" == "--show" ]; then
        tools/lib/apikeys-file.py show
        return
    fi
    if [ "${1:-}" == "--remove" ]; then
        if [ -z "${2:-}" ]; then
            echo "usage: APIKEY --remove <name>" >&2
            return 1
        fi
        tools/lib/apikeys-file.py remove "$issuer" "$2" || return 1
        if [ "$2" == "${HSADMINNG_API_KEY_NAME:-}" ]; then
            unset HSADMINNG_API_KEY HSADMINNG_API_KEY_NAME
        fi
        echo "removed the API-key '$2' for [$issuer]" >&2
        return
    fi
    if [ "${1:-}" == "--transient" ]; then
        # deliberately prompted, not taken as an argument: an argument would end up
        # in the shell history; a transient key exists only in this shell's environment
        local transientApiKey
        read -r -s -p "transient API-key (not stored): " transientApiKey
        echo >&2
        if [ -z "$transientApiKey" ]; then
            echo "ERROR: no API-key given" >&2
            return 1
        fi
        export HSADMINNG_API_KEY="$transientApiKey"
        unset HSADMINNG_API_KEY_NAME HSADMINNG_JWT_BEARER
        echo "HTTP requests now use a transient API-key, which lives only in this shell (use LOGIN to switch back to a JWT)" >&2
        return
    fi
    local reset=no
    if [ "${1:-}" == "--reset" ]; then
        reset=yes
        shift
    fi
    if [[ "${1:-}" == -* ]]; then
        # also rejects key-names starting with '-', to avoid confusion with real options
        echo "ERROR: unknown option '${1}', see APIKEY --help" >&2
        return 1
    fi
    if [ -n "${1:-}" ]; then
        export HSADMINNG_API_KEY_NAME=$1
    elif [ -z "${HSADMINNG_API_KEY_NAME:-}" ]; then
        echo "ERROR: there is no previously given API-key name to re-use, see APIKEY --help" >&2
        return 1
    fi
    local apiKey=""
    if [ "$reset" == no ]; then
        apiKey=$(tools/lib/apikeys-file.py get "$issuer" "$HSADMINNG_API_KEY_NAME") || return 1
    fi
    if [ -z "$apiKey" ]; then
        read -r -s -p "API-key '$HSADMINNG_API_KEY_NAME' for [$issuer]: " apiKey
        echo >&2
        if [ -z "$apiKey" ]; then
            echo "ERROR: no API-key given" >&2
            return 1
        fi
        printf '%s' "$apiKey" | tools/lib/apikeys-file.py store "$issuer" "$HSADMINNG_API_KEY_NAME" || return 1
        echo "stored the API-key '$HSADMINNG_API_KEY_NAME' for [$issuer] in the git-ignored, GPG-encrypted .apikeys.gpg file" >&2
    fi
    export HSADMINNG_API_KEY="$apiKey"
    # tools/http prefers the JWT over the API-key, thus unset it so the API-key takes effect
    unset HSADMINNG_JWT_BEARER
    echo "HTTP requests now use the API-key '$HSADMINNG_API_KEY_NAME' (use LOGIN to switch back to a JWT)" >&2
}

_apikeyLogin "$@"
_apikeyLoginExitCode=$?
unset -f _apikeyLogin
if [ "$_apikeyLoginExitCode" != 0 ]; then
    # end a calling script, but do not close an interactive shell
    if [[ $- == *i* ]]; then return $_apikeyLoginExitCode; else exit $_apikeyLoginExitCode; fi
fi
unset _apikeyLoginExitCode
