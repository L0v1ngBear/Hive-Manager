# Order Multi-Shipment Design

## Goal

Support partial shipment logistics on the management order page by allowing one order to own multiple independent logistics records. Each record can be added and updated, but a saved record cannot be deleted. The management UI queries Kuaidi100 only when the user hovers the corresponding tracking number.

## Scope

- Unified Spring Boot backend in `management/`.
- Management web order page in `management-ui/`.
- Database baseline and one appended versioned migration.
- Order operation-log documentation and release metadata/contracts.
- Server deployment package rebuilt from the final `main` commit.

The mini-program frontend is not changed in this iteration. Installation tasks keep their own logistics fields and no longer depend on a single order-level logistics value.

## Non-Goals

- A shipment does not track item lines, shipped quantity, remaining quantity, recipient, freight cost, or package weight.
- Saved shipment records cannot be deleted.
- No compatibility API, fallback mapping, delimiter format, or JSON-in-order-column format is provided for the retired `sales_order.express_company` and `sales_order.express_no` fields.
- Historical migration SQL files remain immutable.

## Data Model

Create `sales_order_shipment` as a tenant-scoped child table:

| Column | Type | Rule |
| --- | --- | --- |
| `id` | `bigint` | Auto-increment primary key. |
| `tenant_code` | `varchar(50)` | Required tenant boundary. |
| `order_id` | `varchar(50)` | Required parent sales order. |
| `logistics_company` | `varchar(100)` | Required after trimming. |
| `tracking_no` | `varchar(100)` | Required after trimming. |
| `sort_order` | `int` | Stable display order starting at zero. |
| `version` | `int` | Optimistic-lock version starting at zero. |
| `creator` | `varchar(64)` | User identifier that created the record. |
| `updater` | `varchar(64)` | Last modifying user identifier. |
| `updater_name` | `varchar(100)` | Last modifying display name. |
| `create_time` | `datetime` | Server creation time. |
| `update_time` | `datetime` | Server modification time. |

Indexes:

- Unique `(tenant_code, order_id, tracking_no)` to reject duplicate tracking numbers in one order.
- Ordered lookup `(tenant_code, order_id, sort_order, id)`.

The current schema baseline removes `express_company` and `express_no` from `sales_order` and adds the child table. A new migration performs the same forward schema change. Existing historical migrations are not edited.

## Backend Contract

### Save request

`SalesOrderSaveRequest` accepts:

```json
{
  "shipments": [
    {
      "id": null,
      "logisticsCompany": "顺丰速运",
      "trackingNo": "SF1234567890",
      "version": null
    }
  ]
}
```

Rules:

- Maximum 50 logistics records per order.
- Company and tracking number are both required when a row is present.
- Tracking numbers must be unique within the request and within the same tenant/order.
- An existing row ID must belong to the current tenant and order.
- Every saved row must remain in later full-order saves. Omitting an existing row is rejected as an attempted deletion.
- Updates use `id` and `version`; a stale version returns an optimistic-lock conflict.
- New records use `id = null` and `version = null`.
- Server assigns ordering, actor fields, and timestamps.

### Responses

Order list and detail responses expose `shipments` using:

```json
{
  "id": 1,
  "logisticsCompany": "顺丰速运",
  "trackingNo": "SF1234567890",
  "sortOrder": 0,
  "version": 0,
  "updaterName": "张三",
  "updateTime": "2026-07-17T17:00:00"
}
```

The list response includes the records needed to render the logistics-number column without another request.

### Status validation

Advancing or saving an order as `shipped` requires at least one complete logistics record. Later edits may append more logistics records to represent additional partial shipments.

### Tracking endpoint

Replace the order-wide endpoint with:

```text
GET /orders/{orderId}/shipments/{shipmentId}/logistics-tracking
```

The service validates tenant ownership and obtains company/tracking data only from the selected persisted shipment. Client-supplied tracking credentials are never accepted. Cache and external-call guards use tenant, order, and shipment identity so each tracking number has independent success and failure cooldown state.

### Operation logs

Order saves record concise logistics changes:

- `add_order_shipment` for newly persisted rows.
- `update_order_shipment` when company or tracking number changes.

Logs contain the shipment ID and masked/fingerprinted tracking identifier, not external integration credentials. Normal unchanged order saves do not create shipment-specific operation logs.

## Management UI

The order editor replaces the two scalar logistics inputs with a logistics-record section:

- `新增物流` appends an unsaved row.
- Unsaved rows may be discarded before saving.
- Persisted rows have no delete command.
- Persisted rows show last modifier and last modification time.
- Each row edits logistics company and tracking number.
- Existing order permissions continue to guard order editing; no new permission leaf is introduced.

The order list logistics column renders records in stable order:

- One record: display its tracking number.
- Multiple records: display each tracking number as an independent compact trigger and show the total batch count.
- Hovering one trigger opens only that record's tracking popover and starts only that record's request.
- Loading, error, cached result, latest state, and timeline are isolated per shipment.
- No logistics record: display `未填写物流单号`.

## Installation Task Boundary

Installation tasks retain their existing logistics company and number because those fields describe installation delivery handling. Order completion synchronization must not copy a retired scalar order logistics value. No multi-shipment installation UI is added.

## Error Handling

- Invalid or duplicate rows return a business validation error before any order mutation is committed.
- Missing existing IDs in a save request return `已保存的物流记录不允许删除`.
- Stale versions return a conflict explaining that logistics data has changed and must be refreshed.
- Tracking lookup for a shipment outside the order or tenant returns not found/forbidden without calling Kuaidi100.
- A provider failure affects only the selected shipment and uses the existing short failure cooldown.

## Testing

Backend tests cover:

- Add and update behavior.
- No-delete enforcement.
- Tenant and order ownership.
- Duplicate and maximum-count validation.
- Optimistic locking.
- Shipped-state minimum requirement.
- Shipment-specific tracking lookup and cache keying.

Management UI tests cover:

- Array payload and response mapping.
- Add/discard-unsaved/edit-persisted behavior.
- No delete control for persisted rows.
- Multiple list triggers.
- Hover-only request behavior and shipment-specific endpoint.
- Existing order permission guards.

Release gates cover:

- No active order contract references the retired scalar fields.
- Baseline and migration manifest/checksums contain the new table migration.
- Backend JAR, management UI tree, and release metadata are rebuilt from the final `main` commit.

## Acceptance Criteria

1. An order can persist at least two logistics records and return them in stable order.
2. Saved records can be modified but cannot be deleted through UI or API.
3. An order cannot enter `shipped` without at least one valid logistics record.
4. Every tracking number has its own hover popover, request, cache, and error state.
5. The active backend and management UI contain no scalar order logistics fallback.
6. Full backend and management UI tests pass before the branch is merged to `main`.
7. The final non-ZIP deployment directory is rebuilt from the merged `main` commit.
