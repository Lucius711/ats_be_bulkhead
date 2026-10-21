package fu.ats.exception;

public class JobClosedException extends DownstreamException {
    public JobClosedException(String message) {
        super("job-service", 409, "JOB_CLOSED", message);
    }
}