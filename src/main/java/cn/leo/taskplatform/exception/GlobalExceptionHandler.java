package cn.leo.taskplatform.exception;

import cn.leo.taskplatform.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBizException(BizException ex) {
        log.warn("biz exception code={} message={}", ex.getCode(), ex.getMessage());
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(Exception ex) {
        log.warn("validation exception", ex);
        return ApiResponse.fail("PARAM_400", "请求参数不合法");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("unexpected system exception", ex);
        return ApiResponse.fail("SYSTEM_ERROR", "系统异常");
    }
}
