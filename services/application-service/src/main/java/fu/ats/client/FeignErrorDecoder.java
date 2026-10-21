package fu.ats.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import fu.ats.exception.DownstreamException;
import fu.ats.exception.DownstreamUnavailableException;

public class FeignErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = "Downstream request failed: " + methodKey
                + " [HTTP " + response.status() + "]";

        if (response.status() >= 500) {
            return new DownstreamUnavailableException("downstream", message);
        }

        if (response.status() == 404) {
            return new DownstreamException("downstream",  404, "RESOURCE_NOT_FOUND", message);
        }

        if (response.status() == 409) {
            return new DownstreamException("downstream", 409, "CONFLICT", message
            );
        }

        return defaultDecoder.decode(methodKey, response);
    }
}
