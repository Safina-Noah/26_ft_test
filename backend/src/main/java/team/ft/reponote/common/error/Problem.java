package team.ft.reponote.common.error;

import org.springframework.http.HttpStatus;

public class Problem extends RuntimeException {
  public final HttpStatus status;
  public final String code;
  public final boolean retryable;

  public Problem(HttpStatus status, String code, String message, boolean retryable) {
    super(message);
    this.status = status;
    this.code = code;
    this.retryable = retryable;
  }

  public static Problem missing() {
    return new Problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "데이터를 찾을 수 없습니다.", false);
  }

  public static Problem invalid(String message) {
    return new Problem(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message, false);
  }
}
