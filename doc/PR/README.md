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
- Write PR-documentation in German, the working language of the project;
  external readers are rare and can use machine translation.
- Use clear and concise language and keep it short.
- Include relevant details and context, explain the "why".
- Mark reference to Taiga or any other tool that is not public as "Hostsharing-internal".
- If necessary, copy important parts of the ticket description.
- One sentence or statement per line to make diffs easier to read.

## Structure

The main (`##`) sections have to appear in exactly this order, omitting sections which do not apply:

0. Related Links (optional)
1. The Problem (required)
2. Non-Goals (required)
3. The Scenarios (optional for maintenance or bug fixing PRs, required for features)
4. The Solution (required)
5. Open Questions (optional)
6. Additional Changes (optional)
7. Prerequisite PRs (optional)
8. Follow-up PRs (optional)
9. Attachments (optional)

Below these sections, separated by a `---` line, an optional appendix holds the release note.

For details, see [template](TEAMPLATE.md)

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

## Changing Further PR Documents

A PR normally changes exactly one PR document, and `tools/pr-quick-check` enforces that.
When a PR legitimately has to change more, for example to back-fill release notes into merged
PRs, its own PR document declares how many further ones it changes:

    `pr-quick-check:extra-pr-docs=<count>`: <reason>
