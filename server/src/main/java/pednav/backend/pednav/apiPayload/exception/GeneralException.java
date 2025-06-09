package pednav.backend.pednav.apiPayload.exception;

import pednav.backend.pednav.apiPayload.code.BaseErrorCode;
import pednav.backend.pednav.apiPayload.code.ErrorReasonDTO;

public class GeneralException extends RuntimeException {
    private BaseErrorCode code;

    public GeneralException(BaseErrorCode code) {
        super(code.getReason().getMessage());
        this.code = code;
    }

    public ErrorReasonDTO getErrorReason() {
        return this.code.getReason();
    }

    public ErrorReasonDTO getErrorReasonHttpStatus(){
        return this.code.getReasonHttpStatus();
    }
}
