package my.management.common.context;

import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TenantPermissionContext {

    private static final ThreadLocal<ConcurrentHashMap<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    private static final String KEY_TENANT_CODE = "tenantCode";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_PERM_CODES = "permCodes";

    public static void init(String tenantCode, Long userId, Set<String> permCodes) {
        ConcurrentHashMap<String, Object> context = new ConcurrentHashMap<>();
        context.put(KEY_TENANT_CODE, tenantCode);
        context.put(KEY_USER_ID, userId);
        context.put(KEY_PERM_CODES, permCodes == null ? Collections.emptySet() : permCodes);
        THREAD_LOCAL.set(context);
    }

    public static boolean hasPermission(String permCode) {
        if (permCode == null || permCode.trim().isEmpty()) {
            return false;
        }

        ConcurrentHashMap<String, Object> context = THREAD_LOCAL.get();
        if (context == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Set<String> permCodes = (Set<String>) context.get(KEY_PERM_CODES);
        if (CollectionUtils.isEmpty(permCodes)) {
            return false;
        }

        if (permCodes.contains("*") || permCodes.contains("*:*")) {
            return true;
        }
        if (permCodes.contains(permCode)) {
            return true;
        }

        int lastColonIndex = permCode.lastIndexOf(":");
        while (lastColonIndex > 0) {
            String prefix = permCode.substring(0, lastColonIndex);
            if (permCodes.contains(prefix + ":*")) {
                return true;
            }
            lastColonIndex = prefix.lastIndexOf(":");
        }

        return false;
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