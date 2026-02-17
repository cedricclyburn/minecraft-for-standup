#!/usr/bin/env bash
set -euo pipefail

ORG="podman-desktop"
PROJECT_NUMBER="4"
OUTPUT_DIR="source-material/podman-desktop-project-4"
RAW_JSON="$OUTPUT_DIR/project-raw.json"
DERIVED_JSON="$OUTPUT_DIR/project-derived.json"
SUMMARY_MD="$OUTPUT_DIR/summary.md"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd gh
require_cmd jq

mkdir -p "$OUTPUT_DIR"

if ! gh auth status >/dev/null 2>&1; then
  echo "GitHub CLI is not authenticated. Run: gh auth login" >&2
  exit 1
fi

read -r -d '' QUERY <<'GRAPHQL' || true
query($org: String!, $number: Int!) {
  organization(login: $org) {
    projectV2(number: $number) {
      id
      title
      url
      public
      shortDescription
      fields(first: 50) {
        nodes {
          __typename
          ... on ProjectV2FieldCommon {
            name
            dataType
          }
          ... on ProjectV2SingleSelectField {
            options {
              name
            }
          }
        }
      }
      items(first: 100) {
        nodes {
          id
          isArchived
          content {
            __typename
            ... on Issue {
              number
              title
              url
              state
              repository {
                nameWithOwner
              }
              labels(first: 20) {
                nodes {
                  name
                }
              }
            }
            ... on PullRequest {
              number
              title
              url
              state
              repository {
                nameWithOwner
              }
            }
            ... on DraftIssue {
              title
            }
          }
          fieldValues(first: 20) {
            nodes {
              __typename
              ... on ProjectV2ItemFieldSingleSelectValue {
                name
                field {
                  ... on ProjectV2FieldCommon {
                    name
                  }
                }
              }
              ... on ProjectV2ItemFieldTextValue {
                text
                field {
                  ... on ProjectV2FieldCommon {
                    name
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
GRAPHQL

echo "Fetching Projects v2 metadata from ${ORG} project #${PROJECT_NUMBER} (read-only)..."
gh api graphql -f query="$QUERY" -F org="$ORG" -F number="$PROJECT_NUMBER" > "$RAW_JSON"

jq '{
  source: "https://github.com/orgs/podman-desktop/projects/4",
  fetchedAt: now | todate,
  project: {
    id: .data.organization.projectV2.id,
    title: .data.organization.projectV2.title,
    url: .data.organization.projectV2.url,
    public: .data.organization.projectV2.public,
    shortDescription: .data.organization.projectV2.shortDescription
  },
  fields: [
    .data.organization.projectV2.fields.nodes[]
    | {
        name: .name,
        dataType: .dataType,
        options: (.options // [] | map(.name))
      }
  ],
  items: [
    .data.organization.projectV2.items.nodes[]
    | {
        id: .id,
        isArchived: .isArchived,
        contentType: .content.__typename,
        repository: (.content.repository.nameWithOwner // null),
        number: (.content.number // null),
        title: (.content.title // "(draft issue)"),
        url: (.content.url // null),
        state: (.content.state // null),
        status: ([.fieldValues.nodes[]? | select(.field.name == "Status") | .name][0] // "UNKNOWN"),
        labels: (.content.labels.nodes // [] | map(.name))
      }
  ]
}' "$RAW_JSON" > "$DERIVED_JSON"

TOTAL_ITEMS=$(jq '.items | length' "$DERIVED_JSON")
OPEN_ISSUES=$(jq '[.items[] | select(.contentType == "Issue" and .state == "OPEN")] | length' "$DERIVED_JSON")

cat > "$SUMMARY_MD" <<MD
# Podman Desktop Project 4 Mirror (Read-Only)

- Source: https://github.com/orgs/podman-desktop/projects/4
- Fetched at: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
- Total mirrored items: ${TOTAL_ITEMS}
- Open issues mirrored: ${OPEN_ISSUES}

## Important

This is a local reference mirror used for POC scaffolding.
No mutations were sent to the upstream project.

## Generated files

- project-raw.json
- project-derived.json

Raw GraphQL output: $RAW_JSON
Derived metadata: $DERIVED_JSON
MD

echo "Mirror complete:"
echo "  - $RAW_JSON"
echo "  - $DERIVED_JSON"
echo "  - $SUMMARY_MD"
