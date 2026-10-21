package fu.ats.client.job;

import feign.Response;
import feign.codec.ErrorDecoder;
import fu.ats.exception.DownstreamUnavailableException;
import fu.ats.exception.JobClosedException;
import fu.ats.exception.JobNotFoundException;

public class JobErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = "job-service returned HTTP " + response.status();

        return switch (response.status()) {
            case 404 -> new JobNotFoundException(message);
            case 409 -> new JobClosedException(message);
            case 500, 502, 503, 504 -> new DownstreamUnavailableException("job-service", message);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
}