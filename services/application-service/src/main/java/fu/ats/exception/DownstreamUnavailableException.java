package fu.ats.exception;

public class DownstreamUnavailableException extends DownstreamException {
    public DownstreamUnavailableException(String service, String message) {
        super(service, 503, "DOWNSTREAM_UNAVAILABLE", message);
    }
}