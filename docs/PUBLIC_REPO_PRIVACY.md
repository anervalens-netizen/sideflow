# Public Repository Privacy Boundary

SideFlow is developed in a public repository, but it is designed for personal use.

## Allowed in Git

- generic application source code;
- generic test fixtures;
- generic example section names;
- public package names when required by Android integration tests;
- MIT license and upstream attribution;
- build/CI configuration that contains no secrets.

## Never commit

- ChatGPT or other conversation/session URLs;
- account IDs, email addresses, phone numbers or authentication data;
- banking/account-specific deep-links;
- owner-specific exported SideFlow configuration;
- screenshots containing private content;
- API keys, tokens, cookies or credentials;
- Android keystores, signing passwords or release secret files;
- device dumps containing personal data.

## Runtime storage

User selections and shortcuts must be persisted in Android app-private storage. They are not source-controlled. Export/import is an explicit user action and exported files may contain private shortcut URLs; the UI must warn accordingly.

## CI/release signing

CI for pull requests and ordinary pushes builds unsigned/debug artifacts without release secrets. Release signing uses GitHub secrets or local external secrets and must never require a keystore to be present in the repository.
