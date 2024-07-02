package com.leichu.ssh.terminal.proxy.exception;

/**
 * 登录/认证异常.
 *
 * @author leichu.
 * @since 2023-07-30.
 */
public class AuthException extends GenericException {

	private static final Integer CODE = 500;
	private static final String MSG_FORMAT = "Login failed! IP:%s Reason:%s";
	private final String neIp;
	private final String reason;

	public AuthException(String neIp, String reason) {
		super(CODE, String.format(MSG_FORMAT, neIp, reason));
		this.neIp = neIp;
		this.reason = reason;
	}

	public AuthException(String neIp, String reason, Throwable cause) {
		super(CODE, String.format(MSG_FORMAT, neIp, reason), cause);
		this.neIp = neIp;
		this.reason = reason;
	}

	public String getNeIp() {
		return neIp;
	}

	public String getReason() {
		return reason;
	}
}
