package fu.ats.bulkhead;

/** Tên các bulkhead, khớp với resilience4j.*.instances trong config-repo/cv-parse-service.yml. */
public final class BulkheadNames {

    private BulkheadNames() {
    }

    /** ThreadPool bulkhead cho parse CV hàng loạt (bất đồng bộ, từ RabbitMQ). */
    public static final String BATCH = "cvParseBatch";

    /** Semaphore bulkhead cho API đồng bộ của HR (analyze/match). */
    public static final String INTERACTIVE = "cvParseInteractive";
}
