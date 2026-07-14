package my.hive.shared.context;

/**
 * Allows business code to suppress the current @CollectLog record after it has
 * confirmed that the request only performed a non-audit correction.
 */
public final class OperationLogSkipContext {

    private static final ThreadLocal<Boolean> SKIP_CURRENT = ThreadLocal.withInitial(() -> false);

    private OperationLogSkipContext() {
    }

    public static void skipCurrent() {
        SKIP_CURRENT.set(true);
    }

    public static boolean shouldSkip() {
        return Boolean.TRUE.equals(SKIP_CURRENT.get());
    }

    public static void clear() {
        SKIP_CURRENT.remove();
    }
}
