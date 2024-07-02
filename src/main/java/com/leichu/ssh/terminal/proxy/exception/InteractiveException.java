package com.leichu.ssh.terminal.proxy.exception;

/**
 * 设备交互异常.
 *
 * @author leichu.
 * @since 2023-07-30.
 */
public class InteractiveException extends GenericException {

	private static final Integer CODE = 500;
	private static final String MSG_FORMAT = "Interactive error! IP:%s Message:%s";
	private String neIp;
	private String errMsg;

	public InteractiveException() {
		super(CODE, "Interactive error!");
	}

	public InteractiveException(String neIp, String errMsg) {
		super(CODE, String.format(MSG_FORMAT, neIp, errMsg));
		this.neIp = neIp;
		this.errMsg = errMsg;
	}

	public InteractiveException(String neIp, String errMsg, Throwable cause) {
		super(CODE, String.format(MSG_FORMAT, neIp, errMsg), cause);
		this.neIp = neIp;
		this.errMsg = errMsg;
	}

	public String getNeIp() {
		return neIp;
	}

	public String getErrMsg() {
		return errMsg;
	}
}
