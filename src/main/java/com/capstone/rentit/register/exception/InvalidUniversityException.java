package com.capstone.rentit.register.exception;

public class InvalidUniversityException extends RuntimeException {
  public InvalidUniversityException() {
    super("유효하지 않은 대학명 또는 상위 150개 대학에 포함되지 않습니다.");
  }
}
