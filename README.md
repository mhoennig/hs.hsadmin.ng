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
(without any generated entities) before changes to the JDL file can be imported.

| WARNING: This is just a guideline. You should understand what you are doing! |
| ---------------------------------------------------------------------------- |


    git checkout jhipster-generated
    git pull
    git tag REAL-HEAD
    git reset --hard jdl-base
    git clean -f -d
    git cherry-pick -n spotless
    git reset --soft REAL-HEAD
    git checkout REAL-HEAD src/main/jdl/customer.jdl # AND OTHERS!
    git tag -d REAL-HEAD

    # MANUAL STEP: Apply changes to the jdl file!

    # (Re-) Importing
    jhipster import-jdl src/main/jdl/customer.jdl
    jhipster import-jdl src/main/jdl/accessrights.jdl
    # AND OTHERS, if applicable!

    gw spotlessApply
    git add .
    git commit -m"..."

    # MANUAL STEP:
    # - if you've renamed any identifiers, use refactoring to rename in master as well BEFORE MERGING!

    # Merge changeset into master branch
    git checkout master
    git merge jhipster-generated

### Amending the spotless commit

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

## Special Build Tasks

Besides common build tasks like `build`, `test` or `bootRun` this projects has some not so common tasks which are explained in this section.

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
