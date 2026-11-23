# User Story 02: Show the app in the device's preferred language

## User story

> As a user of the mobile app, I want the application to automatically detect
> my device's preferred language and display the app in that language so that
> I can navigate and use the platform without manually changing language
> settings.

## Definition of done

Users can immediately access Ripple Effect Accounting in their preferred
supported language, reducing language barriers and making the application
more accessible to communities in Latin America and East Africa.

## Documentation

### Terms used here

| Term | Meaning in the app |
|---|---|
| Device's preferred language | The first language in the phone's language list (**Settings > System > Languages**) that the app supports. Android 7+ lets a user list several languages in order; the app uses the first one it has. On Android 13+ a user can also pick a language for this app only (**Settings > Apps > Ripple Effect > Language**); that choice counts as the preferred language. |
| Supported language | A language the app has a complete translation for: **English** (default), **Spanish**, **Swahili** and **Portuguese**. |
| Fallback | If none of the phone's languages is supported (for example French or Amharic), the app is shown in English. |
| Displayed text | Every piece of text the app itself shows or produces: screen titles, labels, buttons, tab names, dialogs, toasts, empty states, filter menus, the sale/expense type shown in the report list, the report text and the email subject and body it prepares, and descriptions read by TalkBack. |
| User data | What the user types (entry descriptions, summary text, email address). It is shown exactly as typed and is never translated. |
| Immediately | On the very first screen after launch, with no setup or language picker, and right away when the phone's language changes while the app is open. |

### Problem today

- The app always starts in **English**, whatever the phone's language is
  (`var language by remember { mutableStateOf("en") }` in `RippleEffectApp`).
- Spanish is only reached through a manual **English / Español** switch at the
  top of the Report tab, next to a demo phrase ("Hello, how are you?").
- All text is hard-coded in Kotlin as `if (isSpanish) "…" else "…"` pairs
  (about 100 of them in `MainActivity.kt`), plus `labelEs` / `nameEs` fields on
  the `FilterField`, `FilterOperator` and `Currency` enums. There is no
  `strings.xml`, so Android's built-in language selection cannot work and a
  third language would mean another branch at every line.
- Some text is English in both modes: the **Expenses / Income** badges on the
  summary cards, the **Email** field label, the sale/expense type in the
  report list, the **AND / OR** filter buttons, and the email subject, body
  and chooser title.
- The Spanish empty-state hint says to use the "Artículos" (Items) tab, which
  does not exist; the tab is called **Report** / **Informe**.

### Which languages and why

| Language | Resource folder | Who it is for | Notes |
|---|---|---|---|
| English | `values/` | Default and fallback for everyone else. | Existing English text is kept as the source. |
| Spanish | `values-es/` | Latin America (the app's currencies, GTQ and HNL, are Guatemala and Honduras). Matches `es`, `es-419`, `es-GT`, `es-HN`, `es-MX`, etc. | Existing Spanish text is kept word for word, except the Profit card title (see pass 7). New Spanish strings were written by Claude. |
| Swahili | `values-sw/` | East Africa (Kenya, Tanzania, Uganda). Matches `sw`, `sw-KE`, `sw-TZ`. | Machine translated by Claude. **Needs native-speaker review.** |
| Portuguese | `values-pt/` | Brazil, the largest Latin American country. Matches `pt-BR` and `pt-PT`. | Machine translated by Claude (Brazilian wording). **Needs native-speaker review.** |

Left out for now: French (Rwanda, Burundi, Haiti), Amharic, Kinyarwanda,
Luganda, Somali and indigenous Latin American languages such as K'iche'.
Adding one later is a new `values-xx/strings.xml` plus an entry in
`locales_config.xml`; no code changes are needed.

### How it will work

1. **All displayed text moves to string resources.** English goes in
   `res/values/strings.xml`; each supported language gets its own
   `res/values-xx/strings.xml` with the same keys. Composables read text with
   `stringResource(...)` / `pluralStringResource(...)`; code outside the UI
   (report text, email intent, toasts) uses `context.getString(...)`.
2. **Android picks the language.** At launch Android matches the phone's
   language list against the app's resource folders and loads the first
   match, falling back to `values/` (English). The app does no language
   detection of its own, so it cannot disagree with the phone.
3. **The manual English / Español switch and demo phrase are removed.** The
   app follows the phone. A user who wants a different language from the
   rest of their phone can choose one for Ripple Effect in Android 13+
   **App language** settings, which the app enables by declaring its
   languages in `res/xml/locales_config.xml`.
4. **Language changes apply at once without losing work.** The activity
   declares `android:configChanges="locale|layoutDirection"`, so when the
   phone or app language changes while the app is open, the screen redraws in
   the new language and draft entries, the selected tab and filters are kept.
5. **Stored data stays language-neutral.** Submitted entries keep `Sale` /
   `Expense` and the currency code in the database (story 01) and in Data
   Connect. Only the label shown on screen and in exported text is
   translated, so filters, sorting and saved records keep working when the
   language changes.
6. **Numbers and dates keep their current format.** Amounts stay `$5.00`
   style and the report list keeps `MM/dd/yyyy HH:mm`, because the date
   filter asks the user to type `MM/dd/yyyy`. Localised number and date
   formats are a separate change (see Limits).

### Limits and assumptions

- Translations for Swahili, Portuguese and the new Spanish strings were
  produced by Claude, not by a professional translator. They must be reviewed
  by native speakers before release; the strings files mark them with a
  `NEEDS REVIEW` comment.
- The app's currencies (USD, GTQ, HNL) are Latin American. East African
  currencies (KES, TZS, UGX) are a separate story.
- Number and date formats (decimal comma, day-first dates) are not changed by
  this story.
- The CSV attachment keeps English column headers and `Sale` / `Expense`
  values so it stays machine-readable for spreadsheets and imports; the text
  report attached next to it is translated.
- The brand name **Ripple Effect** is not translated.
- Sample data in the unused "Saved reports" screen ("Annual Audit", "John
  Doe", owner "Current User") is placeholder data, not displayed text, and is
  left as is.
- This branch builds on story 01 (PR #1), because story 01 adds a message
  ("Could not save entries on this device") that must also be translated.

## Tasks

- [x] T1. Add `res/values/strings.xml` with every displayed English string,
      including plurals for the record count and format arguments (`%1$s`)
      where text contains a value. Mark `app_name` as not translatable.
- [x] T2. Add `res/values-es/strings.xml`, reusing the existing Spanish text
      word for word and translating the strings that were English-only.
- [x] T3. Add `res/values-sw/strings.xml` (Swahili) and
      `res/values-pt/strings.xml` (Portuguese) with every key, marked
      `NEEDS REVIEW`.
- [x] T4. Replace every `if (isSpanish) … else …` in `MainActivity.kt` with
      string resources, and remove the `isSpanish` / `language` parameters.
- [x] T5. Change the `FilterField`, `FilterOperator` and `Currency` enums to
      hold string resource ids instead of `label` / `labelEs` and `nameEn` /
      `nameEs`.
- [x] T6. Show `Sale` / `Expense` through translated labels in the report
      list, the type filter and the exported text, while the stored value
      stays `Sale` / `Expense`.
- [x] T7. Fix the **Expenses / Income** badge so it takes its label from a
      parameter instead of comparing the card title to English and Spanish
      text.
- [x] T8. Translate the report text, report name (with its date order as a
      string resource), email subject, body,
      chooser title and error messages built in `sendEmailWithReport` and the
      submit handler, plus story 01's save-failed toast.
- [x] T9. Remove the manual English / Español switch and demo phrase.
- [x] T10. Point the app label at `@string/app_name`, add
      `res/xml/locales_config.xml` (en, es, sw, pt) and reference it with
      `android:localeConfig`, and add
      `android:configChanges="locale|layoutDirection"` to `MainActivity`.
- [x] T11. Add content descriptions for the logo and the edit / delete icons
      so TalkBack reads them in the user's language.
- [x] T12. Add a unit test that checks every translation has exactly the same
      string and plural keys as English, uses the same format arguments, and
      that `locales_config.xml` lists exactly the languages that have a
      `values-xx` folder.
- [x] T14. Package only the supported languages
      (`androidResources.localeFilters` in `app/build.gradle.kts`) so
      library translations cannot make Android pick a language the app's
      own text does not have, and extend T12's test to check the list.
- [x] T15. Give the zero-record count its own string, because Portuguese and
      French plural rules treat 0 as singular ("0 registro").
- [ ] T13. Verify on the emulator, from a fresh install, with the phone set
      to each of: English, Spanish, Swahili, Portuguese, and an unsupported
      language (French) → English. Also check a list of French then Spanish
      → Spanish, and changing the language while the app is open with a
      draft entry → the screen switches language and the draft is kept.

## Logical review

Each pass compared the user story, the definition of done, the
documentation and the tasks, and fixed whatever did not line up before the
next pass.

### Pass 1

| # | Mismatch found | Fix |
|---|---|---|
| 1 | The app's existing manual switch always starts in English and makes the user change language by hand, which is exactly what the story wants to avoid. Keeping it as the main control would contradict "automatically detect". | Remove the switch (T9); Android picks the language from the phone (How it works 2–3). Android 13+ per-app language replaces it for users who want a different language (T10). |
| 2 | "Supported language" was undefined, so the definition of done could not be checked. | Defined the supported set and the English fallback, with reasons tied to Latin America and East Africa (Which languages and why). |
| 3 | The first plan only covered screen labels. "Display the app in that language" also covers dialogs, toasts, empty states, the report text and email the app prepares, and items that are English in both modes today. | Defined "Displayed text" and listed the English-only items in Problem today; T6, T7, T8, T11 cover them. |
| 4 | Translating the `Sale` / `Expense` type at save time would store different words per language, breaking the type filter, the colour of each row and story 01's saved records. | Store the neutral value and translate only at display (How it works 5, T6). |
| 5 | The summary card badge decides "Expenses" vs "Income" by comparing its title to "Costs" / "Costos", which breaks as soon as a third language exists. | T7 passes the badge label in directly. |

### Pass 2

| # | Mismatch found | Fix |
|---|---|---|
| 6 | "Immediately" in the definition of done had no meaning in the doc. Without handling it, changing the phone language while the app is open recreates the activity and throws away draft entries. | Defined "Immediately" (Terms) and added `configChanges` (How it works 4, T10) with an emulator check (T13). |
| 7 | Phones can list several languages. The doc only described a single language, so a French-then-Spanish user's result was unclear. | Terms define the preferred language as the first supported one in the list; T13 checks that case. |
| 8 | A key missing from one translation would silently show English in the middle of a translated screen, which the definition of done would not catch by eye. | T12 adds a unit test that every translation has every key and matching format arguments. |
| 9 | Swahili and Portuguese are machine translated; "reducing language barriers" depends on quality that cannot be verified here. | Limits require native-speaker review; T3 marks files `NEEDS REVIEW`. |
| 10 | Dates and amounts follow US format, which some users may expect in local format; localising the report list's dates would break the `MM/dd/yyyy` date filter. | Kept formats unchanged and listed this as a separate change (How it works 6, Limits). |

### Pass 3

| # | Mismatch found | Fix |
|---|---|---|
| 11 | Story 01 (PR #1, not yet merged) adds a hard-coded English/Spanish toast. Building on `main` would leave that text untranslated once both merge. | This branch builds on story 01 (Limits) and T8 includes the toast. |
| 12 | The Spanish empty-state hint names an "Artículos" tab that does not exist, so it would mislead users even when correctly translated. | Hint now names the Report tab in every language (T2, T3). |
| 13 | `locales_config.xml` and the `values-xx` folders could drift apart, so Android settings might offer a language the app does not have. | T12's test also checks that the two lists match. |
| 14 | "More accessible" in the definition of done: icons without content descriptions are silent to TalkBack in any language. | T11 adds translated content descriptions for the icons on the main screens. |

### Pass 4

No mismatches found. Every part of the story maps to documentation and a
task: automatically detect (How it works 2, T10), device's preferred language
(Terms, T13), display the app in that language (Displayed text, T1–T8, T11),
without manually changing settings (T9). The definition of done maps as
follows: immediately (T10, T13), preferred supported language (Which
languages, T13), Latin America (Spanish, Portuguese) and East Africa
(Swahili), checked by T12 and T13.

### Pass 5 (during implementation)

| # | Mismatch found | Fix |
|---|---|---|
| 15 | Report and export names put the date month-first in English and day-first in Spanish. Choosing the order from the phone's first language would disagree with the text whenever Android falls back (a French phone shows English text with a day-first date). | The date order is a string resource (`report_date_pattern`), so it always matches the language on screen (T8). |
| 16 | After choosing a value in the **Type** filter, the button showed the stored value ("Expense") instead of the translated label. | The button and menu both show the translated label; the stored filter value stays `Sale` / `Expense` (T6). |
| 17 | The unused "Saved reports" screen sets the owner to "Current User", which looked like untranslated text. | It is placeholder data like the sample reports; listed under Limits. |

### Pass 6

No mismatches found. Pass 4's mapping still holds, and the three items from
pass 5 are covered by T6, T8 and Limits.

### Pass 7 (emulator verification, T13)

The first emulator run passed 9 of 10 checks. These mismatches came out of it:

| # | Mismatch found | Fix |
|---|---|---|
| 18 | French then Spanish showed **English**, not Spanish. The app's libraries ship French translations, so the APK had a `fr` folder and Android treated French as supported, leaving the app's own text on the English fallback. That breaks "preferred supported language". | T14: `localeFilters` keeps only en, es, sw, pt in the APK; the unit test checks it matches the `values-xx` folders. |
| 19 | The record count read "0 registro" in Portuguese and "0 record" on a French phone (English text, French plural rules). | T15: a separate `records_zero` string for an empty list. |
| 20 | The Spanish Profit card was titled "Ingresos", the same word as its Income tag, so the card read "Ingresos / Ingresos". | Spanish `profit` is now "Ganancias" and `total_profit` "Ganancias Totales:", flagged for native review. |

### Pass 8

No mismatches found. T14 and T15 are mapped to the definition of done
("preferred supported language") and to T12/T13; the re-run of T13 checks
them.

## Verification (T13)

Run on the Android emulator (Medium Phone, API 37) with a fresh debug
install. Language set with `adb shell cmd locale set-device-locale`, and the
two-language list through Settings > System > Languages.

Unit tests: `StringResourcesTest` 5 of 5 passed; `assembleDebug` passed.

| Check | First run | After pass 7 fixes |
|---|---|---|
| English (en-US) | Pass | |
| Spanish (es-MX) | Pass | |
| Swahili (sw-KE) | Pass | |
| Portuguese (pt-BR) | Pass | |
| French (fr-FR) falls back to English | Pass | |
| No manual language switch | Pass | |
| French then Spanish shows Spanish | **Fail** (English) | |
| Language change with app open keeps the draft entry | Pass | |
| Spanish table shows Venta / Gasto, "2 registros" | Pass | |
| Swahili table shows Mauzo / Matumizi, "rekodi 2" | Pass | |
| Android 13+ app language lists English, Español, Kiswahili, Português | Pass | |
