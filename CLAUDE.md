# Working on this fork

This is a personal fork of [damontecres/Wholphin](https://github.com/damontecres/Wholphin) (upstream `origin`), published as `codecat5/Wholphin`. The `custom` branch carries a small set of local patches (a rejected upstream PR, CI/signing setup, rebrand, and feature additions like the "Upcoming" tab) on top of upstream `main`.

`publish-custom-build.ps1` regularly merges `origin/main` into `custom` and pushes a signed build. This happens repeatedly, indefinitely — it is not a one-time fork-and-forget. **Every change committed to `custom` is a standing liability for that merge**, for as long as this fork exists.

## Golden rule: minimize merge-conflict surface

Before writing code for this fork, prefer the option that touches the fewest lines of files upstream is likely to also touch. Concretely, in rough order of preference:

1. **New, self-contained file** for new functionality (a new Composable, ViewModel, service). A file upstream doesn't have can never conflict.
2. **Strictly additive edit** to a shared file: a new optional parameter with a safe default, a new field with a default value, a new `if` block appended at a natural insertion point. Upstream's existing lines are untouched, so a 3-way merge resolves cleanly even if upstream edits nearby code.
3. **Renumbering/reordering existing code** (e.g. inserting into the middle of a tab list or a chain of `X_ROW = Y_ROW + 1` constants) — only when the feature genuinely requires that position. Keep the diff as small as the codebase's own conventions allow (e.g. this codebase already computes row/tab constants relative to each other, which keeps a mid-list insertion cheap — lean on patterns like that rather than hardcoded indices).
4. Avoid entirely: reformatting, renaming, or restructuring code you don't need to touch, even if it "looks nicer." Every touched line is a line that can conflict on the next `origin/main` merge.

When a new capability needs to hook into an existing page (e.g. a new row on a details page), it's fine — often unavoidable — to add a couple of parameters to that page's composable/state. Keep the *logic* (the actual rendering, the API call, formatting) in the new dedicated file, and make the shared file's job just "pass data through and call it."

## Before finishing a change

Run `git diff --stat` on files outside anything newly created and sanity-check: does each touched shared file's diff look additive and small, or did it grow because logic that could live in a new file got inlined instead? If the latter, factor it out.

## Mention this to the user

If a request would require deep, non-additive changes to a heavily-shared file (e.g. renaming a widely-used function, restructuring a core ViewModel), flag the merge-conflict tradeoff before doing it rather than silently taking the invasive path.
