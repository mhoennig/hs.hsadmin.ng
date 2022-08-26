# hsadminNg Development

This documents gives an overview of the development environment and tools.
For architecture consider the files in the `doc` and `adr` folder.

<!-- generated TOC begin: -->
- [Setting up the Development Environment](#setting-up-the-development-environment)
  - [SDKMAN](#sdkman)
  - [PostgreSQL Server](#postgresql-server)
  - [Markdown](#markdown)
    - [Render Markdown embedded PlantUML](#render-markdown-embedded-plantuml)
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
  - [Spotless Code Formatting](#spotless-code-formatting)
  - [JaCoCo Test Code Coverage Check](#jacoco-test-code-coverage-check)
  - [PiTest Mutation Testing](#pitest-mutation-testing)
  - [OWASP Security Vulnerability Check](#owasp-security-vulnerability-check)
  - [Dependency-License-Compatibility](#dependency-license-compatibility)
  - [Dependency Version Upgrade](#dependency-version-upgrade)
- [How To ...](#how-to-...)
  - [How to Run the Application on a Different Port ](#how-to-run-the-application-on-a-different-port-)
  - [How to Use a Persistent Database for Integration Tests?](#how-to-use-a-persistent-database-for-integration-tests?)
- [How to Amend Liquibase SQL Changesets?](#how-to-amend-liquibase-sql-changesets?)
- [Further Documentation](#further-documentation)
<!-- generated TOC end. -->

## Setting up the Development Environment

All instructions assume that you're using a current _Linux_ or _MacOS_ operating system.
Everything is tested on _Ubuntu Linux 22.04_ and _MacOS Monterey (12.4)_.

To be able to build and run the Java Spring Boot application, you need the following tools:

- Docker 20.x (on MacOS you also need *Docker Desktop* or similar)
- PostgreSQL Server 13.7-bullseye 
  (see instructions below to install and run in Docker)
- Java JDK at least recent enough to run Gradle
  (JDK 17.x will be automatically installed by Gradle toolchain support)
- Gradle in some not too outdated version (7.4 will be installed via wrapper)

You also might need an IDE (e.g. *IntelliJ IDEA* or *Eclipse* or *VS Code* with *[STS](https://spring.io/tools)* and a GUI Frontend for *PostgreSQL* like *Postbird*.

If you have at least Docker, the Java JDK and Gradle installed in appropriate versions and in your `PATH`, then you can start like this:

    cd your-hsadmin-ng-directory
    
    gradle wrapper  # downloads the configured Gradle version into the project
    source .aliases # creates some comforable bash aliases, e.g. 'gw'='./gradlew'

    gw test         # compiles and runs unit- and integration-tests
    
    pg-sql-run      # downloads + runs PostgreSQL in a Docker container on localhost:5432
    gw bootRun      # compiles and runs the application on localhost:8080

    # the following command should reply with "pong":
    curl http://localhost:8080/api/ping

    # the following command should return a JSON array with just all customers:
    curl \
        -H 'current-user: mike@hostsharing.net' \
        http://localhost:8080/api/customers

    # the following command should return a JSON array with just all packages visible for the admin of the customer yyy:
    curl \
        -H 'current-user: mike@hostsharing.net' -H 'assumed-roles: customer#yyy.admin' \
        http://localhost:8080/api/packages

    # add a new customer
    curl \
        -H 'current-user: mike@hostsharing.net' -H "Content-Type: application/json" \
        -d '{ "prefix":"ttt", "reference":80001, "adminUserName":"admin@ttt.example.com" }' \
        -X POST http://localhost:8080/api/customers

If you wonder who 'mike@hostsharing.net' and 'sven@hostsharing.net' are and where the data comes from:
Mike and Sven are just example Hostsharing hostmaster accounts as part of the example data which is automatically inserted in Testcontainers and Development environments.
Also try for example 'admin@xxx.example.com' or 'unknown@example.org'.

If you want a formatted JSON output, you can pipe the result to `jq` or similar.

And to see the full, currently implemented, API, open http://localhost:8080/swagger-ui/index.html.

If you still need to install some of these tools, find some hints in the next chapters. 


### SDKMAN

*SdkMan* is not necessary, but helpful to install and switch between different versions of SDKs (Software-Development-Kits) and development tools in general, e.g. *JDK* and *Gradle*.
It is available for _Linux_ and _MacOS_, _WSL_, _Cygwin_, _Solaris_ and _FreeBSD_.

You can get it from: https://sdkman.io/.

<big>**&#9888;**</big>
Yeah, the `curl ... | bash` install method looks quite scary;
but in a development environment you're downloading executables all the time,
e.g. through `npm`, `Maven` or `Gradle` when downloading dependencies.
Thus, maybe you should at least use a separate Linux account for development.

Once it's installed, you can install *JDK* and *Gradle*:

    sdk install java 17.0.3-tem
    sdk install gradle

    sdk use java 17.0.3-tem # use this to switch between installed JDK versions


### PostgreSQL Server

You could use any PostgreSQL Server (from version 13 on) installed on your machine.
You might amend the port and user settings in `src/main/resources/application.yml`, though.

But the easiest way to run PostgreSQL is via Docker.

Initially, pull an image compatible to current PostgreSQL version of Hostsharing:

    docker pull postgres:13.7-bullseye 

<big>**&#9888;**</big>
If we switch the version, please also amend the documentation as well as the aliases file. Thanks! 

Create and run a container with the given PostgreSQL version:

    docker run --name hsadmin-ng-postgres -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres:13.7-bullseye

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

```sh
sudo apt-get install pandoc texlive-latex-base texlive-fonts-recommended texlive-extra-utils texlive-latex-extra pandoc-plantuml-filter
```

```sh
pandoc --filter pandoc-plantuml rbac.md -o rbac.pdf
```

##### for other IDEs / operating systems

If you have figured out how it works, please add instructions above this section.

### IDE Specific Settings

#### IntelliJ IDEA

Go to [Gradle Settings}(jetbrains://idea/settings?name=Build%2C+Execution%2C+Deployment--Build+Tools--Gradle) and select "Build and run using" and "Run tests using" both to "gradle".
Otherwise, settings from `build.gradle`, like compiler arguments, are not applied when compiling through *IntelliJ IDEA*.

Go to [Annotations Processors](jetbrains://idea/settings?name=Build%2C+Execution%2C+Deployment--Compiler--Annotation+Processors) and activate annotation processing.
Otherwise, *IntelliJ IDEA* can't see *Lombok* generated classes 
and will show false errors (missing identifiers).

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

`.aliases`
    Shell-aliases for common tasks.

`build/`
    Output directory for gradle build results. Ignored by git.

`build.gradle`
    Gradle build-file. Contains dependencies and build configurations.

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

`TODO.md`
    Requirements of initial project. Do not touch!

`TODO-progress.png`
    Generated diagram image of the project progress.

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


### Spotless Code Formatting

Code formatting for Java is checked via *spotless*.
The formatting style can be checked with this command:

```shell
gw spotlessCheck
```

This task is also included in `gw build` and `gw check`.

To apply formatting rules, use:

```shell
gw spotlessApply
```

### JaCoCo Test Code Coverage Check

This project uses the JaCoCo test code coverage report with limit checks.
It can be executed with:

```shell
gw jacocoTestReport
```

This task is also automatically run after `gw test`.
It is configured in [build.gradle](build.gradle).

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

Classes to be scanned, tests to be executed and thresholds are configured in [build.gradle](build.gradle).

A report is generated under [build/reports/pitest/index.html](./build/reports/pitest/index.html).
A link to the report is also printed after the `pitest` run.

This task is also executed as part of `gw check`.

#### Remark

In this project, there is little business logic in *Java* code;
most business code is in *plsql* 
and *Java* ist mostly used for mapping REST calls to database queries.
This mapping ist mostly done through *Spring* annotations and other implicit code.

Therefore, there are only few unit tests and thus mutation testing has limited value.
We'll see if this changes when the project progresses and more validations are added.


### OWASP Security Vulnerability Check

An OWASP security vulnerability is configured and can be utilized by running:

```shell
gw dependencyCheckAnalyze
```

This task is also included in `gw build` and `gw check`.
It is configured in [build.gradle](build.gradle).

Often vulnerability reports don't apply to our use cases.
Therefore, reports can be [suppressed](./etc/owasp-dependency-check-suppression.xml).
In case of suppression, a note must be added to explain why it does not apply to us.

See also: https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html.

### Dependency-License-Compatibility

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

### Dependency Version Upgrade

Dependency versions can be automatically upgraded to the latest available version:

```shell
gw useLatestVersions
```

Afterwards, `gw check` is automatically started.
Please only commit+push to master if the check run shows no errors.

More infos, e.g. on blacklists see on the [projet's website](https://github.com/patrikerdes/gradle-use-latest-versions-plugin).


## How To ...

### How to Run the Application on a Different Port 

By default, `gw bootRun` starts the application on port 8080.

This port can be changed in
[src/main/resources/application.yml](src/main/resources/application.yml) through the property `server.port`.

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
   
## How to Amend Liquibase SQL Changesets?

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

## Further Documentation

- the `doc` directory contains architecture concepts and a glossary
- TODO.md tracks requirements and progress for the contract of the initial project,
  please do not amend anything in this document
