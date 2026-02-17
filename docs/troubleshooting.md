# Troubleshooting

## `401 Unauthorized` from backend

- Confirm plugin API key matches `MINECRAFT_API_KEY` in secret.
- Confirm plugin sends header `X-API-Key`.

## `Status option not found in project`

- Verify your GitHub Project has a `Status` single-select field.
- Verify status names are available as exact options (e.g. `Backlog`, `In Progress`, `Done`).

## `Unsupported item reference`

- Current POC supports:
  - `PVTI_...` project item IDs
  - `#<issueNumber>` when issue is in configured `github.repository` and in first 100 project items.

## Paper plugin can’t reach backend route

- Check route host and TLS from Paper host.
- If using local Paper, test with `curl` from same machine.

## OpenShift NodePort not reachable

- Cluster/firewall may block node ports.
- Use `LoadBalancer` service if available.

## Do not disturb `llm-hosting`

- Discovery only:
  - `oc get svc -n llm-hosting`
  - `oc get route -n llm-hosting`
- No apply/delete/restart/scale/label commands in that namespace.
