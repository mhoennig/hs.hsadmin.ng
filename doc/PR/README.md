# Pull-Request Documentations in doc/PR

This directory contains documentation for each pull request (PR).

IMPORTANT: The PR-documentation documents the change in that PR, it might be outdated right after the next PR was merged. Historic PR-documentation is not maintained.

## Naming Convention

1. Date of the PR in the format `YYYY-MM-DD`
2. followed '-PR#' followed by the number of the pull request in GitEA,
3. a short description of the PR with dashes between the words,
4. '.md'

Yes, to get the PR-number, you need to open a pull request first, 
but initially prefix its title with `WIP: ` to mark it as a work in progress until it is ready for review.

## Guidelines

- Documentations must be written in Markdown format.
- Use Englisch, it's a public open source project.
- Use clear and concise language and keep it short.
- Include relevant details and context, explain the "why".
- Mark reference to Taiga or any other tool that is not public as "Hostsharing-internal".
- If necessary, copy important parts of the ticket description.
- One sentence or statement per line to make diffs easier to read.

## Structure

```Markdown

## The Problem

A prosa description of the problem, which this PR is supposed to solve.

## The Requirements

A schematized specification of the requirements, preferably using [Gherkin](https://cucumber.io/docs/gherkin/reference/) vocabulary.
But Gherkin does not make sense for all kinds of PRs.

Use Markdown-native pseudo-Gherkin instead of fenced Gherkin code blocks.
This keeps the scenarios linkable and allows direct links to tests, issues, ADRs, or explanations inside the requirement text.

### Feature: headline of the feature

#### Background

- definitions of terms
- other background information

#### Scenario#236.01: Description of a requirement in the shape of a scenario!

So that ... (describe the goal behind the requirement here).

- **Given** some precondition
  - **and** another precondition
- **When** whatever is done
- **Then** postcondition
  - **and** another postcondition

##### Verified by

- [ExampleScenarioTests.exampleScenario](../../src/test/java/net/hostsharing/hsadminng/example/ExampleScenarioTests.java)

#### Scenario#236.02: Description of another requirement in the shape of a scenario!

...

See [Examples](2026-06-25-PR%23236-realm-prefix-based-user-and-group-subject-visibility.md).

Such feature descriptions are also very helpful in deriving tests and can lead agentic coding AI very well. 

## The Solution

Here you describe the changes you made and why you made them.

If necessary, you can link to an ADR (Architecture Decision Record).

## Additional Changes

Here you list any additional changes you made, e.g. "fixed formatting in ..." or "fixed some naming issues".

## Attachments

Here you can add any longer sections that would interrupt the reading flow in the previous sections.
Put each attachment on a level-3 heading ('### ...').
