# Kuaidi100 Order Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add on-hover live logistics tracking to the management order list using only Kuaidi100, with a 30-minute server-side cache.

**Architecture:** The browser sends only the order ID when the logistics-number popover opens. The unified backend reuses order-detail permission and stage-scope checks, loads logistics fields from the tenant-scoped order, maps the stored company name to a Kuaidi100 company code, signs and sends the official form request, normalizes the response, and caches the normalized response through the existing external API guard. There is no legacy provider, client-side direct call, or fallback query.

**Tech Stack:** Java 21, Spring Boot 3.1, Java `HttpClient`, Fastjson2, Redis, Vue 3, Element Plus, Node test runner, JUnit 5, Mockito.

## Global Constraints

- Query Kuaidi100 only after the user hovers over a non-empty logistics number.
- Cache successful provider responses for exactly 30 minutes.
- Use `POST https://poll.kuaidi100.com/poll/query.do` with `application/x-www-form-urlencoded`.
- Sign as uppercase MD5 of `param + key + customer`.
- Keep `key`, `customer`, phone numbers, and raw provider bodies out of browser responses and logs.
- Do not add legacy compatibility, alternate providers, or automatic courier-recognition APIs.
- Reuse `order:detail` and existing order stage/scope checks.

---

### Task 1: Kuaidi100 Client Contract

**Files:**
- Create: `management/src/main/java/my/hive/infrastructure/logistics/Kuaidi100Properties.java`
- Create: `management/src/main/java/my/hive/infrastructure/logistics/Kuaidi100Client.java`
- Create: `management/src/main/java/my/hive/domain/order/model/vo/OrderLogisticsTrackingVO.java`
- Test: `management/src/test/java/my/hive/infrastructure/logistics/Kuaidi100ClientTest.java`

**Interfaces:**
- Consumes: courier code, tracking number, optional phone.
- Produces: `OrderLogisticsTrackingVO query(String companyCode, String expressNo, String phone)`.

- [x] **Step 1: Write failing tests for signing, form encoding, response normalization, configuration rejection, and provider error mapping.**

```java
assertThat(client.sign(param)).isEqualTo(expectedUppercaseMd5);
assertThat(client.query("shunfeng", "SF123456", "13800000000").getTraces()).hasSize(1);
assertThatThrownBy(() -> disabledClient.query("shunfeng", "SF123456", null))
        .isInstanceOf(BusinessException.class).hasMessageContaining("未配置");
```

- [x] **Step 2: Run the focused test and confirm it fails because the client contract does not exist.**

Run: `./mvnw -Dtest=Kuaidi100ClientTest test`

- [x] **Step 3: Implement the properties, Java HTTP form request, MD5 signature, safe error mapping, and normalized VO.**

```java
String param = JSON.toJSONString(Map.of("com", companyCode, "num", expressNo,
        "phone", phone == null ? "" : phone, "resultv2", "4", "show", "0", "order", "desc"));
String sign = md5Uppercase(param + properties.getKey() + properties.getCustomer());
```

- [x] **Step 4: Run the focused client test and confirm it passes.**

Run: `./mvnw -Dtest=Kuaidi100ClientTest test`

### Task 2: Order Tracking Service and Protected Endpoint

**Files:**
- Create: `management/src/main/java/my/hive/domain/order/service/OrderLogisticsTrackingService.java`
- Modify: `management/src/main/java/my/hive/api/order/OrderController.java`
- Test: `management/src/test/java/my/hive/domain/order/service/OrderLogisticsTrackingServiceTest.java`
- Test: `management/src/test/java/my/hive/architecture/UniqueRuntimeComponentTest.java`

**Interfaces:**
- Consumes: order ID and current `TenantPermissionContext`.
- Produces: `GET /orders/{orderId}/logistics-tracking` returning `Result<OrderLogisticsTrackingVO>`.

- [x] **Step 1: Write failing tests for order lookup, detail-stage permission reuse, courier mapping, 30-minute cache, and missing logistics fields.**

```java
OrderLogisticsTrackingVO first = service.getTracking("SO-001");
OrderLogisticsTrackingVO second = service.getTracking("SO-001");
verify(client, times(1)).query("shunfeng", "SF123456", "13800000000");
assertThat(second.isCached()).isTrue();
```

- [x] **Step 2: Run the focused service test and confirm it fails.**

Run: `./mvnw -Dtest=OrderLogisticsTrackingServiceTest test`

- [x] **Step 3: Implement tenant/order scope validation, explicit courier aliases, cache lookup/write, and endpoint permission.**

```java
@GetMapping("/{orderId}/logistics-tracking")
@RequirePermission(value = PermissionCatalogV3.CODE_ORDER_DETAIL, message = "您没有权限查看订单物流")
public Result<OrderLogisticsTrackingVO> logisticsTracking(@PathVariable String orderId) {
    return Result.success(orderLogisticsTrackingService.getTracking(orderId));
}
```

- [x] **Step 4: Run focused backend tests and confirm they pass.**

Run: `./mvnw -Dtest=Kuaidi100ClientTest,OrderLogisticsTrackingServiceTest,UniqueRuntimeComponentTest test`

### Task 3: Hover-Only Order List Popover

**Files:**
- Modify: `management-ui/src/views/function/order/api/order.js`
- Modify: `management-ui/src/views/function/order/order.vue`
- Create: `management-ui/tests/order-logistics-tracking.test.js`

**Interfaces:**
- Consumes: `getOrderLogisticsTracking(orderId)` only from an Element Plus popover show event.
- Produces: loading, error, current status, update time, and trace timeline states keyed by order ID.

- [x] **Step 1: Write a failing static contract test proving there is no mount/list-load query and the popover show handler is the only trigger.**

```js
assert.match(orderSource, /@show="loadLogisticsTracking\(row\)"/)
assert.match(orderApiSource, /`\/orders\/\$\{encodeURIComponent\(orderId\)\}\/logistics-tracking`/)
assert.doesNotMatch(loadOrdersSource, /getOrderLogisticsTracking/)
```

- [x] **Step 2: Run the focused frontend test and confirm it fails.**

Run: `node --test tests/order-logistics-tracking.test.js`

- [x] **Step 3: Add the API function, hover popover, request de-duplication, and accessible loading/error/timeline presentation.**

```vue
<el-popover v-if="row.expressNo" trigger="hover" :show-after="250" @show="loadLogisticsTracking(row)">
  <template #reference><button type="button" class="order-express-number-trigger">{{ row.expressNo }}</button></template>
  <div v-loading="trackingState(row).loading">...</div>
</el-popover>
```

- [x] **Step 4: Run the focused UI test and build.**

Run: `node --test tests/order-logistics-tracking.test.js`

Run: `npm run build`

### Task 4: Deployment Configuration and Documentation

**Files:**
- Modify: `management/src/main/resources/application.yaml`
- Modify: `deploy/docker-compose.yml`
- Modify: `deploy/.env.example`
- Modify: `docs/architecture/2026-07-13-hive-system-logic-chain-map.md`
- Test: `management-ui/tests/deploy-secret-hardening.test.js`

**Interfaces:**
- Consumes: `KUAIDI100_ENABLED`, `KUAIDI100_KEY`, `KUAIDI100_CUSTOMER`, and timeout settings. The provider URL is fixed to Kuaidi100's official HTTPS endpoint.
- Produces: explicit production configuration without committed secrets.

- [x] **Step 1: Extend deployment tests to require environment mapping and forbid populated example secrets.**

```js
assert.match(composeSource, /KUAIDI100_KEY: \$\{KUAIDI100_KEY:-\}/)
assert.match(envExample, /^KUAIDI100_KEY=$/m)
```

- [x] **Step 2: Run the deployment test and confirm it fails.**

Run: `node --test tests/deploy-secret-hardening.test.js`

- [x] **Step 3: Add configuration mappings and document the endpoint, cache, permission, trigger, provider, and deployment variables.**

```yaml
logistics:
  kuaidi100:
    enabled: ${KUAIDI100_ENABLED:false}
    key: ${KUAIDI100_KEY:}
    customer: ${KUAIDI100_CUSTOMER:}
    cache-ttl: 30m
```

- [x] **Step 4: Run backend tests, all management UI tests, and the frontend production build.**

Run: `./mvnw test`

Run: `npm test`

Run: `npm run build`
