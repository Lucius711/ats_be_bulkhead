package fu.ats.entity;

public enum CvParseStatus {
    /** Đã gửi đi parse hoặc chưa parse được (bulkhead đầy / cv-parse bận) -> chờ parse lại sau */
    PENDING,
    PARSED,
    FAILED
}
