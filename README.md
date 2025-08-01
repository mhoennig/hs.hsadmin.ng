# hsadminNg Development

(The origin repository for this project can be found at [Hostsharing eG](https://git.hostsharing.net/hostsharing/hs.hsadmin.ng).)

This documents gives an overview of the development environment and tools.
For architecture consider the files in the `doc` and `adr` folder.

<!-- generated TOC begin: -->
- [Setting up the Development Environment](#setting-up-the-development-environment)
    - [PostgreSQL Server](#postgresql-server)
    - [Markdown](#markdown)
        - [Render Markdown embedded PlantUML](#render-markdown-embedded-plantuml)
        - [Render Markdown Embedded Mermaid Diagrams](#render-markdown-embedded-mermaid-diagrams)
    - [IDE Specific Settings](#ide-specific-settings)
        - [IntelliJ IDEA](#intellij-idea)
    - [Other Tools](#other-tools)
- [Running the SQL files](#running-the-sql-files)
    - [For RBAC](#for-rbac)
    - [For Historization](#for-historization)
- [Coding Guidelines](#coding-guidelines)
    - [Directory and Package Structure](#directory-and-package-structure)
        - [General Directory Structure](#general-directory-structure)
        - [Source Code Package Structure](#source-code-package-structure)
    - [Run Tests from Command Line](#run-tests-from-command-line)
    - [Spotless Code Formatting](#spotless-code-formatting)
    - [JaCoCo Test Code Coverage Check](#jacoco-test-code-coverage-check)
    - [PiTest Mutation Testing](#pitest-mutation-testing)
        - [Remark](#remark)
    - [OWASP Security Vulnerability Check](#owasp-security-vulnerability-check)
    - [Dependency-License-Compatibility](#dependency-license-compatibility)
    - [Dependency Version Upgrade](#dependency-version-upgrade)
- [Biggest Flaws in our Architecture](#biggest-flaws-in-our-architecture)
    - [The RBAC System is too Complicated](#the-rbac-system-is-too-complicated)
    - [The Mapper is Error-Prone](#the-mapper-is-error-prone)
    - [Too Many Business-Rules Implemented in Controllers](#too-many-business-rules-implemented-in-controllers)
- [How To ...](#how-to-...)
    - [How to Run the Application With Other Profiles, e.g. production](#)
    - [How to Do a Clean Run of the Application](#how-to-do-a-clean-run-of-the-application)
    - [How to Configure .pgpass for the Default PostgreSQL Database?](#how-to-configure-.pgpass-for-the-default-postgresql-database?)
    - [How to Run the Tests Against a Local User-Space Podman Daemon?](#how-to-run-the-tests-against-a-local-user-space-podman-daemon?)
        - [Install and Run Podman](#install-and-run-podman)
        - [Use the Command Line to Run the Tests Against the Podman Daemon ](#use-the-command-line-to-run-the-tests-against-the-podman-daemon-)
        - [Use IntelliJ IDEA Run the Tests Against the Podman Daemon](#use-intellij-idea-run-the-tests-against-the-podman-daemon)
        - [~/.testcontainers.properties](#~/.testcontainers.properties)
    - [How to Run the Tests Against a Remote Podman or Docker Daemon?](#how-to-run-the-tests-against-a-remote-podman-or-docker-daemon?)
    - [How to Run the Application on a Different Port?](#how-to-run-the-application-on-a-different-port?)
    - [How to Use a Persistent Database for Integration Tests?](#how-to-use-a-persistent-database-for-integration-tests?)
    - [How to Amend Liquibase SQL Changesets?](#how-to-amend-liquibase-sql-changesets?)
    - [How to Re-Generate Spring-Controller-Interfaces from OpenAPI specs?](#how-to-re-generate-spring-controller-interfaces-from-openapi-specs?)
    - [How to Generate Database Table Diagrams?](#how-to-generate-database-table-diagrams?)
    - [How to Add (Real) Admin Users](#how-to-add-(real)-admin-users)
- [Further Documentation](#further-documentation)
<!-- generated TOC end. -->

## Setting up the Development Environment

All instructions assume that you're using a current _Linux_ or _MacOS_ operating system.
Everything is tested on _Ubuntu Linux 22.04_ and _MacOS Monterey (12.4)_.

To be able to build and run the Java Spring Boot application, you need the following tools:

- Docker 20.x (on MacOS you also need *Docker Desktop* or similar) or Podman
- optionally: PostgreSQL Server 15.5-bookworm, if you want to use the database directly, not just via Docker
  (see instructions below to install and run in Docker)
- The matching Java JDK at will be automatically installed by Gradle toolchain support to `~/.gradle/jdks/`.
- You also might need an IDE (e.g. *IntelliJ IDEA* or *Eclipse* or *VS Code* with *[STS](https://spring.io/tools)* and a GUI Frontend for *PostgreSQL* like *Postbird*.
- Python 3 is expected in /usr/bin/python3 if you want to run the `howto` tool (see `bin/howto`)

If you have at least Docker and the Java JDK installed in appropriate versions and in your `PATH`, then you can start like this:

    cd your-hsadmin-ng-directory
    
    source .aliases     # creates some comfortable bash aliases, e.g. 'gw'='./gradlew'
    gw                  # initially downloads the configured Gradle version into the project

    gw test             # compiles and runs unit- and integration-tests - takes >10min even on a fast machine
                        # `gw test` does NOT run import- and scenario-tests.
                        # Use `gw-test` instead to make sure .tc-environment is sourced.
    gw scenarioTest     # compiles and scenario-tests - takes ~1min on a decent machine
                        # Use `gw-test scenarioTest` instead to make sure .tc-environment is sourced.
                        
    howto test          # shows more test information about how to run tests
    
    # if the container has not been built yet, run this:
    pg-sql-run          # downloads + runs PostgreSQL in a Docker container on localhost:5432

    # if the container has been built already and you want to keep the data, run this:
    pg-sql-start

Next, compile and run the application on `localhost:8080` and the management server on `localhost:8081`:

    # this disables CAS-authentication, for using the REST-API with CAS-authentication, see `bin/cas-curl`.
    export HSADMINNG_CAS_SERVER=

    # this runs the application with test-data and all modules:
    gw bootRun --args='--spring.profiles.active=dev,fakeCasAuthenticator,complete,test-data'

    # there is also an alias which takes an optional port as an argument:
    gw-bootRun 8888

The meaning of these profiles is:

- **dev**: the PostgreSQL users are created via Liquibase
- **fakeCasAuthenticator**: The username is simply taken from whatever is after "Bearer " in the "Authorization" header.
- **complete**: all modules are started
- **test-data**: some test data inserted 

Now we can access the REST API, e.g. using curl:

    # the following command should reply with "pong":
    curl -f -s http://localhost:8080/api/ping

    # the following command should return a JSON array with just all customers:
    curl -f -s\
        -H 'Authorization: Bearer superuser-alex@hostsharing.net' \
        http://localhost:8080/api/test/customers \
    | jq # just if `jq` is installed, to prettyprint the output

    # the following command should return a JSON array with just all packages visible for the admin of the customer yyy:
    curl -f -s\
        -H 'Authorization: Bearer superuser-alex@hostsharing.net' -H 'assumed-roles: rbactest.customer#yyy:ADMIN' \
        http://localhost:8080/api/test/packages \
    | jq

    # add a new customer
    curl -f -s\
        -H 'Authorization: Bearer superuser-alex@hostsharing.net' -H "Content-Type: application/json" \
        -d '{ "prefix":"ttt", "reference":80001, "adminUserName":"admin@ttt.example.com" }' \
        -X POST http://localhost:8080/api/test/customers \
    | jq

If you wonder who 'superuser-alex@hostsharing.net' and 'superuser-fran@hostsharing.net' are and where the data comes from:
Mike and Sven are just example global admin accounts as part of the example data which is automatically inserted in Testcontainers and Development environments.
Also try for example 'admin@xxx.example.com' or 'unknown@example.org'.

If you want a formatted JSON output, you can pipe the result to `jq` or similar.

And to see the full, currently implemented, API, open http://localhost:8080/swagger-ui/index.html).
For a locally running app without CAS-authentication (export HSADMINNG_CAS_SERVER=''), 
authorize using the name of the subject (e.g. "superuser-alex@hostsharing.net" in case of test-data).
Otherwise, use a valid CAS-ticket.

If you want to run the application with real CAS-Authentication:

    # set the CAS-SERVER-Root, also see `bin/cas-curl`.
    export HSADMINNG_CAS_SERVER=https://login.hostsharing.net # or whatever your CAS-Server-URL you want to use

    # run the application against the real CAS authenticator
    gw bootRun --args='--spring.profiles.active=dev,realCasAuthenticator,complete,test-data'


### PostgreSQL Server

You could use any PostgreSQL Server (version 15) installed on your machine.
You might amend the port and user settings in `src/main/resources/application.yml`, though.

But the easiest way to run PostgreSQL is via Docker.

Initially, pull an image compatible to current PostgreSQL version of Hostsharing:

    docker pull postgres:15.5-bookworm 

<big>**&#9888;**</big>
If we switch the version, please also amend the documentation as well as the aliases file. Thanks! 

Create and run a container with the given PostgreSQL version:

    docker run --name hsadmin-ng-postgres -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres:15.5-bookworm

    # or via alias: 
    pg-sql-run

To check if the PostgreSQL container is running, the following command should list a container with the name "hsadmin-ng-postgres": 

    docker container ls 

Stop the PostgreSQL container:
    
    docker stop hsadmin-ng-postgres
    # or via alias: pg-sql-stop

Start the PostgreSQL container again:

    docker container start hsadmin-ng-postgres
    # or via alias: pg-sql-start

Remove the PostgreSQL container:

    docker rm hsadmin-ng-postgres
    
    # or via alias:
    pg-sql-remove

To reset to a clean database, use:

    pg-sql-stop; pg-sql-remove; pg-sql-run

    # or via alias:
    pg-sql-reset

After the PostgreSQL container is removed, you need to create it again as shown in "Create and run ..." above.

Given the container is running, to create a backup in ~/backup, run:

    docker exec -i hsadmin-ng-postgres /usr/bin/pg_dump --clean --create -U postgres postgres | gzip -9 > ~/backup/hsadmin-ng-postgres.sql.gz

    # or via alias:
    pg-sql-backup >~/backup/hsadmin-ng-postgres.sql.gz


Again, given the container is running, to restore the backup from ~/backup, run:

    gunzip --stdout --keep ~/backup/hsadmin-ng-postgres.sql.gz | docker exec -i hsadmin-ng-postgres psql -U postgres -d postgres

    # or via alias:
    pg-sql-restore <~/backup/hsadmin-ng-postgres.sql.gz


### Markdown

To generate the TOC (Table of Contents), a little bash script from a
[Blog Article](https://medium.com/@acrodriguez/one-liner-to-generate-a-markdown-toc-f5292112fd14) was used.

Given this is in PATH as `md-toc`, use:

```shell
md-toc <README.md 2 4 | cut -c5-
```

To render the Markdown files, especially to watch embedded PlantUML diagrams, you can use one of the following methods:

#### Render Markdown embedded PlantUML

Can you see the following diagram right in your IDE?
I mean a real graphic diagram, not just some markup code.

```plantuml
@startuml
me -> you: Can you see this diagram?
you -> me: Sorry, I don't :-(
me -> you: Install some tooling!
@enduml
```

If not, you need to install some tooling.

##### for IntelliJ IDEA (or derived products)

You just need the bundled Markdown plugin enabled and install and activate the PlantUML plugin in its [settings](jetbrains://idea/settings?name=Languages+%26+Frameworks--Markdown). 

You might also need to install Graphviz on your operating system.
For Debian-based Linux systems this might work:

```sh
sudo apt install graphviz
```


##### Ubuntu Linux command line

1. Install Pandoc with some extra libraries:
```shell
sudo apt-get install pandoc texlive-latex-base texlive-fonts-recommended texlive-extra-utils texlive-latex-extra pandoc-plantuml-filter 
```

2. Install mermaid-filter, e.g. this way:
```shell
npm install -g mermaid-filter
```

3. Run Pandoc to generate a PDF from a Markdown file with PlantUML and Mermaid diagrams:
```shell
pandoc --filter mermaid-filter --filter pandoc-plantuml rbac.md -o rbac.pdf
```

##### for other IDEs / operating systems

If you have figured out how it works, please add instructions above this section.

#### Render Markdown Embedded Mermaid Diagrams

The source of RBAC role diagrams are much easier to read with Mermaid than with PlantUML or GraphViz, that's also the main reason Mermaid is used.

Can you see the following diagram right in your IDE?
I mean a real graphic diagram, not just some markup code.
@startuml
me -> you: Can you see this diagram?
you -> me: Sorry, I don't :-(
me -> you: Install some tooling!
@enduml

```mermaid
graph TD;
    A[Can you see this diagram?];
    A --> yes;
    A --> no;
    no --> F[Follow the instructions below!]
    F --> yes
    yes --> E[Then everything is fine.]
```

If not, you need to install some tooling.

##### for IntelliJ IDEA (or derived products)

1. Activate the bundled Jebrains Markdown PlantUML Extension via
    [File | Settings | Languages & Frameworks | Markdown](jetbrains://idea/settings?name=Languages+%26+Frameworks--Markdown)  
2. Install the Jetbrains Mermaid plugin: https://plugins.jetbrains.com/plugin/20146-mermaid, it also works embedded in Markdown files.

Now the above diagram should be rendered.

##### for other IDEs / command-line / operating systems

If you have figured out how it works, please add instructions above this section.

### IDE Specific Settings

#### IntelliJ IDEA

##### Build Settings

Go to [Gradle Settings}(jetbrains://idea/settings?name=Build%2C+Execution%2C+Deployment--Build+Tools--Gradle) and select "Build and run using" and "Run tests using" both to "gradle".
Otherwise, settings from `build.gradle.kts`, like compiler arguments, are not applied when compiling through *IntelliJ IDEA*.

##### Annotation Processor

Go to [Annotations Processors](jetbrains://idea/settings?name=Build%2C+Execution%2C+Deployment--Compiler--Annotation+Processors) and activate annotation processing.
Otherwise, *IntelliJ IDEA* can't see *Lombok* generated classes 
and will show false errors (missing identifiers).


##### Suggested Plugins

- [Jetbrains Mermaid Integration](https://plugins.jetbrains.com/plugin/20146-mermaid)
- [Vojtěch Krása PlantUML Integration](https://plugins.jetbrains.com/plugin/7017-plantuml-integration)

### Other Tools

**jq**: a JSON formatter. 
On _Debian_'oid systems you can install it with `sudo apt-get install jq`.
On _MacOS_ you can install it with `brew install jq`, given you have _brew_ installed.

## Running the SQL files

### For RBAC

The Schema is automatically created via *Liquibase*, a database migration library.
Currently, also some test data is automatically created.

To increase the amount of test data, increase the number of generated customers in `2022-07-28-051-hs-customer.sql` and run that

If you already have data, e.g. for customers 0..999 (thus with reference numbers 10000..10999) and want to add another 1000 customers, amend the for loop to 1000...1999 and also uncomment and amend the `CONTINUE WHEN` or `WHERE` conditions in the other test data generators, using the first new customer reference number (in the example that's 11000).

### For Historization

The historization is not yet integrated into the *Liquibase*-scripts.
You can explore the prototype as follows:

- start with an empty database
  (the example tables are currently not compatible with RBAC),
- then run `historization.sql` in the database,
- finally run `examples.sql` in the database.

## Coding Guidelines

### Directory and Package Structure

#### General Directory Structure
 
`.aider.conf.yml`
    Configuration for the _aider AI_ coding agent.

`.aliases`
    Shell-aliases for common tasks.

`build/`
    Output directory for gradle build results. Ignored by git.

`build.gradle.kts`
    Gradle build-file (Kotlin-Script). Contains dependencies and build configurations.

`CONVENTIONS.md`
    Coding conventions for use by an AI agent.

`doc/`
    Contains project documentation.

`.editorconfig`
    Rules for indentation etc. considered by many code editors.

`etc/`
    Miscellaneous configurations, as long as these don't need to be in the rood directory. 

`.git/`
    Git repository. Do not temper with this!

`.gitattributes`
    Git configurations regarding text file format conversion between operating systems. 

`.gitignore`
    Git configuration regarding which files and directories should be ignored (not checked in).

`.gradle/`
    Config files created by `gradle wrapper`. Ignored by git.

`gradle/`
    The gradle distribution downloaded by `gradle wrapper`. Ignored by git.

`gradlew` and `gradlew.bat` use these batches to run gradle for builds etc. 

`.idea/` (optional)
    Config and cache files created by *IntelliJ IDEA*. Ignore by git.

`LICENSE.md`
    Contains the license used for this software.

`out/` (optional)
    Build output created by *IntelliJ IDEA". Ignored by git. 

`README.md`
    Contains an overview about how to build the project and the used tools. 

`.run/` (optional)
    Created by *IntelliJ IDEA* to contain run and debug configurations.

`settings.gradle`
    Configuration file for gradle.

`sql/`
    Contains SQL scripts for experiments and useful tasks.
    Most of this will sooner or later be moved to Liquibase-scripts.

`src/`
    The actual source-code, see [Source Code Package Structure](#source-code-package-structure) for details.

`tools/`
    Some shell-scripts to useful tasks.


#### Source Code Package Structure

For the source code itself, the general standard Java directory structure is used, where productive and test code are separated like this:

```
src
    main/
        java/
            net.hostsharing.hasadminng/
        resources/
        
    test/
        java/
            net.hostsharing.hasadminng/
        resources/
```

The Java package structure below contains:

- config and global (utility) packages,
  these should not access any other packages within the project
- rbac, containing all packages related to the RBAC subsystem
- hs, containing Hostsharing business object related packages

Underneath of rbac and hs, the structure is business oriented, NOT technical / layer -oriented.

Some of these rules are checked with *ArchUnit* unit tests.


### Run Tests from Command Line

Run all unit-, integration- and acceptance-tests which have not yet been passed with the current source code:

```shell
gw test # uses the current environment, especially HSADMINNG_POSTGRES_JDBC_URL
```

If the referenced database is not empty, the tests might fail.

To explicitly use the Testcontainers-environment, run:

```shell
gw-test # uses the environment from .tc-environment
```

Force running all tests:

```shell
gw-test --rerun 
```

To find more options about running tests, try `howto test`.


### Spotless Code Formatting

Code formatting for Java is checked via *spotless*.
To apply formatting rules, use:

```shell
gw-spotless
```

The gradle task spotlessCheck is also included in `gw build` and `gw check`,
thus if the formatting is not compliant to the rules, the build is going to fail. 


### JaCoCo Test Code Coverage Check

This project uses the JaCoCo test code coverage report with limit checks.
It can be executed with:

```shell
gw jacocoTestReport
```

This task is also automatically run after `gw test`.
It is configured in [build.gradle.kts](build.gradle.kts).

A report is generated under [build/reports/jacoco/tests/test/index.html](./build/reports/jacoco/test/html/index.html).

Additionally, quality limits are checked via:

```shell
gw jacocoTestCoverageVerification
```

This task is also executed as part of `gw check`.


### PiTest Mutation Testing

PiTest mutation testing is configured for unit tests.
It can be executed with:

```shell
gw pitest
```

Classes to be scanned, tests to be executed and thresholds are configured in [build.gradle.kts](build.gradle.kts).

A report is generated under [build/reports/pitest/index.html](./build/reports/pitest/index.html).
A link to the report is also printed after the `pitest` run.

<!-- TODO.test: This task is also executed as part of `gw check`. -->

#### Remark

In this project, there is a large amount of code is in *plsql*, especially for RBAC. 
*Java* ist mostly used for mapping and validating REST calls to database queries.
This mapping ist mostly done through *Spring* annotations and other implicit code.

Therefore, there are only few unit tests and thus mutation testing has limited value.
We'll see if this changes when the project progresses and more validations are added.


### OWASP Security Vulnerability Check

An OWASP security vulnerability is configured, but you need an API key.
Fetch it from https://nvd.nist.gov/developers/request-an-api-key.

Then add it to your `~/.gradle/gradle.properties` file:

```
OWASP_API_KEY=........-....-....-....-............
```

Now you can run the dependency vulnerability check:

```shell
gw dependencyCheckUpdate
gw dependencyCheckAnalyze
```

This task is also included in `gw build` and `gw check`.
It is configured in [build.gradle.kts](build.gradle.kts).

Often vulnerability reports don't apply to our use cases.
Therefore, reports can be [suppressed](./etc/owasp-dependency-check-suppression.xml).
In case of suppression, a note must be added to explain why it does not apply to us.

See also: https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html.

### How to Check Dependency-License-Compatibility

The `gw check` phase depends on a dependency-license-compatibility check.
If any dependency violates the configured [list of allowed licenses](etc/allowed-licenses.json), the build will fail.
New licenses can be added to that list after a legal investigation.

<big>**&#9888;**</big>
*GPL* (*GNU General Public License*) is only allowed with classpath exception.
Do <u>not</u> use any dependencies under *GPL* without this exception,
except if these offer an alternative license which is allowed.
*LGPL* (*GNU <u>Library</u> General Public License*) is also allowed.

To run just the dependency-license-compatibility check, use:

```shell
gw checkLicense
```

If the check fails, a report can be found here: The generated license can be found under [dependencies-without-allowed-license.json](/build/reports/dependency-license/dependencies-without-allowed-license.json).

And to generate a report, use:

```shell
gw generateLicenseReport
```

The generated license can be found here: [index.html](build/reports/dependency-license/index.html).

More information can be found on the [project's website](https://github.com/jk1/Gradle-License-Report).

### How to Upgrade Versions of Dependencies

Dependency versions can be automatically upgraded to the latest available version:

```shell
gw useLatestVersions
```

Afterward, `gw check` is automatically started.
Please only commit+push to master if the check run shows no errors.

More infos, e.g. on blacklists see on the [project's website](https://github.com/patrikerdes/gradle-use-latest-versions-plugin).


## Biggest Flaws in our Architecture

### The RBAC System is too Complicated

Now, where we have a better experience with what we really need from the RBAC system, we have learned
that and creates too many (grant- and role-) rows and too even tables which could be avoided completely.

The basic idea is always to always have a fixed set of ordered role-types which apply for all DB-tables under RBAC,
e.g. OWNER>ADMIN>AGENT\[>PROXY?\]>TENENT>REFERRER.
Grants between these for the same DB-row would be implicit by order comparison.
This way we would get rid of all explicit grants within the same DB-row
and would not need the `rbac.role` table anymore.
We would also reduce the depth of the expensive recursive CTE-query.

This has to be explored further.  For now, we just keep it in mind and avoid roles+grants
which would not fit into a simplified system with a fixed role-type-system.


### The Mapper is Error-Prone

Where `org.modelmapper.ModelMapper` reduces bloat-code a lot and has some nice features about recursive data-structure mappings,
it often causes strange errors which are hard to fix.
E.g. the uuid of the target main object is often taken from an uuid of a sub-subject.
(For now, use `StrictMapper` to avoid this, for the case it happens.)


### Too Many Business-Rules Implemented in Controllers

Some REST-Controllers implement too much code for business-roles.
This should be extracted to services.


## How To ...

Besides the following *How Tos* you can also find several *How Tos* in the source code:

```sh
grep -r HOWTO src
```

also try this (assumed you've sourced .aliases):
```sh
howto 
```

### How to Run the Application With Other Profiles, e.g. production:

Add `--args='--spring.profiles.active=...` with the wanted profile selector:

```sh
gw bootRun --args='--spring.profiles.active=fakeCasAuthenticator,external-db,only-prod-schema,without-test-data'
```

These profiles mean:

- **external-db**: an external PostgreSQL database is used with the PostgreSQL users already created as specified in the environment
- **only-prod-schema**: only the Office module is started, but neither the Booking nor the Hosting modules
- **without-test-data**: no test-data is inserted


### How to Run the Application in a Debugger

Add `' --debug-jvm` to the command line:


```sh
gw bootRun ... --debug-jvm
```

At the very beginning, the application is going to wait for a debugger with a message like this:

> Listening for transport dt_socket at address: 5005

As soon as a debugger connects to that port, the application will continue to run.

In IntelliJ IDEA you need a 'Remote JVM Debug' run configuration like this:

![IntelliJ IDEA JVM-Debug Run Config](./doc/.images/intellij-idea-jvm-debug-run-config.png)

Now, to attach IntelliJ IDEA as a debugger, you just need to run that config in debug mode.
If it's selected, just hit the *bug*-symbol next to it.


### How to Do a Clean Run of the Application

If you frequently need to run with a fresh database and a clean build, you can use this:

```sh
export HSADMINNG_CAS_SERVER=
gw clean && pg-sql-reset && sleep 5 && gw bootRun' 2>&1 | tee log
```


### How to Configure .pgpass for the Default PostgreSQL Database?

To access the default database schema as used during development, add this line to your `.pgpass` file in your users home directory:

```
localhost:5432:postgres:postgres:password
```

Amend host and port if necessary.


### How to Run the Tests Against a Local User-Space Podman Daemon?

Using a normal Docker daemon running as root has some security issues.
As an alternative, this chapter shows how you can run a Podman daemon in user-space.

#### Install and Run Podman

You can find directions in [this project on Github](https://stackoverflow.com/questions/71549856/testcontainers-with-podman-in-java-tests) 

Summary for Debian-based Linux systems:

1. Install Podman, e.g. like this:

 ```shell
sudo apt-get -y install podman
```

It is possible to move the storage directory to /tmp, e.g. to increase performance or to avoid issues with NFS mounted home directories:

```shell
cat .config/containers/storage.conf
[storage]
driver = "vfs"
graphRoot = "/tmp/containers/storage"
```

2. Then start it like this:

```shell
systemctl --user enable --now podman.socket
systemctl --user status podman.socket
ls -la /run/user/$UID/podman/podman.sock
```

These commands are also available in `.aliases` as `podman-start`.


#### Use the Command Line to Run the Tests Against the Podman Daemon 

1. In a local shell. in which you want to run the tests, set some environment variables:

```shell
export DOCKER_HOST="unix:///run/user/$UID/podman/podman.sock"
export TESTCONTAINERS_RYUK_DISABLED=true
```

These commands are also available in `.aliases` as `podman-use`. 

Disabling RYUK is necessary, because it's not supported by Podman.
Supposedly this means that containers are not properly cleaned up after test runs,
but I could not see any remaining containers after test runs.
If we are running into problems with stale containers,
we need to register a shutdown-hook in the test source code.

2. Now You Can Run the Tests

```shell
gw test # gw is from the .aliases file
```

#### Use IntelliJ IDEA Run the Tests Against the Podman Daemon

To run the tests against a Podman Daemon in IntelliJ IDEA too, you also need to set the environment variables `DOCKER_HOST` and `TESTCONTAINERS_RYUK_DISABLED` as show above.
This can either be done in the environment from which IDEA is started.
Or you can use the run config template for gradle to set these variables:

![IntelliJ IDEA Gradle Run Template](./doc/.images/intellij-idea-gradle-run-template.png)

If you already have Gradle run configs, you need to delete them, so they get re-created from the template.
Alternatively you need to add the environment varibles here too:

![IntelliJ IDEA Gradle Run Config Example](./doc/.images/intellij-idea-gradle-run-config.png)

Find more information [here](https://www.jetbrains.com/help/idea/run-debug-configuration.html).


#### ~/.testcontainers.properties

It should be possible to set these environment variables in `~/.testcontainers.properties`,
but it did not work so far.
Maybe a problem with quoting.

If you manage to make it work, please amend this documentation, thanks.


### How to Run the Tests Against a Remote Podman or Docker Daemon?

1. On the remote host, you need to have a Podman or Docker daemon running on a port accessible from the Internet. 
Probably, you want to protect it with a VPN, but that's not part of this documentation.

e.g. to make Podman listen to a port, run this:

```shell
podman system service -t 0 tcp:HOST:PORT # please replace HOST+PORT
```

2. In a local shell. in which you want to run the tests, set some environment variables:

```shell
export DOCKER_HOST=tcp://HOST:PORT # please replace HOST+PORT again
export TESTCONTAINERS_RYUK_DISABLED=true  # only for Podman
```

Regarding RYUK, see also in the directions for a locally running Podman, above.

3. Now you can run the tests:

```shell
gw clean test # gw is from the .aliases file
```

For information about how to run the tests in IntelliJ IDEA against a remote Podman daemon, see also in the chapter above just with the HOST:PORT-based DOCKER_HOST.

### How to Run the Application on a Different Port?

By default, `gw bootRun` starts the application on port 8080.

This port can be changed in
[src/main/resources/application.yml](src/main/resources/application.yml) through the property `server.port`.

Or on the command line, add ` --server.port=...` to the `--args` parameter of the `bootRun` task, e.g.:

```sh
gw bootRun --args='--spring.profiles.active=dev,fakeCasAuthenticator,complete,test-data --server.port=8888'
```

### How to Use a Persistent Database for Integration Tests?

Usually, the `DataJpaTest` integration tests run against a database in a temporary docker container.
As soon as the test ends, the database is gone; this might make debugging difficult.

Alternatively, a persistent database could be used by amending the
[resources/application.yml](src/main/resources/application.yml) through the property `spring.datasource.url` in [src/test/resources/application.yml](src/test/resources/application.yml) , e.g. to the JDBC-URL from [src/main/resources/application.yml](src/main/resources/application.yml).

If the persistent database and the temporary database show different results, one of these reasons could be the cause:

1. You might have some changesets only running in either context,
   check the `context: ...` in the changeset control lines.
2. You might have changes in the database which interfere with the tests,
   e.g. from a previous run of tests or manually applied.
   It's best to run `pg-sql-reset && gw bootRun` before each test run, to have a clean database.
  
### How to Amend Liquibase SQL Changesets?

Liquibase changesets are meant to be immutable and based on each other.
That means, once a changeset is written, it never changes, not even a whitespace or comment.
Liquibase is a *database migration tool*, not a *database initialization tool*.

This, if you need to add change a table, stored procedure or whatever, 
create a new changeset and apply `ALTER`, `DROP`, `CREATE OR REPLACE` or whatever SQL commands to perform your changes.
These changes will be automatically applied once the application starts up again.
This way, any staging or production database will always match the application code. 

But, during initial development that can be a big hassle because the database structure changes a lot in that stage.
Also, the actual structure of the database won't be easily recognized anymore through lots of migration changesets.

Therefore, during initial development, it's good approach just to amend the existing changesets and delete the database:

```shell
pg-sql-reset
gw bootRun
```

<big>**&#9888;**</big>
Just don't forget switching to the migration mode, once there is a production database!

### How to Re-Generate Spring-Controller-Interfaces from OpenAPI specs?

The API is described as OpenAPI specifications in `src/main/resources/api-definition/`.

Once generated, the interfaces for the Spring-Controllers can be found in `build/generated/sources/openapi`.

These interfaces have to be implemented by subclasses named `*Controller`.

All gradle tasks which need the generated interfaces depend on the Gradle task  `openApiGenerate` which controls the code generation.
It can also be executed directly:

```shell
gw openApiGenerate
```

### How to Generate Database Table Diagrams?

Some overview documentation about the database can be generated via [postgresql_autodoc](https://github.com/cbbrowne/autodoc").
To make it easier, the command line is included in the `.aliases`, just call:

```shell
postgres-autodoc
```

The output will list the generated files.


### How to Add (Real) Admin Users

```sql
DO $$
DECLARE
    -- replace with your admin account names
    admin_users TEXT[] := ARRAY['admin-1', 'admin-2', 'admin-3'];
    admin TEXT;
BEGIN
    -- run as superuser
    call base.defineContext('adding real admin users', null, null, null);
    
    -- for all new admin accounts
    FOREACH admin IN ARRAY admin_users LOOP
        call rbac.grantRoleToSubjectUnchecked(
                rbac.findRoleId(rbac.global_ADMIN()), -- granted by role
                rbac.findRoleId(rbac.global_ADMIN()), -- role to grant
                rbac.create_subject(admin)); -- creates the new admin account
        END LOOP;
END $$;
```

### How to Use _aider AI_ - Pair Programming in Your Terminal

[aider](https://aider.chat/) is an [open source](https://github.com/Aider-AI/aider) AI agent in the shape of a command-line tool that lets you code with large language models (LLMs) OpenAI GPT, Claude Sonnet or Google Gemini.
It allows you to easily analyze and edit files by chatting with the AI.

*BEWARE*: aider is going to send your source code to the LLM!

_hsadmin-NG_ is open source, so this is not a big problem.
For more information about security regarding aider, please have a look at the end of this chapter and check out [the aider privacy policy](https://aider.chat/docs/legal/privacy.html).

#### Installation

Assuming you have Python 3 and `pipx` installed (a tool to install and run Python applications in isolated environments), you can install `aider-chat` like this:

```shell
pipx install aider-chat
```

If you want to use specific features like OpenAI's vision capabilities, you might need to add the following dependencies:

```shell
pipx inject aider-chat openai --include-apps
```

To add support for Google's Gemini AI, you can add the `google-generativeai` package:

```shell
pipx inject aider-chat google-generativeai --include-apps
```

#### Configuration

`aider` requires an API key for the AI model you want to use.

E.g. for _OpenAI GPT_, set the `OPENAI_API_KEY` environment variable:

```shell
export OPENAI_API_KEY="your-api-key-here"
```

And e.g. for _Google Gemini_, set the `GEMINI_API_KEY` environment variable:

```shell
export GEMINI_API_KEY="your-api-key-here"
```

You might want to add this to your shell's startup file (e.g., `.bashrc`, `.zshrc`).

#### Usage

1.  Navigate to your project's root directory in the terminal.
2.  Start `aider` by simply typing:
    ```shell
    aider # see also .aider.conf.yml
    ```
3.  Add the files you want the AI to work with:
    ```
    /add path/to/your/file.java path/to/another/file.py
    ```
4.  Start chatting! Describe the changes you want, ask questions, or request code generation. `aider` will propose changes and apply them directly to your files after your confirmation.
5.  Use `/quit` to exit `aider`.

Refer to the [official aider documentation](https://aider.chat/docs/usage.html) for more commands and advanced features.

#### Example Session

Aider is not yet very good at figuring out which files to amend in a large code base.
I tried giving hints with `/ask`, but it was always missing too many files.
With some of my approaches, it even wanted to create new files, which is not necessary for this task. 

I even tried with other language models, like gpt-o4 or r1 (deepseek-reasoning), no success.
Maybe somebody else can figure it out, or it gets better with time?

For now, I just determined the files myself.
As I knew that the new filed needs to be supported everywhere, 
where the existing field `registrationOffice` occurs, I could simply use grep:  

```shell
    aider `grep -rl registrationOffice src/main/java/ src/test/java/ src/main/resources/api-definition src/main/resources/db/`
```

Then I requested my change to the _aider AI_ chat:

> I want to add a text field `notes` to the database table `hs_office.partner_details` and related files.
  Files to amend have already been added to aider AI.
  Please apply all required changes for Java production+test-code,
  add the Liquibase changeset and amend the OpenAPI-Spec. 

I ran the tests and found that patching the partner details did not work.
So, I told the _aider AI_ about it:

> Please doublecheck if you followed all conventions.
  Any other amendments necessary to support the new field `notes` in the partner details?   

Then I saw, that _aider AI_ did add some notes to the test data, but not to the assertions.
I decided that the changes in the test-data are not necessary and reverted thos files using git.

Now, all tests passed.

Try it yourself, but keep in mind that LLMs use a concept called _temperature_ which specifies a level of randomness.
This means, you might get different results.

#### Security

To reassure myself which files _aider AI_ accesses, I checked this with `strace`:

```
# run aider under strace: 
strace -f -t -e trace=file -o build/aider.strace aider ...

# and in another terminal check the strace log:
tail -f build/aider.strace | grep -oP '"\K[^\n"]+(?=")' 
```

At the time I've checked it, all accessed files made sense.
Of course, as with any locally installed application, there is no guarantee.

There is a _Docker_ image for _aider AI_, but it's pretty restriced
and to be able to use some features, you'd need to rebuild the image.


## Further Documentation

- the `doc` directory contains architecture concepts and a glossary
- the `ideas` directory contains unstructured ideas for future development or documentation
