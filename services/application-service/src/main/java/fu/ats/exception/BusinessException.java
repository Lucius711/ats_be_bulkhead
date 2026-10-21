package fu.ats.exception;

public class BusinessException extends RuntimeException {
    private int code;
    private  String message;
    public BusinessException() {
    }
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
