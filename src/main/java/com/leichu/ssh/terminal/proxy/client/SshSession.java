package com.leichu.ssh.terminal.proxy.client;


import com.leichu.ssh.terminal.proxy.exception.AuthException;
import com.leichu.ssh.terminal.proxy.model.SshAuthParam;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SshSession {

	public static final Logger logger = LoggerFactory.getLogger(SshSession.class);
	private final String sid;
	private final SshAuthParam authParam;
	private final SshClient client;
	private ClientSession session;

	public static final long DEFAULT_TIMEOUT = 60L;

	public SshSession(SshAuthParam authParam) {
		this.sid = UUID.randomUUID().toString();
		this.authParam = authParam;
		client = SshClientHolder.getInstance().getSshClient();
	}

	public void connect() {
		try {
			session = client.connect(authParam.getUsername(), authParam.getHost(), authParam.getPort())
					.verify(TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT)).getSession();
			session.addPasswordIdentity(authParam.getPassword());
			session.addCloseFutureListener(future -> {
				// 关闭会话监听器 -- Add by chul at 2023-09-15
				if (future.isClosed()) {
					logger.warn("Close session listener：SSH session closed!");
				}
			});

			final AuthFuture verify = session.auth().verify(TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT));
			if (!verify.isSuccess()) {
				logger.error("ssh verify failed!", verify.getException());
				throw new AuthException(authParam.getHost(), "SSH login timeout!");
			}
			logger.info("ssh session auth and create success! authParam:{}", authParam);
		} catch (Exception e) {
			logger.error("ssh session auth and create error! authParam:{}", authParam, e);
			throw new AuthException(authParam.getHost(), "SSH auth failed!");
		}


	}

	public ClientSession getSession() {
		return session;
	}

	public void destroy() {
		try {
			if (null != session && !session.isClosed()) {
				session.close(true);
				logger.info("SSH Session closed!");
			}
		} catch (Exception e) {
			logger.error("SSH session close error!", e);
		}
	}

	public String getSid() {
		return sid;
	}
}
