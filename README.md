= hsadminNg Development

== Setting up the Development Environment

You'll often need to execute `./gradlew`, therefore we suggest to define this alias:

    alias gw='./gradlew'

== Building the Application with Test Execution

gw build

== Starting the Application

Either simply:

    gw bootRun

or with a specific port:

    SERVER_PORT=8081 ./gradlew bootRun

== Running JUnit tests with branch coverage

=== for IntelliJ IDEA

see: https://confluence.jetbrains.com/display/IDEADEV/IDEA+Coverage+Runner

Either apply it to specific test configurations or,
better, delete the previous test configurations and amend the JUnit template.

== Git Workflow

The jhipster-generated git branch tracks the history of the JDL model file
and the generated source code. The project has to be resetted to a clean state
(without any generated entitites) before changes to the JDL file can be imported.

    # Prepare/Cleanup Workspace

    git checkout jhipster-generated
    git reset --hard jdl-base
    git clean -f -d
    git checkout HEAD@{1} src/main/jdl/customer.jdl
    git reset HEAD .

    # Apply changes to the jdl file

    # Invoke JHipster generator

    jhipster import-jdl src/main/jdl/customer.jdl --force

    # Let Git determine change set between most recent commit and the re-generated source

    git reset --soft HEAD@{1}
    git reset HEAD .
    git add .

    # Commit changeset

    git commit -m '...'

    # Merge changeset into master branch

    git checkout master
    git merge jhipster-generated
