# hsadminNg Development

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

-   [Setting up the Development Environment](#setting-up-the-development-environment)
-   [Frequent Tasks](#frequent-tasks)
    -   [Building the Application with Test Execution](#building-the-application-with-test-execution)
    -   [Starting the Application](#starting-the-application)
    -   [Running JUnit tests with branch coverage](#running-junit-tests-with-branch-coverage)
-   [HOWTO Commits](#howto-commits)
    -   [Creating HOWTO Commits](#creating-howto-commits)
-   [Special Build Tasks](#special-build-tasks)
    -   [Spotless Formatting](#spotless-formatting)
    -   [Mutation Testing PiTest](#mutation-testing-pitest)
    -   [Git Workflow for JHipster Generator](#git-workflow-for-jhipster-generator)
    -   [Generating the Table of Contents for Markdown](#generating-the-table-of-contents-for-markdown)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Setting up the Development Environment

You'll often need to execute `./gradlew`, therefore we suggest to define this alias:

    alias gw='./gradlew'

TODO: Instructions for setting up the dev environment from scratch.

## Frequent Tasks

### Building the Application with Test Execution

    gw build

### Starting the Application

To use an **H2 in-memory database** populated with sample-data.

    gw bootRun

To use an **H2 file-based database**, start the application with the h2file profile:

    gw bootRun -Ph2file
    gw bootRun -Ph2file,sample-data     # populated with sample data

To use a **local Postgres database**, first prepare your environment:

    export HSADMINNG_DB_URL='jdbc:postgresql://localhost:5432/DBNAME'
    export HSADMINNG_DB_USER='DBUSER'
    export HSADMINNG_DB_PASS='DBPASS'

Where `DBNAME`, `DBUSER` and `DBPASS` are replaced by your credentials.

Then start the application with the pgsql profile:

    gw bootRun -Ppgsql
    gw bootRun -Ppgsql,sample-data     # populated with sample data

To use a **remote Postgres database** on a hostsharing server,

    autossh -M 0 -o "ServerAliveInterval 60" -o "ServerAliveCountMax 3" \
        -f -N -L 55432:127.0.0.1:5432 "xyz00@xyz.hostsharing.net"

Then prepare your environment, e.g. like this:

    export HSADMINNG_DB_URL='jdbc:postgresql://localhost:55432/xyz00_hsadminng'
    export HSADMINNG_DB_USER='xyz00_hsadminng'
    export HSADMINNG_DB_PASS='whatever'

In all cases, you can also **specify the port** to used for the application via environment:

    SERVER_PORT=8081 gw bootRun ...

### Running JUnit tests with branch coverage

#### for IntelliJ IDEA

see: https://confluence.jetbrains.com/display/IDEADEV/IDEA+Coverage+Runner

Either apply it to specific test configurations or,
better, delete the previous test configurations and amend the JUnit template.

## HOWTO Commits

There are git tags on some commits which show how to add certain features.

Find all of such tags with:

    git tag | grep HOWTO

### Creating HOWTO Commits

If you want to add such a commit, make sure that it contains no clutter
(no changes which are not necessary for whatever the commit is about to explain),
and is complete with all unit tests, code coverage, pitest and other checks.
Otherwise the next developer would run into the same problems again.

One way to keep the commit clean, is to develop it on a local branch.
If any other changes (e.g. bugfixes, API extensions etc.) are necessary,
apply these only to the master or cherry-pick just these to the master,
then rebase your local branch. Do not forget to run all checks locally:

    gw clean check pitest # might need over an hour

(Check the PiTest section for speeding up mutation testing.)

To create and push a new tag use:

    git tag HOWTO-... master
    git push origin HOWTO-...

After you've moved an existing the tag to another commit, you can use:

    git push --force origin HOWTO-...

## Special Build Tasks

Besides common build tasks like `build`, `test` or `bootRun` this projects has some not so common tasks which are explained in this section.

### Spotless Formatting

To make sure that no IDE auto-formatter destroys the git history of any file and
especially to avoid merge conflicts from JHipster generated files after these had been changed,
we are using a standard formatter enforced by _spotless_, which is based on the standard Eclipse formatter.

The rules can be checked and applied with these commands:

    gw spotlessCheck
    gw spotlessApply

The spotlessCheck task is included as an early step in our Jenkins build pipeline.
Therefore wrong formatting is automatically detected.

Our configuration can be found under the directory `cfg/spotless`.
Currently we only have specific rules for _\*.java_-files and their import-order.

#### Our Changes to the Standard Eclipse Formatter

We amended the Standard Eclipse Formatter in these respects:

-   Lines of code are never joined, thus the developer has control about linebreaks,
    which is important for readability in some implementations like toString().
-   Lines in comments are never joined either, because that often destroys readable stucture.
-   Parts of files can be excluded from getting formatted, by using `@formatter:off` and `@formatter:on` in a comment.
    See for example in class `SecurityConfiguration`.

#### Pre-Commit Hook

If you like, you could add this code to the _pre-commit or \_pre_push_ hook\_ in your `.git/hooks` directory:

    if  ! ./gradlew spotlessCheck; then
        exit 1
    fi

#### The Tagged Spotless Commit

The commit which introduces the spotless configuration is tagged.
Through this tag it can easily be cherry-picked in the JHipster workflow.

If you need to amend the commit tagged 'spotless', e.g. to change the spotless configuration,
it can be done with these steps:

    git tag REAL-HEAD
    git reset --hard spotless^
    git cherry-pick -n spotless
    ...
    git add .
    # do NOT run: gw spotlessApply yet!
    # for the case you have a commit hook which runs spotlessCheck:
    git commit --no-verify
    git tag --force spotless
    git push --no-verify origin spotless
    git reset --hard REAL-HEAD
    git tag -d REAL-HEAD

### Mutation Testing PiTest

    ./gradlew pitest

Runs (almost) all JUnit tests under mutation testing.
Mutation testing is a means to determine the quality of the tests.

On Jenkins, the results can be found in the build artifacts under:

-   https://ci.hostsharing.net/job/hsadmin-ng-pitest/XX/artifact/build/reports/pitest/index.html

Where XX is the build number. Or for the latest build under:

-   https://ci.hostsharing.net/job/hsadmin-ng-pitest/lastCompletedBuild/artifact/build/reports/pitest/index.html

#### Some Background Information on Mutation Testing

PiTest does it with these steps:

-   initially PiTest checks which production code is executed by which tests
-   if the tests don't pass, it stops
-   otherwise the production code is 'mutated' and PiTest checks whether this makes a test fail ('mutant killed')
-   Finally it checks thresholds for coverage and mutant killing.

More information about can be found here:

-   PiTest: http://pitest.org/
-   gradle-plugin: https://gradle-pitest-plugin.solidsoft.info/

#### How to Configure PiTest

These thresholds can be configured in `build.gradle`,
but we should generally not lower these.

There is also a list of excluded files, all generated by JHipster or MapStruct, not containing any changes by us.

As you might figure, mutation testing is CPU-hungry.
To limit load in our Jenkins build server, it only uses 2 CPU threads, thus it needs over an hour.

If you want to spend more CPU threads on your local system, you can change that via command line:

    gw pitest -Doverride.pitest.threads=7

I suggest to leave one CPU thread for other tasks or your might lag extremely.

### Git Workflow for JHipster Generator

The following workflow steps make sure that

-   JHipster re-imports work properly,
-   the git history of changes to the JDL-files, the generated code and the master is comprehensible,
-   and merging newly generated code to the master branch is smooth.

It uses a git branch `jhipster-generated` to track the history of the JDL model file and the generated source code.
Applying commits which contain non-generated changes to that branch breaks the normal git history for generated files.
Therefore, this documentation is also not available in that branch.
Thus:

**MANUAL STEP before starting:** Copy this workflow documentation, because this file will be gone once you switched the branch.

| WARNING: The following steps are just a guideline. You should understand what you are doing! |
| -------------------------------------------------------------------------------------------- |


#### 1. Preparing the `jhipster-generated` git Branch

This step assumes that the latest `*.jdl` files are on the `HEAD` of the `jhipster-generated` git branch.
On a re-import of a JDL-file, JHipster does not remove any generated classes which belong to entities deleted from the JDL-file.
Therefore, the project has to be reset to a clean state before changes to the JDL file can be re-imported.
We have not yet finally tested a simplified workflow for just adding new entities or properties.

A git tag `jdl-base` is assumed to sit on the base commit after the application was generated, but before any entities were imported.

    git checkout jhipster-generated
    git pull
    git tag REAL-HEAD
    git reset --hard jdl-base
    git clean -f -d
    git cherry-pick -n spotless
    git reset --soft REAL-HEAD
    git checkout REAL-HEAD src/main/jdl/customer.jdl # AND OTHERS!
    git tag -d REAL-HEAD

#### 2. Amending and Re-Importing the JDL

**MANUAL STEP:** First apply all necessary changes to the JDL files.
Then re-import like this:

    # (Re-) Importing
    jhipster import-jdl src/main/jdl/customer.jdl
    jhipster import-jdl src/main/jdl/accessrights.jdl
    jhipster import-jdl src/main/jdl/... # once there are more

For smoothly being able to merge, we need the same formatting in the generated code as on the master:

    gw spotlessApply

#### 3. Committing our Changes

    git add .
    git commit -m"..."

#### 4. Merging our Changes to the `master` Branch

    git checkout master
    git pull

**MANUAL STEP:** If you've renamed any identifiers, use the refactoring feature of your IDE to rename in master as well.
To avoid oodles of merge-conflicts, you need to do that **BEFORE MERGING!**
Commit any of such changes, if any.

Now we can finally merge our changes to master.

    git merge jhipster-generated

It's a good idea doing this step in an IDE because it makes conflict resolving much easier.
Typical merge conflicts stem from:

-   Random numbers in test data of `*IntTest.java` files.
-   Timestamps in Liquibase-xml-Files.

Now, I suggest to run all tests locally:

    gw clean test

Once everything works again, we can push our new version:

    git push

### Generating the Table of Contents for Markdown

This README file contains a table of contents generated by _doctoc_.
It's quite simple to use:

npm install -g doctoc
doctoc --maxlevel 3 README.md

Further information can be found [https://github.com/thlorenz/doctoc/blob/master/README.md](on the _doctoc_ github page).
