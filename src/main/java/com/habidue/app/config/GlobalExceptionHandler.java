package com.habidue.app.config;

import com.habidue.app.dto.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid 검증 실패 시
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("handleMethodArgumentNotValidException", e);
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.error(HttpStatus.BAD_REQUEST, message, e.getClass().getSimpleName());
    }

    // 데이터 중복 (예: 링크 중복, 키워드 중복 등)
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("handleIllegalArgumentException", e);
        return ApiResponse.error(HttpStatus.CONFLICT, e.getMessage(), e.getClass().getSimpleName());
    }

    // 상태 오류 (예: 차단됨, 발송 횟수 초과 등)
    @ExceptionHandler(IllegalStateException.class)
    protected ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException e) {
        log.error("handleIllegalStateException", e);
        return ApiResponse.error(HttpStatus.BAD_REQUEST, e.getMessage(), e.getClass().getSimpleName());
    }

    // 리소스(데이터)를 찾을 수 없을 때
    @ExceptionHandler(NoSuchElementException.class)
    protected ResponseEntity<ApiResponse<Void>> handleNoSuchElementException(NoSuchElementException e) {
        log.error("handleNoSuchElementException", e);
        return ApiResponse.error(HttpStatus.NOT_FOUND, e.getMessage(), e.getClass().getSimpleName());
    }

    // 인증 실패 (토큰 유효하지 않음, 비밀번호 불일치 등)
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    protected ResponseEntity<ApiResponse<Void>> handleAuthenticationException(RuntimeException e) {
        log.error("handleAuthenticationException", e);
        return ApiResponse.error(HttpStatus.UNAUTHORIZED, e.getMessage(), e.getClass().getSimpleName());
    }
    
    // JWT 만료 예외 처리
    @ExceptionHandler(ExpiredJwtException.class)
    protected ResponseEntity<ApiResponse<Void>> handleExpiredJwtException(ExpiredJwtException e) {
        log.error("handleExpiredJwtException", e);
        return ApiResponse.error(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.", e.getClass().getSimpleName());
    }

    // 접근 권한 없음
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.error("handleAccessDeniedException", e);
        return ApiResponse.error(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", e.getClass().getSimpleName());
    }

    // 파일 크기 초과
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("handleMaxUploadSizeExceededException", e);
        return ApiResponse.error(HttpStatus.BAD_REQUEST, "파일 크기가 너무 큽니다. 최대 50MB까지 업로드 가능합니다.", e.getClass().getSimpleName());
    }

    // 그 외 모든 예외
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("handleException", e);
        String errorName = (e != null) ? e.getClass().getSimpleName() : "UnknownException";
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다.", errorName);
    }
}
