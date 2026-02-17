# Minecraft Stand-up (PaperMC + OpenShift + Quarkus)

Proof-of-concept system where Minecraft players run `/jira` commands in chat and a backend updates GitHub Projects v2.

## Components

- `paper-plugin/`: PaperMC plugin implementing `/jira help|status|update|standup`
- `backend-quarkus/`: Quarkus backend with API key auth, GitHub GraphQL integration, LLM-intent stub
- `deploy/openshift/`: namespace-scoped manifests for `minecraft`
- `scripts/`: metadata mirroring script for podman-desktop Project #4
- `source-material/`: read-only mirrored project metadata
- `docs/`: setup, runbook, troubleshooting, safety notes

## Guardrails

- All new OpenShift resources belong in namespace `minecraft`.
- Existing `llm-hosting` namespace is read-only for endpoint discovery only.
- No secrets in git. Use OpenShift Secrets or local `.env` values.

## Quick links

- Setup: `docs/setup.md`
- Runbook: `docs/runbook.md`
- Troubleshooting: `docs/troubleshooting.md`
- LLM safety constraints: `docs/llm-safety.md`
