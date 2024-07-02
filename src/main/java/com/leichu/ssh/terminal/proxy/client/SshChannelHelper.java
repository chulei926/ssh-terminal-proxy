package com.leichu.ssh.terminal.proxy.client;


import com.leichu.ssh.terminal.proxy.Callback;
import com.leichu.ssh.terminal.proxy.exception.InteractiveException;
import org.apache.commons.io.IOUtils;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.common.channel.PtyChannelConfiguration;
import org.apache.sshd.common.channel.StreamingChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * SSH会话通道.
 *
 * @author ffchul.
 * @since 2023-07-30.
 */
public class SshChannelHelper {

	public static final Logger logger = LoggerFactory.getLogger(SshChannelHelper.class);

	public static final long DEFAULT_TIMEOUT = 30L;

	private static final String PTY_TYPE = "xterm";

	public static ChannelShell getChannelShell(SshSession sshSession, Callback channelCloseCallback) {
		ChannelShell channel;
		try {
			PtyChannelConfiguration ptyChannelConfiguration = new PtyChannelConfiguration();
			ptyChannelConfiguration.setPtyColumns(1000);
			ptyChannelConfiguration.setPtyType(PTY_TYPE);
			channel = sshSession.getSession().createShellChannel(ptyChannelConfiguration, new HashMap<>());
			channel.setRedirectErrorStream(true);
			channel.open().verify(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
			String rawSid = new String(channel.getSession().getSessionId());
			channel.addCloseFutureListener(future -> {
				// SSH 通道关闭监听器 -- Add by chul at 2023-09-15
				if (future.isClosed()) {
					logger.warn("Close channel listener: SSH channel closed!");
					IOUtils.closeQuietly(channel.getInvertedIn(), channel.getInvertedOut(), channel.getInvertedErr());
					channelCloseCallback.emit(rawSid);
				}
			});

			logger.info("Start creating an SSH channel.");
		} catch (Exception e) {
			throw new InteractiveException(null, "ClientChannel create failed!", e);
		}
		return channel;
	}

	public static void close(ChannelShell channelShell) {
		try {
			if (null != channelShell && !channelShell.isClosed()) {
				channelShell.close();
				logger.info("ChannelShell closed!");
			}
		} catch (Exception e) {
			logger.error("ChannelShell close error!", e);
		}
	}
}
