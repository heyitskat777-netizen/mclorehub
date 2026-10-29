# Phase 7 — Main lore, monthly automatic checks and optional sessions

The latest user instruction overrides Phase 6's reply/pagination behavior. Default
search and reading include only `content_text` / the first main forum post. Refresh
compares main body, title, author and observed source status, not discussion.
It requests only the selected topic's first page (plus its public robots policy).
Previously saved replies are retained in original packs/snapshots and remain under
Source and review → Archived replies. Opening that archive does not replace the
saved main-lore reading position. Old reply-position saves reset safely to main text.

## Monthly automation

Enabled by default in the client. The dedicated background scheduler checks only
known saved topic IDs while Minecraft is running. Newly seen pages are first due
one calendar month after activation; already-refreshed pages are due one month
after the last successful source check. UTC calendar-month arithmetic handles
short months/leap years. Dates/progress persist in `updates/monthly.json`; overdue
pages catch up after restart. It cannot wake Minecraft or run while the game is closed.

Checks run one at a time with the existing request spacing and atomic snapshot/
index/Undo pipeline. Manual success updates the monthly due date. No-change checks
also persist their check date. HTTP 401/403/404 retain stored lore and defer that
page to the next month. Other failures back off 1–7 days; Retry-After defers the
automatic queue. Cancelling an automatic check defers that page for a day; pausing
or closing preserves its due date for resumption. Monthly pause does not cancel
someone else's manual refresh. No global forum search or new-topic discovery runs.

Open **Updates** to enable/pause monthly checks or process currently due pages.
Manual Refresh remains available. A corrupt, future-format or externally changed
schedule pauses automation and retains the file; repair/back up it and restart.
Source snapshots, favorites, characters and original pack remain independent.

## Forum access

Optional browser-session import is implemented; see [session guide](FORUM_SESSION.md).
The mod does not collect passwords, extract browser stores or automate login forms.
Cookie headers are restricted to exact official HTTPS topic URLs; unsupported/
challenge/unrelated cookies are discarded. Real authenticated MArt access depends
on the user's account, the page still existing and server acceptance of its session.
No actual user account/session was available for verification in this development run.

## Validation

Core tests exercise main-only fetching/search/reader/archive behavior, month-end
dates, no startup bulk fetch, catch-up, manual-date resets, restart, pause, error
backoff and Retry-After, cookie host/path/expiry scope, redaction, secure import,
forget and unchanged source/personal data. Native tests advance a clock to trigger
an actual scheduled check, use the pause control, and import/forget synthetic
session cookies without exposing their value. The real 488-record corpus remains
normally available under user trust. **198 core tests and all native scenarios
passed.** A live main-only Tawkin check inside Minecraft fetched one page and no new
replies and survived restart. Synthetic cookie tests are not real authentication.
See STATUS.md for final evidence and limits.

Compatibility rules, curated lore selection and real modpack/hardware/authenticated
MArt acceptance remain future work. This phase does not fabricate authentication
success, staff approvals, compatibility rules or new accepted lore.
