
## Related Links (Hostsharing-internal)

This section is optional.

A bullet-list with links to the GitEA-PR, the Taiga ticket and other related resources.

## The Problem

This section is required.

A prosa description of the problem, which this PR is supposed to solve.
Keep it short!

## Non-Goals

This section is required.

What this PR deliberately does not do, to delimit the scope.
Usually a terse bullet-list.

If this PR legitimately has to exceed the scope budget of `tools/pr-quick-check`,
declare an approved ceiling for the scope-budget exception here (see
`pr-quick-check --help`). The ceiling may leave room for further related changes,
for example:

- `pr-quick-check:max-scope-score=<n>`: reason why splitting this PR would not be sensible

## The Scenarios

This section is optional for maintenance or bug-fixing PRs, but required for features.

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
  using the use-case [ExampleUseCase](../../src/test/java/net/hostsharing/hsadminng/example/ExampleUseCase.java)

Link both the scenario-test class file and the related use-case class file.
Methods cannot be linked in Markdown, but class files can — at least in an IDE.

#### Scenario#236.02: Description of another requirement in the shape of a scenario!

...

See [Examples](2026-06-25-PR%23236-realm-prefix-based-user-and-group-subject-visibility.md).

Such feature descriptions are also very helpful in deriving tests and can lead agentic coding AI very well.

## The Solution

This section is required.

Here you describe the changes you made and why you made them.

If necessary, you can link to an ADR (Architecture Decision Record).

## Open Questions

This section is optional.

A bullet-list, listing decisions which are deliberately left open for the reviewer or a follow-up.

## Additional Changes

This section is optional.

Here you list any additional changes you made, e.g. "fixed formatting in ..." or "fixed some naming issues".
Keep it short!

## Prerequisite PRs

This section is optional.

Here you list PRs this PR builds upon.

If this PR targets the branch of one of them instead of the default base,
name that branch here, so that `tools/pr-quick-check` measures the right diff:

- `pr-quick-check:base=<ref>`

## Follow-up PRs

This section is optional.

Here you list work which is intentionally deferred to later PRs.

## Attachments

This section is optional.

Here you can add any longer sections that would interrupt the reading flow in the previous sections.
Put each attachment on a level-3 heading ('### ...').

---

## Release Note

released: <YYYY-MM-DD>

Only for changes which users of the REST API can observe, otherwise leave this appendix out.
The build collects these notes into the public news page at `/doc/`,
thus write English even in a German PR document.
`released:` names the merge day; `tools/pr-quick-check` insists that it is the current date.
Write a dated statement about the change, not a description of the current state, for example:

### Booking projects no longer require a debitor UUID

Since 2026-05-15, booking projects can be fetched without a debitor UUID.
