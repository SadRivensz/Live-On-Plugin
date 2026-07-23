# Contributing

[English](CONTRIBUTING.md) | [Brazilian Portuguese](CONTRIBUTING.pt-BR.md)

1. Each class should have one clear responsibility.
2. RuneLite events must not perform blocking I/O.
3. HTTP requests go through `LiveOnApiClient`; SQL goes through `Database`.
4. Never add secrets, webhooks or `.env` files to Git.
5. Use `gameval` constants when fixed game IDs are required.
6. Add tests for authorization rules and data transformations.
7. Run `gradlew test` and the backend tests before opening a pull request.

Formatting and naming should optimize for readability. Comments explain
decisions; they do not repeat what the code already says.
