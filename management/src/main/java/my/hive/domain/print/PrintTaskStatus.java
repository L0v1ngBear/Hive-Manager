package my.hive.domain.print;

/**
 * 打印任务状态。
 */
public final class PrintTaskStatus {

    private PrintTaskStatus() {
    }

    /** 待打印 */
    public static final int PENDING = 0;

    /** 已打印 */
    public static final int SUCCESS = 1;

    /** 打印失败 */
    public static final int FAILED = 2;

    /** 已作废 */
    public static final int CANCELED = 3;
}
