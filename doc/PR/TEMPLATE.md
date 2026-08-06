
## The Problem

A prosa description of the problem, which this PR is supposed to solve.
Keep it short!

## Non-Goals

What this PR deliberately does not do, to delimit the scope.
Usually a terse bullet-list.

If this PR legitimately has to exceed the scope budget of `tools/pr-quick-check`,
declare an approved ceiling for the scope-budget exception here (see
`pr-quick-check --help`). The ceiling may leave room for further related changes,
for example:

- `pr-quick-check:max-scope-score=<n>`: reason why splitting this PR would not be sensible

## The Scenarios

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

Here you describe the changes you made and why you made them.

If necessary, you can link to an ADR (Architecture Decision Record).

## Open Questions

A bullet-list, listing decisions which are deliberately left open for the reviewer or a follow-up.

## Additional Changes

Here you list any additional changes you made, e.g. "fixed formatting in ..." or "fixed some naming issues".
Keep it short!

## Prerequisite PRs

Here you list PRs this PR builds upon.

## Follow-up PRs

Here you list work which is intentionally deferred to later PRs.

## Attachments

Here you can add any longer sections that would interrupt the reading flow in the previous sections.
Put each attachment on a level-3 heading ('### ...').
ö

---

## Release Note

released: <YYYY-MM-DD>

Only for changes which whoever uses the REST API can observe, otherwise leave this appendix out.
The build collects these notes into the news page of the documentation at `/doc/`.

Write this appendix in English, even in a German PR document, because the news page is public.
Keep the `---` on its own line with an empty line before it, otherwise Markdown turns the line
above it into a heading.

`released: <YYYY-MM-DD>` names the day this PR is merged, because a PR document is started long
before that and its file name is therefore no reliable release date.
`tools/pr-quick-check` insists that this date is the current one, so that it is right at merge time.

### A headline for the news page

Write the note as a dated statement about a change, not as a description of the current state:
"Since 2026-05-15, booking projects can be fetched without a debitor UUID."
The news page keeps the note after later changes, thus a note about the current state would
silently become wrong, whereas a dated note about a change stays true.

Keep it to one or two sentences and leave out implementation detail.
