# Paisa Android App Design

Date: 2026-04-19

## Goal

Build a real Android personal finance app that lets a user log money activity in under 3 seconds. The app should stay simple to run, test, and change. It should work offline, avoid account setup, and keep the first version focused on quick awareness rather than complex budgeting.

## Product Scope

The first version opens directly to the logging experience. The user types or speaks a short phrase, and the app records the transaction automatically.

Example entries:

- `200 food`
- `5000 salary`
- `300 to Rahul`
- `300 from Aman`
- `+2000 freelance`

Core features:

- Quick text entry
- Voice input through Android speech recognition
- Recent transaction list
- Simple dashboard
- People balance tracking
- Daily spending notification
- Weekly summary notification
- Local offline storage
- Smart suggestions from recent categories and people

Out of scope for the first version:

- Cloud sync
- Login/accounts
- Bank integrations
- Receipt scanning
- Complex budget planning
- Multi-currency support
- AI/cloud parsing

## Recommended Tech Stack

- Language: Kotlin
- UI: Jetpack Compose
- Storage: Room
- Architecture: simple MVVM
- Background work: WorkManager
- Voice input: Android `RecognizerIntent`
- Build: standard Android Gradle project

This stack gives a real native Android app with a small dependency surface. It is easy to run from Android Studio, easy to test locally, and does not require a backend or web runtime.

## App Structure

The app will use a small, feature-based package layout:

- `data`: Room entities, DAO, database, repository
- `domain`: transaction parser, category rules, summary calculations
- `ui`: Compose screens and state holders
- `notifications`: daily and weekly notification workers

Screens:

- Home screen: quick entry, voice button, dashboard cards, recent transactions
- People screen: list of people and net balances
- History screen: searchable or grouped transaction list

The first build can keep these as simple tabs or a bottom navigation bar.

## Data Model

`Transaction`

- `id`
- `amount`
- `type`: expense, income, lent, borrowed
- `category`
- `personName`
- `note`
- `rawText`
- `createdAt`

`PersonBalance`

This can initially be computed from transactions instead of stored separately. Keeping it derived avoids balance drift.

## Parser Rules

The parser should be deterministic and offline-first.

Basic rules:

- Leading `+` means income.
- Known income words such as `salary`, `freelance`, `bonus`, `refund` imply income.
- `amount to Person` means lent/shared money to that person.
- `amount from Person` means money received from that person or borrowed from them, depending on final wording rules.
- Plain `amount category` means expense.
- Category is inferred from known keywords.
- Unknown category falls back to `other`.

The parser should return both the parsed result and a confidence/debug reason so tests can explain why a phrase was classified a certain way.

## Dashboard

The dashboard shows:

- Today spending
- This week spending
- Total income for the current month
- Total expense for the current month
- Top spending category

These values are calculated from local transactions through repository/domain functions.

## Notifications

Daily notification:

- Runs once per day.
- Example: `You spent ₹450 today. Most on food.`

Weekly notification:

- Runs once per week.
- Summarizes total spending, top category, and whether spending increased or decreased compared with the previous week.

WorkManager will schedule these jobs so they survive app restarts.

## Testing Strategy

Keep testing simple and high-value:

- Unit tests for parser rules.
- Unit tests for dashboard summary calculations.
- DAO tests only if Room behavior becomes non-trivial.
- Manual app run through Android Studio for UI and voice checks.

Parser tests are the most important because they protect the core promise of logging in seconds.

## Running And Changing

The project should use a normal Android Gradle layout so it can be opened directly in Android Studio.

Expected developer flow:

1. Open project in Android Studio.
2. Sync Gradle.
3. Run the app on emulator or device.
4. Run unit tests from Android Studio or Gradle.

No backend services, API keys, or custom local infrastructure should be required for the first version.
