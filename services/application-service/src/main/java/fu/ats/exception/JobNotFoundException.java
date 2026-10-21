package fu.ats.exception;

public class JobNotFoundException extends DownstreamException {
    public JobNotFoundException(String message) {
        super("job-service", 404, "JOB_NOT_FOUND", message);
    }
}