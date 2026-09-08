# Recommended repository settings

Several GitHub controls cannot be expressed in committed files. A repository
administrator should apply this checklist before the first public release.

## Features and security

- Enable Issues and Discussions.
- Enable private vulnerability reporting, Dependabot alerts, dependency graph,
  and secret scanning with push protection where the plan supports them.
- Restrict Actions to trusted actions and require approval for first-time
  outside contributors.
- Keep workflow token permissions read-only by default.

## `master` ruleset

- Require pull requests and at least one approving review.
- Dismiss stale approvals and require all conversations to be resolved.
- Require the contracts, Python, Java, and .NET CI checks.
- Require the branch to be current before merge.
- Block force pushes and branch deletion.
- Allow bypass only for a small maintainer or incident-response team.

## Protected live environment

Create an environment named `dataconnect-live`, require a maintainer review,
and limit deployment branches to `master`. Add these environment secrets:

- `DATACONNECT_ORG_EXTERNAL_ID`
- `DATACONNECT_CLIENT_EXTERNAL_ID`
- `DATACONNECT_USER_EXTERNAL_ID`
- `DATACONNECT_ORG_API_KEY`
- `DATACONNECT_REST_BASE_URL`
- `DATACONNECT_MCP_URL`

The manual live-smoke workflow generates exactly three rows. Review available
quota before approval and rotate any credential that appears in a workflow log.
