# Pull-Request Documentations in doc/PR

This directory contains documentation for each pull request (PR).

IMPORTANT: The PR-documentation documents the change in that PR, it might be outdated right after the next PR was merged. Historic PR-documentation is not maintained.

## Naming Convention

1. Date of the PR in the format `YYYY-MM-DD`
2. followed '-PR#' followed by the number of the pull request in GitEA,
3. a short description of the PR with dashes between the words,
4. '.md'

Yes, for this you need to open a pull request,
but initially prefix its title with "WIP: " to mark it as a work in progress until it is ready for review.

## Guidelines

- Documentations must be written in Markdown format.
- Use Englisch, it's a public open source project.
- Use clear and concise language and keep it short.
- Include relevant details and context.
- Do not reference to Taiga or any other tool that is not public.
- If necessary, copy important parts of the ticket description.
- One sentence or statement per line to make diffs easier to read.

## Structure

```Markdown

## The Problem

Here you describe what this PR is supposed to solve.

## The Solution

Here you describe the changes you made and why you made them.

If necessary, you can link to an ADR (Architecture Decision Record).

## Additional Changes

Here you list any additional changes you made, e.g. "fixed formatting in ..." or "fixed some naming issues".

## Attachments

Here you can add any longer sections that would interrupt the reading flow in the previous sections.
Put each attachment on a level-3 heading ('### ...').




```


