# LLM Integration Safety Rules

## Hard constraints

- `llm-hosting` namespace is read-only from this project perspective.
- Allowed actions in `llm-hosting`:
  - discover existing Service/Route URLs
  - send inference HTTP requests to exposed endpoints
- Forbidden actions in `llm-hosting`:
  - create/update/delete resources
  - restart/scale workloads
  - relabel/reconfigure existing objects

## Recommended backend behavior

- Keep LLM interaction server-side only (never from plugin).
- Ask LLM for strict JSON output.
- Validate JSON schema before applying GitHub updates.
- Normalize status names to existing project options.
- Reject ambiguous updates with clear user feedback.
