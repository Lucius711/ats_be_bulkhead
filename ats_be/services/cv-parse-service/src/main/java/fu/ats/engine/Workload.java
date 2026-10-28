package fu.ats.engine;

/** Hai loại việc cùng dùng engine phân tích. */
public enum Workload {
    /** Parse CV hàng loạt từ RabbitMQ: bất đồng bộ, không ai đứng chờ. */
    BATCH,
    /** API đồng bộ cho HR (analyze/match): người dùng đang đứng chờ kết quả. */
    INTERACTIVE
}
