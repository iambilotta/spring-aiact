# Requirements — spring-aiact-sample, grouped by User Story

Auto-generated companion to `requirements.md`. Tests link to a User Story via the javadoc tag `@spec.us=US-NNN-slug` (the slug points to a User Story defined in `PRODUCT.md`). Implementation-detail tests with no `@spec.us` are collected at the bottom; declared User Stories in PRODUCT.md with zero linked tests are listed as **not implemented yet**.

## Coverage

- Total tests scanned: **5**
- Tests linked to a User Story: **0**
- Tests without `@spec.us` (implementation detail): **5**
- User Stories declared in PRODUCT.md: **0**
- User Stories with at least one linked test: **0**
- User Stories declared but **not yet implemented**: **0**

## Implementation detail (no `@spec.us` link)

These tests are valid requirements but exist below the user-story horizon (unit-level mechanism, internal invariant, white-box assertion). Add `@spec.us` if a user story should claim them.

### Module `(root)`

- `FR-(root).EndpointDenyByDefault#exportEndpointReturns403WhenNoGuardConfigured`
- `FR-(root).EndpointDenyByDefault#headEndpointReturns403WhenNoGuardConfigured`
- `FR-(root).EndpointDenyByDefault#verifyEndpointReturns403WhenNoGuardConfigured`
- `FR-(root).HiringScreenerIntegration#exportEndpointReturnsNdjsonAndVerifyEndpointIsOk`
- `FR-(root).HiringScreenerIntegration#scoringProducesArticle12RecordWithValidHmacChain`
