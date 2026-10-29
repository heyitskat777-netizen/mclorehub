# Optional forum session and restricted MArt pages

Yes: an authenticated forum session can let the mod read pages that **your account
can already view**. It cannot grant staff-only permissions, restore deleted pages,
or bypass a site challenge. No ready-to-use OAuth client was verified for this forum;
the public OAuth/OpenID discovery endpoints checked returned 404. This is optional
session reuse, not an automatic username/password sign-in flow.

1. In the mod's **Updates** screen, choose **Open forum sign-in**. Sign in normally
   in your own browser, including any MFA. Verify that the particular MArt page is
   accessible there; a login alone does not establish permission.
2. Using your own trusted local tooling, export this forum's session cookies in
   Netscape `.txt` format. Do not export/share your full browser profile. The mod
   accepts only supported Invision session-cookie names for this forum and ignores
   unrelated cookies and challenge-clearance cookies. JSON cookie exports and
   password files are not accepted.
3. Return to Updates and drop that local `.txt` file onto the screen. The mod copies
   only the selected forum-session cookies into `config/mclorehub/auth/forum-session.json`.
   It does not read browser credential databases, store your password, or upload the
   exported file. The source export remains yours to remove/secure.
4. Open the MArt document and Refresh topic. If the account/session is accepted, its
   main lore is saved through the normal update pipeline and later monthly checks
   can use that session. If login/access fails, cached lore remains intact.
5. Use **Forget session** to remove the mod's local copy, including expired/corrupt
   sessions. Sign in and import again when the session expires or is revoked.

Session cookies are credentials: anyone with them may be able to use your session.
Keep the exported file private, never paste it into chat or upload it, and exclude
`auth/` from Git, database bundles, support archives and shared backups. The stored
file is local plaintext with owner-only permissions on POSIX systems; it is not an
encrypted OS credential vault. On other systems it inherits the user's config-folder
permissions. Original exports are not deleted by Forget session.

Headers are sent only to `https://www.lordofthecraft.net/forums/topic/...`, never
robots/login/profile/media, another host, HTTP, arbitrary queries or redirect targets
outside the selected topic. Cookie values are withheld from UI, logs, exceptions,
snapshots and bundles. Server-side access checks still apply. No Cloudflare or login
challenge bypass is implemented.

Synthetic cookie import/scope/expiry/redaction/forget tests passed. **Real MArt
authentication has not been verified with your account**. If a legitimate session
still receives an access/login/challenge page, the mod reports failure and retains
saved lore. A registered forum API/OAuth client would require a separately supported
integration, rather than guessing endpoints or collecting passwords.
