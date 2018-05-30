package org.coding.git.exceptions;

/**
 * 两步验证异常定义
 * @author robin
 */
public class CodingNetTwoFactorAuthenticationException extends CodingNetAuthenticationException {
  public CodingNetTwoFactorAuthenticationException() {
    super();
  }

  public CodingNetTwoFactorAuthenticationException(String message) {
    super(message);
  }

  public CodingNetTwoFactorAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }

  public CodingNetTwoFactorAuthenticationException(Throwable cause) {
    super(cause);
  }
}
