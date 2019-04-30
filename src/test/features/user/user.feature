Feature: User management

    Scenario: Retrieve administrator user
        When I search user 'admin'
        Then the user is not found
        And his last name is 'Administrator'
