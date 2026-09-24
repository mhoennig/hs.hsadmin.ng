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
- Write PR-documentation either in English or in German, but do not mix languages within one document;
  except for identifiers, file names, technical terms and the like, which stay in their original language.
- Section headings (`##`, `###`, ...) follow the language of the document;
  the German headings are defined in the [German template](TEMPLATE-de.md).
  Gherkin structure keywords stay in English in either language:
  `Feature:`, `Background`, `Scenario#...`, `Given`/`When`/`Then`, `Verified by`.
- Write scenarios in Markdown-native pseudo-Gherkin under a `#### Scenario#…` heading, not in fenced Gherkin blocks.
  Fenced blocks either render no recognizable headings, so that all scenarios run into each other,
  or they force the scenario title twice, once as heading and once as `Scenario:` line.
  Pseudo-Gherkin also allows direct links to tests, issues, ADRs, or explanations inside the requirement text.
- Use clear and concise language and keep it short.
- Include relevant details and context, explain the "why".
- Mark reference to Taiga or any other tool that is not public as "Hostsharing-internal".
- If necessary, copy important parts of the ticket description.
- One sentence or statement per line to make diffs easier to read.
- A PR document is read as CR before the merge and as record of the change after it.
  Thus the problem section describes the state before the PR as "the former version of …" in the present tense,
  which is true at both times, and tells concrete incidents in the past tense.

## Structure

The main (`##`) sections, their headings and which of them are required or optional
are defined in the [English template](TEMPLATE.md) and the [German template](TEMPLATE-de.md).

IMPORTANT: The sections have to appear in exactly the order given in the template;
omit optional sections which do not apply, but never reorder them.

Below these sections, separated by a `---` line, an optional appendix holds the release note.

## Release Notes

Below the sections above, separated by a `---` line, a PR may append a release note.
The build collects these notes into the news page of the documentation which the application
serves at `/doc/`.
That documentation addresses whoever uses the REST API, thus a note only belongs into a PR which
changes something observable through the API.

Write the appendix in English, even in a German PR document, because the news page is public.
Its `###` heading becomes the headline of the news entry, so that the entry does not have to
borrow the PR title, which is written for a different audience.

A `released: <YYYY-MM-DD>` line right below the heading names the day the PR is merged, which the
news page sorts and shows by.
The file name is no reliable release date, because a PR document is started long before its merge —
PR#212 was started in December and merged in March.
`tools/pr-quick-check` therefore insists that this date is the current one.

Write each note as a dated statement about a change, not as a description of the current state.
The news page keeps a note after later changes, thus a note about the current state would silently
become wrong, whereas a dated note about a change stays true.

## Declarations for `tools/pr-quick-check`

A PR document declares exceptions and its base with a tag of the form `pr-quick-check:<key>=<value>`.
Only a tag outside of backticks declares something;
in backticks it just documents the declaration, like a quoted marker.
Thus a PR document which changes these rules can quote any tag without declaring it.

## Changing Further PR Documents

A PR normally changes exactly one PR document, and `tools/pr-quick-check` enforces that.
When a PR legitimately has to change more, for example to back-fill release notes into merged
PRs, its own PR document declares how many further ones it changes:

    pr-quick-check:extra-pr-docs=<count>: <reason>

## A PR Built on Another PR

A PR can target the branch of another, not yet merged PR.
Only the pull-request platform (e.g. GitEA) knows this target,
thus `tools/pr-quick-check` measures against the default base and reports the changes of both PRs together.
The PR document therefore names its target branch, preferably in the Prerequisite-PRs section:

    pr-quick-check:base=<ref>

The declaration counts until the named branch is merged or deleted.
From then on the default base gives the same result, and the line can stay as history.

## Ideas And Free-Floating TODOs

Ideas and TODOs which hang on no specific code location and belong to no open PR yet
are collected in [IDEAS.md](IDEAS.md).
Several related entries can grow into a PR of their own,
and a single entry may join a thematically fitting PR as an additional change.
`tools/pr-quick-check` does not treat that file as a PR document.
