# Unified Employee Login Final Security Remediation Report

Date: 2026-07-19 (Asia/Shanghai)

## Outcome

The reviewed public employee-login findings are remediated on
`codex/unified-employee-login`. Backend, management UI, and mini-program full
test suites pass; both production builds pass; the fixed desktop release tree
has been refreshed and verified without remote deployment.

## Focused commits

- `ccc5ecf` — `fix: rate limit public authentication flows`
- `8ba6154` — `fix: consume organization invitations atomically`
- `b93bfe0` — `fix: preserve tenant reasons across invitation joins`
- `97cb45f` — `fix: audit unresolved employee login data`
- Mini repository `82bd200` — `fix: harden mini authentication inputs and logs`

Unrelated pre-existing `.superpowers/sdd` edits were preserved and never
staged. No merge, push, upload, or remote command was performed.

## TDD evidence

### Trusted IP and public rate limits

RED: the focused Maven command failed to compile because the resolver,
limiter, and IP-plumbed signatures did not exist.

GREEN:

```text
cd D:\HiveManager\management
.\mvnw.cmd -q '-Dtest=TrustedClientIpResolverTest,PublicAuthRateLimiterTest,UnifiedAuthenticationIntegrationTest,WechatTenantSelectionPublicPathIntegrationTest,AuthenticationServiceTest' test
exit=0 tests=38 failures=0 errors=0
```

### Atomic invitations and audit scope

RED: the focused suite failed to compile because the structured invitation
payload/service did not exist.

GREEN:

```text
.\mvnw.cmd -q '-Dtest=OrganizationInvitationServiceTest,OrganizationPositionServiceTest,SensitiveDataSanitizerTest,CommercialHardeningStaticTest,AuthenticationServiceTest' test
exit=0
```

The concurrency regression proves exactly one success across 24 concurrent
consumers for a one-use invitation.

### Tenant reasons and cross-tenant joining

RED: the first run failed to compile for the missing canonical selection
reason/mapper method; the next run exposed the real quota failure as
`(400, null)` instead of the typed tenant-license reason.

GREEN:

```text
.\mvnw.cmd -q '-Dtest=AuthenticationServiceTest,TenantLicenseServiceTest,OrganizationInvitationServiceTest,GlobalExceptionHandlerTest' test
exit=0
```

### Cleartext-safe backfill and release audit

RED: the mapper contract retained plaintext phone data, and the Node audit
contract lacked the required report categories.

GREEN:

```text
.\mvnw.cmd -q '-Dtest=AuthenticationServiceTest' test
tests=30 failures=0 errors=0

node --test tests/unified-employee-login-migration.test.js
tests=3 pass=3 fail=0
```

### Mini-program validation and safe logging

RED:

```text
node --test --test-name-pattern="join organization requires exactly six|join organization enforces backend password|join failure console" tests/wechat-employee-login-flow.test.js
tests=3 pass=0 fail=3

node --test --test-name-pattern="EMPLOYEE_NOT_FOUND opens|tenant selection backend rejection|tenant selection lost response|authentication pages never" tests/wechat-employee-login-flow.test.js
tests=4 pass=0 fail=4
```

GREEN:

```text
node --test tests/wechat-employee-login-flow.test.js
tests=18 pass=18 fail=0
```

## Security and data-integrity decisions

- Forwarded client IP headers are trusted only from configured proxy CIDRs;
  the default trusts loopback only. Chains are normalized right-to-left and
  malformed/untrusted input falls back to the socket peer.
- Redis limits use one atomic Lua increment/first-expiry operation, fail closed
  on Redis failure, and use independent IP plus phone/account/ticket
  dimensions. Every subject is SHA-256 fingerprinted before key construction.
- Invitations are Redis hashes containing tenant, issuer, expiry, and remaining
  uses. Consumption validates/decrements/deletes atomically; legacy string
  values are consumed once. Default lifetime is 15 minutes and default use
  count is one.
- Auth/organization operation logs are enabled without recording sensitive
  arguments/results; invitation, proof, SMS, password, phone, and selection
  fields are sanitized.
- Stable reviewed reasons are preserved: `INVITATION_INVALID_OR_EXPIRED`,
  `TENANT_SELECTION_INVALID_OR_EXPIRED`, `TENANT_UNAVAILABLE`,
  `TENANT_LICENSE_UNAVAILABLE`, employee/account state reasons, and phone
  ambiguity/activation reasons.
- Organization join candidates are restricted to the target tenant plus
  tenant-less legacy rows. An unrelated tenant membership creates a distinct
  target-tenant employee; one tenant-less row may be reused; a target duplicate
  blocks.
- Compatibility backfill atomically writes phone hash/mask and clears plaintext
  only while the original id/tenant/phone still match. Authentication proceeds
  only after the hash-only requery.
- The release audit is SELECT-only. Duplicate real-tenant phone hashes are the
  sole data blocker; tenant-less rows, missing extensions/roles, invalid
  statuses, and blank login-capable hashes are explicitly reported.
- Mini join accepts exactly six numeric SMS digits and an 8–64 character
  password containing at least one ASCII letter and digit. Auth-page console
  output is restricted to a constant label and safe `{code, reason}` object.

## Fresh complete verification

```text
Backend focused combined suite: exit=0
Backend .\mvnw.cmd test: tests=351 failures=0 errors=0 skipped=0
Backend .\mvnw.cmd -DskipTests package: BUILD SUCCESS
Management UI npm test: tests=312 pass=312 fail=0
Management UI npm run build: PASS, files=99
Mini node --test tests/*.test.js: tests=53 pass=53 fail=0
Dedicated migration/schema/audit gate: tests=5 pass=5 fail=0
CustomerFacingChineseMessageContractTest: exit=0
```

The mini repository has no `package.json`, so the plan's `npm test` command
correctly fails with `ENOENT`; its complete native Node test command was used.

## Release refresh and integrity

Target: `C:\Users\HUAWEI\Desktop\hive全新部署`

```text
BackendJarSha256=4f51410b4bab8228e504c568ccdfd4c2315e6096bdac1bf8b77e9ca07762818c
BackendJarBytes=103358279
BackendJars=1
RepositoryDeployStagedJars=0
ManagementUiSha256=6210e28f4c25b6be0fc74a1840cf53e1f36c3c575a0f98ccf634a9cbd038c2d5
ManagementUiFiles=99
MigrationFiles=79
MigrationChecksumEntries=81
RequiredReleaseEntries=14
ForbiddenPathsOrFiles=0
ReleaseOwnedSourceTrees=MATCH
ReleaseCommitMetadata=RESOLVED
```

The desktop JAR equals the Maven artifact and metadata. Every UI manifest and
migration checksum entry was re-hashed. Backend, migrations, UI, Nginx,
scripts, and root release files match their repository sources byte-for-byte.
The mini source package metadata covers 114 deployable source files,
1,165,917 bytes, with canonical manifest SHA-256
`42a3bc6ab504b010f7aed2da9e1c7a8abd3023ab9784e3c141c2372851555e68`.

Compiled-class scan:

```text
Account is disabled or unavailable=False
Chinese account/tenant/not-found messages=True
ACCOUNT_DISABLED=True
EMPLOYEE_NOT_FOUND=True
TENANT_SELECTION_INVALID_OR_EXPIRED=True
INVITATION_INVALID_OR_EXPIRED=True
TENANT_LICENSE_UNAVAILABLE=True
```

## Remaining operational gates

- Git Bash and Docker are unavailable locally. The equivalent Windows artifact
  checks passed, but `scripts/verify-release-integrity.sh`, upload-package Bash
  execution, Compose validation, and runtime smoke remain release-host gates.
- The read-only data audit was contract-tested but not run against a live
  production database; operators must run it with production credentials
  before migration and stop on duplicate real-tenant phone hashes.
- WeChat DevTools preview/upload was not run and remains recorded as
  `NOT_RUN_TASK9`.
- Existing legacy password/QR/generic validation exceptions outside the
  reviewed unified employee public-flow reason contract may still have a null
  machine reason; their messages remain covered by the Chinese-message gate.

Handoff remains local only: overwrite `/root/hive` with the fixed desktop tree,
then an authorized operator may run `cd /root/hive && bash publish.sh`.
