package fu.ats.entity;


/**
 * Maps cvs.parse_status (varchar(50)), stored as STRING via @Enumerated.
 */
public enum ParseStatus {
    PENDING,
    PARSED,
    FAILED
}