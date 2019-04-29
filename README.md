# hsadminNg Development

## Setting up the Development Environment

You'll often need to execute `./gradlew`, therefore we suggest to define this alias:

    alias gw='./gradlew'

## Frequent Tasks

### Building the Application with Test Execution

gw build

### Starting the Application

Either simply:

    gw bootRun

or with a specific port:

    SERVER_PORT=8081 ./gradlew bootRun

### Running JUnit tests with branch coverage

#### for IntelliJ IDEA

see: https://confluence.jetbrains.com/display/IDEADEV/IDEA+Coverage+Runner

Either apply it to specific test configurations or,
better, delete the previous test configurations and amend the JUnit template.

## Git Workflow for JHipster Generator

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

## HOWTO do This and That

There are git tags on some commits which show how to add certian features.

Find all of such tags with:

    git tag | grep HOWTO

### creating HOWTO commits

If you want to add such a commit, make sure that it contains no clutter
(no changes which are not necessary for whatever the commit is about to explain),
and is complete with all unit tests, code coverage, pitest and other checks.
Otherwise the next developer would run into the same problems again.

One way to keep the commit clean, is to develop it on a local branch.
If any other changes (e.g. bugfixes, API extensions etc.) are necessary,
apply these only to the master or cherry-pick just these to the master,
then rebase your local branch. Do not forget to run all checks locally:

    gw clean check pitest # might need over an hour

(Check the pitest section for speeding up pitest.)

To create and push a new tag use:

    git tag HOWTO-... master
    git push origin HOWTO-...

After you've moved an existing the tag to another commit, you can use:

    git push origin HOWTO-... --force
