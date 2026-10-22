package fu.ats.client.cvparse;

import feign.Response;
import feign.codec.ErrorDecoder;
import fu.ats.exception.DownstreamUnavailableException;

public class CvParseErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = "cv-parse-service returned HTTP " + response.status();

        // 503: cv-parse-service tự từ chối (Semaphore bulkhead phía cv-parse đầy) hoặc đang lỗi
        if (response.status() >= 500) {
            return new DownstreamUnavailableException("cv-parse-service", message);
        }

        return defaultDecoder.decode(methodKey, response);
    }
}
