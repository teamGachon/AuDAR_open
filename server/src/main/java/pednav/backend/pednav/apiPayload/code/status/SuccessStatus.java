package pednav.backend.pednav.apiPayload.code.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import pednav.backend.pednav.apiPayload.code.BaseCode;
import pednav.backend.pednav.apiPayload.code.ReasonDTO;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {
    _OK(HttpStatus.OK, "OK200", "성공"),
    NOTIFICATION_SUCCESS(HttpStatus.OK, "NOTI2001", "알람 전송 성공"),
    FIND_LOCATION_SUCCESS(HttpStatus.OK, "FIND2001", "위치 테이블 불러오기"),
    GET_ACCIDENT_SUCCESS(HttpStatus.OK, "ACCI2001", "사고 불러오기");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }

    @Override
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}
