package fu.ats.client.candidate;

import feign.Response;
import feign.codec.ErrorDecoder;
import fu.ats.exception.DownstreamException;
import fu.ats.exception.DownstreamUnavailableException;

public class CandidateErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = "candidate-service returned HTTP " + response.status();

        if (response.status() == 404) {
            return new DownstreamException("candidate-service", 404,
                    "CANDIDATE_NOT_FOUND", message);
        }

        if (response.status() >= 500) {
            return new DownstreamUnavailableException("candidate-service", message);
        }

        return defaultDecoder.decode(methodKey, response);
    }
}
