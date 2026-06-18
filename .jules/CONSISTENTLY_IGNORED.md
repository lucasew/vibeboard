## IGNORE: Committing Tooling Artifacts

**- Pattern:** Committing downloaded binaries (e.g., `mise`), archives (`mise.tar.gz`), or bootstrap scripts into the repository.
**- Justification:** Tool binaries bloat the repository and pose a security risk. Setup must be handled via environment workflows (CI) or local setup scripts, not by checking binaries into version control.
**- Files Affected:** `mise`, `mise.tar.gz`, `mise/bin/mise`

## IGNORE: Scope Creep and Unrelated Bundling

**- Pattern:** Including out-of-scope changes in a PR (e.g., adding `mise.toml` in a Sentinel PR focused on exceptions, or moving/renaming UI files in a Refactor PR focused on extracting magic numbers or logic).
**- Justification:** Violates the "Scope Discipline" rule. PRs must remain minimal, atomic, and focused purely on the stated objective to allow easy review and revert.
**- Files Affected:** `mise.toml`, `app/src/main/java/br/tec/lew/vibeboard/VibeboardKeyboard.kt`, `app/src/main/java/br/tec/lew/vibeboard/theme/Theme.kt`

## IGNORE: Obvious or Redundant Documentation

**- Pattern:** Adding KDoc/JSDoc comments that merely restate the component's name or basic function (e.g., documenting that `MainActivity` is the entry point, or explaining that `VibeboardKeyboard` renders the UI).
**- Justification:** Violates the documentation rule to explain the "why", system flow, and non-obvious nuances. Restating the obvious adds noise without value.
**- Files Affected:** `app/src/main/java/br/tec/lew/vibeboard/MainActivity.kt`, `app/src/main/java/br/tec/lew/vibeboard/VibeboardService.kt`, `app/src/main/java/br/tec/lew/vibeboard/ui/VibeboardKeyboard.kt`

## IGNORE: False Positive Security Fixes

**- Pattern:** Implementing protections against non-threats, such as adding `FLAG_SECURE` / tapjacking protection to non-sensitive onboarding screens, or wrapping standard Android Settings implicit intents (like `showInputMethodPicker`) in `try/catch` blocks.
**- Justification:** Reacting to generic security warnings without context leads to poor UX (e.g., blocking screenshots) and unnecessary boilerplate for standard system functions.
**- Files Affected:** `app/src/main/java/br/tec/lew/vibeboard/MainActivity.kt`

## IGNORE: Invalid Janitor Cleanups

**- Pattern:** Suppressing warnings (`@Suppress("DEPRECATION")`) instead of fixing them, or removing mandatory API level checks (e.g., `dynamicColor` requiring Android S) under the guise of removing "obsolete" code.
**- Justification:** Suppressing warnings defeats the purpose of cleanup, and removing valid SDK checks introduces regressions and runtime crashes on older devices.
**- Files Affected:** `app/src/main/java/br/tec/lew/vibeboard/VibeboardService.kt`, `app/src/main/java/br/tec/lew/vibeboard/ui/theme/Theme.kt`

## IGNORE: Hallucinated Dependency Updates

**- Pattern:** Updating dependencies like `org.jetbrains.kotlin.plugin.compose` to non-existent or hallucinated versions (e.g., `v2.3.21`).
**- Justification:** Bumping to incompatible or future versions breaks the build and gets automatically rejected. Dependency updates must use verified, existing versions.
**- Files Affected:** `gradle/libs.versions.toml`

## IGNORE: Over-abstracting Core Lifecycle Components

**- Pattern:** Extracting core Android lifecycle components (like `LifecycleOwner`, `ViewModelStoreOwner`, `InputConnection`) from the primary `InputMethodService` (`VibeboardService`) into separate wrapper classes (e.g., `ComposeLifecycleOwner`, `InputController`).
**- Justification:** `VibeboardService` inherently acts as the orchestrator for the keyboard lifecycle. Over-abstracting these specific Android system integrations adds unnecessary indirection and complexity without meaningfully improving cohesion.
**- Files Affected:** `app/src/main/java/br/tec/lew/vibeboard/VibeboardService.kt`, `app/src/main/java/br/tec/lew/vibeboard/ComposeLifecycleOwner.kt`, `app/src/main/java/br/tec/lew/vibeboard/InputController.kt`