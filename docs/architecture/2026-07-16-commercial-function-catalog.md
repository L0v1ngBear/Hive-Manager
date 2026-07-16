# Hive commercial function catalog

## Product scope

Hive is a single-tenant-first enterprise workflow system. The public business model uses one order entry and one backend. Production records remain internal fulfillment data and are not exposed as a second order product.

## Retained modules

| Module | Commercial responsibility |
| --- | --- |
| Dashboard | Operational totals and work reminders; no AI advice |
| Orders | Unified creation, item details, notes, status flow, approval, warning, invoicing, logistics and flow-code printing |
| Installation | Delivery-to-installation tracking, installers, exception notes and attachments |
| Inventory | Stock, inbound/outbound records, model detail and warning settings |
| Quality | Quality records, handling, attachments and quality approval |
| Customers | Customers, contacts, projects and ownership |
| Pricing | SKU, tier and customer override pricing |
| Approval | Order, quality, finance, leave and resignation approval with configurable default auditors |
| Printing | Labels, receipts and order flow-code print tasks |
| Organization | Employee records and reporting lines; one organization-management page for department trees, department positions and members; built-in roles and per-user permission overrides |
| Attendance | Rules, locations, employee exemptions, records and statistics |
| Equipment | Equipment registry and inspection records |
| Announcements | Enterprise announcements and read receipts |
| Documents and manual | Tenant documents, custom fields and editable system manual |
| Operations | Tenant isolation, permission enforcement, order operation logs, migrations, backup, restore and smoke checks |

## Optional integrations

The following integrations remain disabled by default and are enabled only through production configuration:

- WeChat mini-program login and subscription messages
- Kuaidi100 logistics tracking
- Aliyun OSS
- SMS
- RabbitMQ
- XXL-JOB

The default low-cost topology uses an in-process synchronous database operation log and no external scheduler or queue.

## Removed functionality

- AI business advice pages, permissions, quotas, tables and manual cleanup entrypoints
- Public production-order and sales-order dual navigation; `/orders` is the only public order API
- Dual management/mini backend services and compatibility routes
- Automatic historical migration-file replacement
- Partial business-data reset scripts
- File-only rollback that could start old code against a forward-migrated database
- Test tenant and `TENANT_002` bootstrap behavior

Historical versioned migrations remain immutable for checksum evidence. Their presence does not make retired features active in a fresh database.

## Release rules

1. A fresh empty database is initialized only by `initialize-fresh-database.sh` with explicit confirmation.
2. Normal migration accepts only a managed database with non-empty successful migration history and no failed records.
3. Backend writes stop before migration. Migration failure leaves backend stopped.
4. MySQL and Redis are never recreated by a normal application release.
5. Release synchronization is allowlisted and preserves runtime secrets and persistent data.
6. Baseline, permission seed and every historical migration are protected by `migration_checksums.sha256`.
7. Database recovery must pass a shadow restore before online replacement.
