package my.hive.shared.context;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TenantPermissionContext {

    private static final ThreadLocal<ConcurrentHashMap<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    private static final String KEY_TENANT_CODE = "tenantCode";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_PERM_CODES = "permCodes";
    private static final String KEY_IGNORE_TENANT = "ignoreTenant";
    private static final String DENY_PREFIX = "!";

    public static void init(String tenantCode, Long userId, Set<String> permCodes) {
        ConcurrentHashMap<String, Object> context = new ConcurrentHashMap<>();
        if (tenantCode != null && !tenantCode.isBlank()) {
            context.put(KEY_TENANT_CODE, tenantCode);
        }
        if (userId != null) {
            context.put(KEY_USER_ID, userId);
        }
        context.put(KEY_PERM_CODES, permCodes == null ? Collections.emptySet() : permCodes);
        context.put(KEY_IGNORE_TENANT, false);
        THREAD_LOCAL.set(context);
    }

    public static void setIgnoreTenant(boolean ignoreTenant) {
        ConcurrentHashMap<String, Object> context = THREAD_LOCAL.get();
        if (context == null) {
            context = new ConcurrentHashMap<>();
            THREAD_LOCAL.set(context);
        }
        context.put(KEY_IGNORE_TENANT, ignoreTenant);
    }

    public static boolean isIgnoreTenant() {
        ConcurrentHashMap<String, Object> context = THREAD_LOCAL.get();
        if (context == null) {
            return false;
        }
        Object value = context.get(KEY_IGNORE_TENANT);
        return value instanceof Boolean && (Boolean) value;
    }

    public static void clearIgnore() {
        ConcurrentHashMap<String, Object> context = THREAD_LOCAL.get();
        if (context == null) {
            return;
        }
        context.remove(KEY_IGNORE_TENANT);
        if (context.isEmpty()) {
            THREAD_LOCAL.remove();
        }
    }

    public static boolean hasPermission(String permCode) {
        if (permCode == null || permCode.trim().isEmpty()) {
            return false;
        }

        ConcurrentHashMap<String, Object> context = THREAD_LOCAL.get();
        if (context == null) {
            return false;
        }

        Set<String> permCodes = getPermCodes();
        if (permCodes.isEmpty()) {
            return false;
        }

        String normalizedPermCode = permCode.trim();
        return !permCodes.contains(DENY_PREFIX + normalizedPermCode)
                && permCodes.contains(normalizedPermCode);
    }

    public static String getTenantCode() {
        ConcurrentHashMap<String, Object> context = THREAD_LOCAL.get();
        return context == null ? null : (String) context.get(KEY_TENANT_CODE);
    }

    public static Long getUserId() {
        ConcurrentHashMap<String, Object> context = THREAD_LOCAL.get();
        return context == null ? null : (Long) context.get(KEY_USER_ID);
    }

    @SuppressWarnings("unchecked")
    public static Set<String> getPermCodes() {
        ConcurrentHashMap<String, Object> context = THREAD_LOCAL.get();
        if (context == null) {
            return Collections.emptySet();
        }
        Set<String> permCodes = (Set<String>) context.get(KEY_PERM_CODES);
        return permCodes == null ? Collections.emptySet() : permCodes;
    }

    public static void clear() {
        THREAD_LOCAL.remove();
    }
}
