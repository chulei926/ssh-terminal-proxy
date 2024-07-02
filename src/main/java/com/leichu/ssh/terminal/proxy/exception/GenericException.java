package com.leichu.ssh.terminal.proxy.exception;

/**
 * 设备交互自定义异常的基类.
 *
 * @author leichu 2022-08-15.
 */
public class GenericException extends RuntimeException {

	protected Integer code;

	public GenericException() {
		super();
	}

	public GenericException(String message) {
		super(message);
	}

	public GenericException(String message, Throwable cause) {
		super(message, cause);
	}

	public GenericException(Integer code, String message) {
		super(message);
		this.code = code;
	}

	public GenericException(Integer code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
}
