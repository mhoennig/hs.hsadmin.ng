#!/usr/bin/env python3
"""Reads and writes the git-ignored, GPG-encrypted `.apikeys.gpg` properties-file in the
current directory, which stores API-keys by name in a section per stage, identified by its API base-URL,
because hsadmin-ng API-keys belong to a backend, not to a Keycloak:

    [https://backend.ng.hostsharing.net]
    hsadminng.provisioning.key=hsak_...
    hsadminng.keycloak.sync=hsak_...

usage:
    apikeys-file.py get <stage> <name>      # prints the stored API-key, or nothing if there is none
    apikeys-file.py store <stage> <name>    # reads the API-key from stdin (kept off the process list)
    apikeys-file.py remove <stage> <name>   # removes the stored API-key
    apikeys-file.py list <stage>            # prints the key-names stored for that stage, then legacy ones
    apikeys-file.py show <stage> <name>     # prints the API-key, falling back to a legacy entry
    apikeys-file.py stages                  # prints the stage sections, marking the legacy ones

Legacy entries are those in sections of a Keycloak issuer (containing `/realms/`), used by former
versions; `list` and `show` still find them, so they can be moved to their stage via `APIKEY --reset`.

API-keys are only ever stored encrypted: to HSADMINNG_APIKEYS_GPG_RECIPIENT or, if that is
not set, to your default GPG key (--default-recipient-self). A legacy plain `.apikeys` file
is migrated (encrypted, then deleted) on first use, whatever the command. Encrypting needs
just the public key; decrypting asks for the GPG passphrase, which gpg-agent caches
(see the README for the agent setup).

Used by the APIKEY shell function (via tools/lib/apikey-login.sh), see `.aliases`.
"""

import configparser
import io
import os
import re
import subprocess
import sys

PLAIN_FILE = ".apikeys"
ENCRYPTED_FILE = ".apikeys.gpg"
GPG_RECIPIENT_VARIABLE = "HSADMINNG_APIKEYS_GPG_RECIPIENT"


def fail(message):
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)


def run_gpg(arguments, **kwargs):
    try:
        return subprocess.run(["gpg", "--quiet", *arguments], **kwargs)
    except FileNotFoundError:
        fail("gpg is not installed, but API-keys are only ever stored GPG-encrypted;"
             " please install GnuPG")


def read_properties():
    properties = configparser.ConfigParser(interpolation=None)
    properties.optionxform = str  # keep key-names case-sensitive
    if os.path.exists(PLAIN_FILE):
        # legacy plain-text file, migrated (encrypted+deleted) by main() on first use
        properties.read(PLAIN_FILE)
    if os.path.exists(ENCRYPTED_FILE):
        gpg = run_gpg(["--decrypt", ENCRYPTED_FILE], stdout=subprocess.PIPE, text=True)
        if gpg.returncode != 0:
            fail(f"could not decrypt {ENCRYPTED_FILE} (see gpg message above)")
        properties.read_string(gpg.stdout)  # takes precedence over same-name legacy entries
    return properties


def write_properties(properties):
    content = io.StringIO()
    properties.write(content, space_around_delimiters=False)
    write_file(ENCRYPTED_FILE, lambda file: encrypt(content.getvalue(), file))
    if os.path.exists(PLAIN_FILE):
        os.remove(PLAIN_FILE)
        print(f"NOTE: migrated the plain {PLAIN_FILE} into the GPG-encrypted {ENCRYPTED_FILE}",
              file=sys.stderr)


def write_file(target_file, write_content):
    # written atomically and with owner-only access, the file contains secrets
    temp_file = target_file + ".tmp"
    try:
        with os.fdopen(os.open(temp_file, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600), "wb") as file:
            write_content(file)
        os.replace(temp_file, target_file)
    except BaseException:  # also covers the sys.exit(1) from fail()
        if os.path.exists(temp_file):
            os.remove(temp_file)
        raise


def encrypt(content, file):
    recipient = os.environ.get(GPG_RECIPIENT_VARIABLE, "").strip()
    recipient_arguments = ["--recipient", recipient] if recipient else ["--default-recipient-self"]
    gpg = run_gpg(["--batch", "--yes", "--encrypt", *recipient_arguments],
                  input=content.encode(), stdout=file)
    if gpg.returncode != 0:
        fail("could not encrypt the API-keys (see gpg message above): an own GPG key is"
             " required, e.g. created via `gpg --full-generate-key`;"
             f" set {GPG_RECIPIENT_VARIABLE} to choose a specific key")


def main():
    if len(sys.argv) < 2:
        fail(f"usage, see: {sys.argv[0]} --help")
    command, arguments = sys.argv[1], sys.argv[2:]
    if command == "--help":
        print(__doc__.strip())
        return
    if command not in ("get", "store", "remove", "list", "show", "stages"):
        fail(f"unknown command '{command}', expected get, store, remove, list, show, or stages")
    if len(arguments) != {"list": 1, "stages": 0}.get(command, 2):
        fail(f"wrong number of arguments for '{command}', see: {sys.argv[0]} --help")

    stage = arguments[0] if arguments else None
    if command != "stages" and not stage:
        fail("the <stage> (API base-URL) must not be empty")
    name = arguments[1] if len(arguments) > 1 else None
    if name is not None and not re.fullmatch(r"[A-Za-z0-9_][A-Za-z0-9_.-]*", name):
        fail(f"API-key name '{name}' must only contain letters, digits, '.', '-' and '_',"
             " and must not start with '-' or '.'")

    properties = read_properties()
    if os.path.exists(PLAIN_FILE):
        # never keep API-keys unencrypted on disk: migrate even for read-only commands
        write_properties(properties)
    if command == "get":
        if properties.has_option(stage, name):
            print(properties.get(stage, name))
    elif command == "store":
        api_key = sys.stdin.read().strip()
        if not api_key:
            fail("no API-key given on stdin")
        if not properties.has_section(stage):
            properties.add_section(stage)
        properties.set(stage, name, api_key)
        write_properties(properties)
    elif command == "remove":
        if not properties.has_option(stage, name):
            fail(f"no API-key named '{name}' is stored for [{stage}]")
        properties.remove_option(stage, name)
        if not properties.options(stage):
            properties.remove_section(stage)
        write_properties(properties)
    elif command == "list":
        if properties.has_section(stage):
            for key_name in properties.options(stage):
                print(key_name)
        for section in legacy_sections(properties):
            for key_name in properties.options(section):
                print(f"{key_name}  (legacy: [{section}])")
    elif command == "stages":
        for section in properties.sections():
            print(f"[{section}]" + ("  (legacy)" if section in legacy_sections(properties) else ""))
    elif command == "show":
        if properties.has_option(stage, name):
            print(properties.get(stage, name))
            return
        legacy = [section for section in legacy_sections(properties) if properties.has_option(section, name)]
        if not legacy:
            fail(f"no API-key named '{name}' is stored for [{stage}]")
        for section in legacy:
            print(f"legacy entry from [{section}], move it with: APIKEY --reset {name}", file=sys.stderr)
            print(properties.get(section, name))


def legacy_sections(properties):
    return [section for section in properties.sections() if "/realms/" in section]


if __name__ == "__main__":
    main()
