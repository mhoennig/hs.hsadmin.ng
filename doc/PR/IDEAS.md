# Ideas and Free-Floating TODOs

This file collects ideas and TODOs which hang on no specific code location
and belong to no open PR yet.
Several related entries can later grow into a PR of their own;
a single entry may join a PR as an "Additional Change"
when it roughly fits that PR's topic or the code it touches.

Conventions:

- One entry per bullet in the format `- [ ] TODO.<area>: <idea>`,
  with enough context to understand it later without the original conversation.
- Whoever takes an entry into a PR moves it into that PR document
  and deletes it here; nothing gets checked off in this file.

## Entries

- [ ] TODO.test: Consider a check against boolean-heavy signatures and types.
      Neither the PMD ratchet (`config/pmd/complexity.xml`) nor the `ArchitectureTest` notices
      a record full of booleans or a parameter list with several booleans today.
      Candidates: PMD `ExcessiveParameterList` in ratchet style, a custom XPath rule,
      or an ArchUnit rule against more than two `boolean` parameters per method.
      The underlying design question — flags instead of enums or dedicated types — stays with the review.
