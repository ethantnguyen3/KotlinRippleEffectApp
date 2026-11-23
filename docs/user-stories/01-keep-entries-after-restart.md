# User Story 01: Keep submitted entries after the app restarts

## User story

> As a community user, I want all submitted sales and expense entries to be
> securely saved and retained in the database so that I can access my previous
> records in the report list even after closing and reopening the mobile app.

## Definition of done

Once a user submits an entry, it stays on their report list even after they
close out of the app.

## Documentation

### Terms used here

| Term | Meaning in the app |
|---|---|
| Entry | One sale or expense line the user adds with **+ Sale** / **+ Expense** on the **Report** tab (`AccountingEntry`). |
| Submit | Tapping **Submit Report** on the **Report** tab. Every entry currently listed is submitted together. |
| Submitted entry | An entry after it has been submitted (`SubmittedEntry`): description, amount, currency, Sale/Expense, date. |
| Report list | The table on the second tab (labelled **Categories** / **Tabla**, `DataTableScreen`) that lists every submitted entry and can be filtered, sorted and emailed. |
| Close the app | Any way the app process ends: back out, swipe it away from recents, the system killing it, or restarting the phone. |
| Database | An SQLite database stored in the app's private storage on the phone. |

### Problem today

Submitted entries only live in memory (`mutableStateListOf` in `RippleEffectApp`).
The app also tries to send them to Firebase Data Connect, but
`DATACONNECT_SERVICE_URL` and `DATACONNECT_API_KEY` are blank in
`gradle.properties`, so `Backend` skips both the save and the load. When the
app process ends, every submitted entry is gone and the report list is empty
on the next launch.

### How it will work

1. **Saving.** When the user taps **Submit Report**, the app turns each entry
   into a submitted entry and writes all of them to the on-device database in
   **one transaction** (either every entry of that report is saved or none
   is). Only after the write finishes are they added to the report list on
   screen, newest first. If the write fails, the entries are still shown for
   this session and a message tells the user they could not be saved.
2. **Loading.** Every time the app starts (and when the user taps **Retry**),
   it reads all submitted entries from the database, newest first, and shows
   them in the report list. If the database cannot be read, the existing
   **Retry** screen is shown.
3. **Firebase Data Connect (optional backup).** Sending entries to Data
   Connect stays as it is today: a best-effort copy that only runs when the
   endpoint is configured. The on-device database is what the report list is
   built from, so the list works with no network and no backend. If the
   on-device database is empty (for example after reinstalling) and Data
   Connect is configured and returns entries, those entries are shown and
   stored in the database so they are kept from then on. Remote entries are
   never merged into a non-empty local list, because Data Connect gives
   entries its own ids and merging would show each entry twice.

### Security

- The database file lives in the app's internal storage
  (`/data/data/com.example.rippleeffect/databases/`), which other apps cannot
  read. Android's device encryption protects it while the phone is locked.
- All reads and writes use parameterised SQL, so text a user types (for
  example a description containing quotes) cannot change the query.
- Not covered by this story: an extra encryption layer on the database file,
  user accounts or sign-in, and editing or deleting submitted entries.

### What is stored

Table `submitted_entries`:

| Column | Type | Notes |
|---|---|---|
| `id` | TEXT, primary key | UUID made when the entry is submitted |
| `description` | TEXT, not null | What the user typed, e.g. "Water bottles" |
| `amount` | REAL, not null | Amount as entered |
| `currency` | TEXT, not null | Currency code, e.g. `USD` |
| `type` | TEXT, not null | `Sale` or `Expense` |
| `date_created` | INTEGER, not null | Milliseconds since 1970 (UTC) |

### Limits and assumptions

- The app has no sign-in, so "their report list" means every entry submitted
  on this phone in this app install.
- Uninstalling the app or using **Clear storage** in Android settings deletes
  the database, as with any app's private data.
- Entries that are added but not yet submitted (the draft list on the Report
  tab) are not saved. The definition of done covers submitted entries only.
- The "Saved reports" list (`SavedReport`) is not shown on any tab today and
  is out of scope.

## Tasks

- [x] T1. Add `LocalEntryStore` (new file `LocalEntryStore.kt`): an
      `SQLiteOpenHelper` that creates the `submitted_entries` table above.
- [x] T2. `LocalEntryStore.loadAll()`: return every row as a `SubmittedEntry`,
      newest `date_created` first, using parameterised queries.
- [x] T3. `LocalEntryStore.insertAll(entries)`: write a list of entries in one
      transaction using `insertWithOnConflict` (ignore an id that already
      exists, so re-saving the same entry cannot create a duplicate).
- [x] T4. Run both functions on `Dispatchers.IO` so the screen never freezes.
- [x] T5. In `RippleEffectApp`, create the store from the application
      context and, on start / Retry, load entries from it into
      `submittedEntries`. A read failure shows the Retry screen.
- [x] T6. On start, if the local list is empty, fetch from Data Connect; if
      it returns entries, show them and store them with `insertAll`.
- [x] T7. On **Submit Report**, build the `SubmittedEntry` list once, save it
      with `insertAll`, then add it to the top of `submittedEntries`. Give
      the entries of one submit dates one millisecond apart so the order on
      screen is the same after a restart.
- [x] T8. If the save fails, still add the entries to the list for this
      session and show a short message (English / Spanish) saying they could
      not be saved.
- [x] T9. Keep the existing best-effort `Backend.saveSubmittedEntries` call.
- [x] T10. Verify on the emulator: submit a sale and an expense, swipe the
      app away from recents (or `adb shell am force-stop
      com.example.rippleeffect`), reopen, and confirm both are still in the
      report list with the same description, amount, currency, type and date.

## Logical review

Each pass compared the user story, the definition of done, the
documentation and the tasks, and fixed whatever did not line up before the
next pass.

### Pass 1

| # | Mismatch found | Fix |
|---|---|---|
| 1 | The story says "the database", but the only database in the code (Data Connect) is not configured, so a plan that relied on it could never meet the definition of done. | Documentation names an on-device SQLite database as the store the report list is built from; Data Connect stays an optional backup (T1, T9). |
| 2 | "Report list" was ambiguous: the code has a `SavedReport` list and a submitted-entries table. | Defined it as the table on the second tab (`DataTableScreen`), which is the only list on screen. `SavedReport` marked out of scope. |
| 3 | The plan added entries to the screen first and saved in the background. If saving failed, the user would see entries that disappear after a restart, which breaks the definition of done without warning. | Save first, then show (T7); on failure tell the user (T8). |
| 4 | A report with several entries could be half saved if the app was killed mid-write. | Write all entries of one submit in a single transaction (T3). |
| 5 | Loading from Data Connect and from the local database both at start would show every entry twice when the backend is configured, because the ids differ. | Use remote only when the local database is empty, and cache it locally (Documentation step 3, T6). |

### Pass 2

| # | Mismatch found | Fix |
|---|---|---|
| 6 | "Securely" in the story had no matching task or documentation. | Added the Security section (private storage, parameterised SQL) and T2/T3 require parameterised queries. Extra file encryption listed as out of scope. |
| 7 | "Their report list" implies a per-user list, but the app has no sign-in. | Documented the assumption: the list is per phone / app install. |
| 8 | The definition of done says "close out of the app", but no task checked that. | Added T10, which force-stops the app and checks every field after reopening. |
| 9 | T6 caches remote entries with `insertAll`; if the same remote entry were cached twice it would duplicate. | T3 ignores ids that already exist. |

### Pass 3

No mismatches found. Every part of the story maps to documentation and a
task: saved (T1, T3, T7), securely (Security, T2, T3), retained in the
database (T1, T5), report list (T5, T7), after closing and reopening (T5,
T10). The definition of done is checked by T10.

### Pass 4 (during implementation)

| # | Mismatch found | Fix |
|---|---|---|
| 10 | All entries of one submit had the same date, so after a restart their order in the report list could change, even though the definition of done expects the same list back. | T7 gives them dates one millisecond apart, and the database sorts by date then insert order. |

## Verification (T10)

Run on the Android emulator (Medium Phone, API 37) with a debug build, starting from empty app data:

1. Added "Water test" 5 as a Sale and "Bottles test" 2 as an Expense, then tapped **Submit Report**.
2. The report list showed 2 records: Bottles test, $2.00, USD, Expense and Water test, $5.00, USD, Sale.
3. Force-stopped the app (`adb shell am force-stop com.example.rippleeffect`) and relaunched it.
4. The report list showed the same 2 records, with the same amounts, currency, type and order.
5. `ripple_effect.db` was present in the app's private `databases` folder.

Definition of done: met.
