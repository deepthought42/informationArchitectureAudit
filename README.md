# Information Architecture Accessibility Audit

A Spring Boot service that runs page-level information architecture and accessibility audits (WCAG-oriented checks) against stored `PageState` data and emits completion updates.

## What this service does

When a Pub/Sub message is received, the service:

1. Decodes a `PageAuditMessage` payload.
2. Loads the corresponding `AuditRecord` and `PageState`.
3. Executes a set of IA/accessibility audits (headers, tables, forms, language, links, metadata, etc.).
4. Persists generated `Audit` records.
5. Publishes an `AuditProgressUpdate` marking completion.

Primary entrypoint: `AuditController` (`POST /`).

## Local development

### Prerequisites

- Java 17+
- Maven 3.9+
- Access to Maven Central (or an internal mirror/proxy)

### Run tests

```bash
mvn test
```

> Note: In restricted environments, dependency resolution can fail (e.g., HTTP 403 from Maven Central).

### Run the app

```bash
mvn spring-boot:run
```

## Configuration

Main configuration files:

- `src/main/resources/application.properties`
- `src/main/resources/application.yml`

Typical values to configure:

- `server.port`
- datasource settings (`spring.datasource.*`)
- logging levels (`logging.level.*`)

## Code review summary (this pass)

This repository review focused on correctness, resilience, and audit metadata accuracy.

### Fixed in this update

1. **Message parsing hardening in controller**
   - Added request validation for missing `body.message` and missing/empty `message.data`.
   - Replaced unsafe `.get()` on optional audit record lookup with explicit `orElseThrow(...)`.

2. **Header audit logic bug**
   - Fixed boolean branch logic that used assignment (`=`) instead of comparison, which could force incorrect branches.

3. **Incorrect audit identity metadata**
   - `HeaderStructureAudit`, `TableStructureAudit`, and `FormStructureAudit` were persisting as `AuditName.LINKS` under navigation.
   - Updated to use structure-appropriate `AuditSubcategory` and `AuditName` values.

4. **Form audit target element bug**
   - `FormStructureAudit` was parsing `<table>` elements instead of `<form>` elements.
   - Updated to audit actual form elements.

5. **Logger category cleanup**
   - Fixed logger declarations in structure audits to use their own class instead of `LinksAudit.class`.

### Remaining high-value issues to address (proposed fixes)

1. **Overly broad / misleading class comments and descriptions**
   - Several audit classes still contain “links” copy in JavaDocs and descriptions.
   - **Proposed fix:** normalize class-level docs and user-facing descriptions to match each audit’s purpose.

2. **Potential null handling for element lookup**
   - `elementStateService.findByPageAndCssSelector(...)` may return `null`; issue message creation may then fail depending on downstream contracts.
   - **Proposed fix:** null-check and fall back to page-level `UXIssueMessage` when element resolution fails.

3. **Header order analysis quality**
   - `mapHeadersByAncestor` currently groups headers by ancestor but does not explicitly validate heading level transitions (`h1 -> h2`, etc.) in a deterministic scoring model.
   - **Proposed fix:** add explicit sequence validation and severity weighting for skipped levels.

4. **Test coverage gaps for fixed behavior**
   - Existing tests validate header mapping utility logic but not controller message validation paths.
   - **Proposed fix:** add controller slice tests (`@WebMvcTest`) for invalid message payloads and missing audit records.

## Deployment to GCP (Cloud Run)

1. Build container:

```bash
docker build -t gcr.io/[PROJECT-ID]/ia-accessibility-audit:v1 .
```

2. Push image:

```bash
docker push gcr.io/[PROJECT-ID]/ia-accessibility-audit:v1
```

3. Deploy:

```bash
gcloud run deploy ia-accessibility-audit \
  --image gcr.io/[PROJECT-ID]/ia-accessibility-audit:v1 \
  --platform managed \
  --region [REGION] \
  --allow-unauthenticated
```

Replace `[PROJECT-ID]` and `[REGION]` with your values.

## Contributing

Please follow `CONTRIBUTING.md` and include:

- tests for behavior changes
- clear commit messages
- updated documentation when audit behavior changes

## License

See `LICENSE`.
