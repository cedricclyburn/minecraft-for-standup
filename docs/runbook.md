# Runbook

## Verify backend health

```bash
BACKEND_URL=$(oc get route minecraft-backend -n minecraft -o jsonpath='{.spec.host}')
curl -s "https://${BACKEND_URL}/health" | jq
```

## Verify authenticated backend calls

```bash
curl -s -H "X-API-Key: ${MINECRAFT_API_KEY}" "https://${BACKEND_URL}/jira/help" | jq
curl -s -H "X-API-Key: ${MINECRAFT_API_KEY}" "https://${BACKEND_URL}/jira/status?item=PVTI_example" | jq
curl -s -X POST -H "Content-Type: application/json" -H "X-API-Key: ${MINECRAFT_API_KEY}" \
  -d '{"item":"PVTI_example","newStatus":"In Progress","actor":"runbook"}' \
  "https://${BACKEND_URL}/jira/update" | jq
```

## Test natural-language update request

```bash
curl -s -X POST -H "Content-Type: application/json" -H "X-API-Key: ${MINECRAFT_API_KEY}" \
  -d '{"text":"Move PVTI_abc123 back to backlog because tokenizer bug is unresolved","actor":"runbook"}' \
  "https://${BACKEND_URL}/jira/update" | jq
```

## Mirror source material from Podman Desktop project #4

```bash
./scripts/mirror_podman_project.sh
```

Generated files:

- `source-material/podman-desktop-project-4/project-raw.json`
- `source-material/podman-desktop-project-4/project-derived.json`
- `source-material/podman-desktop-project-4/summary.md`
