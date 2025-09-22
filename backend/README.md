# Backend: Smart Meeting Room System

This document explains how to enable LLM integration (OpenAI) and run the backend locally on Windows (PowerShell).

## Enable OpenAI LLM calls

Temporary (current PowerShell session):

```powershell
$env:OPENAI_API_KEY = 'sk-REPLACE_WITH_YOUR_KEY'
```

Persistent (user-level):

```powershell
setx OPENAI_API_KEY "sk-REPLACE_WITH_YOUR_KEY"
# Close and reopen PowerShell to pick up the value
```

Run the application with the variable set:

```powershell
$env:OPENAI_API_KEY='sk-...'; mvn spring-boot:run
```

To run tests with the key available:

```powershell
$env:OPENAI_API_KEY='sk-...'; mvn test
```

## Docker / Compose

Pass the env var into the container or use a `.env` file (gitignore it):

```yaml
services:
  backend:
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
```

## CI / GitHub Actions

Store `OPENAI_API_KEY` in CI secrets and map it into the workflow using `${{ secrets.OPENAI_API_KEY }}`.

## Local testing without real LLM

The codebase provides a `TestLlmConfig` (test source) that injects a mock `LlmClient` bean for Spring-powered tests. If you run unit tests that don't start a Spring context, no real LLM calls will be made by default.

## Security

- Never commit secrets to the repository.
- Use a secrets manager or Kubernetes Secrets for production.
- Rotate API keys periodically and monitor usage.
