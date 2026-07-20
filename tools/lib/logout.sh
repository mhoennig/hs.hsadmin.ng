# Implements the LOGOUT function from `.aliases`; run `LOGOUT --help` for usage.
#
# Must be sourced (done by the LOGOUT function), so that it can unset HSADMINNG_JWT_BEARER /
# HSADMINNG_API_KEY (and, with --all, more) in the calling shell.

if [ "${BASH_SOURCE[0]}" == "$0" ]; then
    echo "ERROR: ${0} must be sourced, e.g. via the LOGOUT function from .aliases" >&2
    exit 1
fi

function _logout() {
    if [[ "${1:-}" == "-h" || "${1:-}" == "-?" || "${1:-}" == "--help" ]]; then
        source tools/lib/unindented.sh
        unindented <<'HELP'
            usage: LOGOUT [--all]

            Drops the active identity, so HTTP requests are no longer authenticated.

            LOGOUT                unsets the JWT (HSADMINNG_JWT_BEARER) and the API-key (HSADMINNG_API_KEY)
            LOGOUT --all          also forgets the LOGIN username+password and the last API-key name
            LOGOUT -h|-?|--help   shows this help

            API-keys stored in the git-ignored .apikeys.gpg file are kept; replace them via APIKEY --reset.
            Use LOGIN or APIKEY to authenticate again.
HELP
        return
    fi
    unset HSADMINNG_JWT_BEARER HSADMINNG_API_KEY
    if [ "${1:-}" == "--all" ]; then
        unset HSADMINNG_API_KEY_NAME HSADMINNG_JWT_USERNAME HSADMINNG_JWT_PASSWORD
        echo "logged out, also forgot the LOGIN username+password and the last API-key name" >&2
    elif [ -n "${1:-}" ]; then
        echo "usage: LOGOUT [--all], see LOGOUT --help" >&2
        return 1
    else
        echo "logged out, HTTP requests are no longer authenticated (use LOGIN or APIKEY to log in again)" >&2
    fi
}

_logout "$@"
_logoutExitCode=$?
unset -f _logout
if [ "$_logoutExitCode" != 0 ]; then
    # end a calling script, but do not close an interactive shell
    if [[ $- == *i* ]]; then return $_logoutExitCode; else exit $_logoutExitCode; fi
fi
unset _logoutExitCode
