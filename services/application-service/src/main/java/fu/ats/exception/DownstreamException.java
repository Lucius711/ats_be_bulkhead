package fu.ats.exception;

import lombok.Getter;

@Getter
public class DownstreamException extends RuntimeException {
    private final String service;
    private final int status;
    private final String errorCode;

    public DownstreamException(String service, int status, String errorCode, String message) {
        super(message);
        this.service = service;
        this.status = status;
        this.errorCode = errorCode;
    }
}