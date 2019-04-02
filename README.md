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
