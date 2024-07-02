package com.leichu.ssh.terminal.proxy.server;

import com.alibaba.fastjson.JSONObject;
import com.leichu.ssh.terminal.proxy.Callback;
import com.leichu.ssh.terminal.proxy.client.SshChannelHelper;
import com.leichu.ssh.terminal.proxy.client.SshSession;
import com.leichu.ssh.terminal.proxy.model.SshAuthParam;
import com.leichu.ssh.terminal.proxy.model.User;
import com.leichu.ssh.terminal.proxy.service.UserService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ProxyServerStarter implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ProxyServerStarter.class);

	@Resource
	private UserService userService;
	@Value("${ssh.proxy.port:1122}")
	private int sshProxyPort;

	private SshServer sshd;

	private String banner = "";

	@PostConstruct
	public void init() {
		try {
			ClassPathResource classPathResource = new ClassPathResource("config/banner.txt");
			banner = String.format("\r\n%s\r\n", IOUtils.toString(classPathResource.getInputStream()));
			System.out.println(banner);
		} catch (Exception e) {
			banner = "\r\nWelcome To SSH Terminal\r\n";
			logger.error("Banner load fail!", e);
		}

	}

	@PreDestroy
	public void preDestroy() {
		try {
			if (null != sshd) {
				sshd.close();
			}
		} catch (Exception e) {
			logger.error("SshServer close error!", e);
		}
	}

	@Override
	public void run(String... args) throws Exception {
		sshd = SshServer.setUpDefaultServer();
		sshd.setPort(sshProxyPort);
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser")));

		sshd.setPasswordAuthenticator((username, password, session) -> {
			logger.info("执行认证逻辑 >>>>>client:{} username:{} password:{}", session.getClientAddress(), username, password);
			User user = userService.getUser(username);
			if (null == user || !user.getPassword().equals(password)) {
				return false;
			}
			// 认证成功，此处可做记录
			return true;
		});

		CoreModuleProperties.WELCOME_BANNER.set(sshd, banner);
		CoreModuleProperties.WELCOME_BANNER_CHARSET.set(sshd, StandardCharsets.UTF_8);

		sshd.setShellFactory(new CustomShellFactory());
		sshd.start();
		logger.info("SSH Proxy Server started on port {}", sshProxyPort);
	}

	public static class CustomShellFactory implements ShellFactory {

		@Override
		public Command createShell(ChannelSession channelSession) throws IOException {
			CustomShellCommand customShellCommand = new CustomShellCommand();
			logger.info("CustomShellCommand created!");
			return customShellCommand;
		}

		public static class CustomShellCommand implements Command, Runnable {
			private InputStream clientInputStream;
			private OutputStream clientOutputStream;
			private OutputStream clientErrorOutputStream;
			private ExitCallback callback;
			private Thread thread;

			private ChannelShell channelShell;

			private final AtomicBoolean bind = new AtomicBoolean(false);

			private final StringBuilder inputCache = new StringBuilder();

			public CustomShellCommand() {
			}

			@Override
			public void setInputStream(InputStream in) {
				this.clientInputStream = in;
			}

			@Override
			public void setOutputStream(OutputStream out) {
				this.clientOutputStream = out;
			}

			@Override
			public void setErrorStream(OutputStream err) {
				this.clientErrorOutputStream = err;
			}

			@Override
			public void setExitCallback(ExitCallback callback) {
				this.callback = callback;
			}

			private void read() throws Exception {
				int b;
				while ((b = clientInputStream.read()) != -1) {
					if (Boolean.FALSE.equals(bind.get())) {
						if (b == '\n' || b == '\r') {
							if (inputCache.length() > 0) {
								String command = inputCache.toString();
								String result = executeCommand(command);
								clientOutputStream.write(result.getBytes());
								clientOutputStream.flush();
								inputCache.setLength(0);
							} else {
								clientOutputStream.write("\r\n$ ".getBytes(StandardCharsets.UTF_8));
								clientOutputStream.flush();
								inputCache.setLength(0);
							}
						} else if (b == 127 || b == 8) {
							if (inputCache.length() <= 0) {
								// 缓冲区已经没有内容，不能退格
							} else {
								clientOutputStream.write('\b');
								clientOutputStream.write(' ');
								clientOutputStream.write('\b');
								clientOutputStream.flush();
								if (inputCache.length() > 0) {
									inputCache.deleteCharAt(inputCache.length() - 1);
								}
							}
						} else if (b == 0x41 || b == 0x42 || b == 0x43 || b == 0x44) {
							// 禁用上下左右（暂不实现）
						} else {
							inputCache.append((char) b);
							clientOutputStream.write(b);
							clientOutputStream.flush();
						}
					} else {
						OutputStream invertedIn = channelShell.getInvertedIn();
						System.out.println("Char:" + (char) b);
						invertedIn.write(b);
						invertedIn.flush();
					}

				}
			}

			@Override
			public void run() {
				try {
					read();
				} catch (Exception e) {
					try {
						printException(e);
					} catch (Exception e1) {

					}
				} finally {
					callback.onExit(0);
				}

			}


			@Override
			public void start(ChannelSession channelSession, Environment environment) throws IOException {
				thread = new Thread(this, "CustomShellCommand");
				thread.start();
				printHelp();
			}

			private void printHelp() throws IOException {
				clientOutputStream.write("\r\nPlease input ssh target. Example:{host:\"192.182.1.1\",port:22,username:\"readonly_user\",password:\"xxxxxx\"}\r\n$ ".getBytes(StandardCharsets.UTF_8));
				clientOutputStream.flush();
			}

			private void printParseFail(String input) throws IOException {
				clientOutputStream.write(("\r\nThe input format is incorrect:" + input + "\r\n").getBytes(StandardCharsets.UTF_8));
				clientOutputStream.flush();
				printHelp();
			}

			private void printExit() throws IOException {
				clientOutputStream.write("\r\nGoodbye!\r\n".getBytes(StandardCharsets.UTF_8));
				clientOutputStream.flush();
				callback.onExit(0);
			}

			private void printException(Exception e) throws IOException {
				clientOutputStream.write(("\r\n" + e.getMessage() + "\r\n$ ").getBytes(StandardCharsets.UTF_8));
				clientOutputStream.flush();
			}

			@Override
			public void destroy(ChannelSession channelSession) throws Exception {
				if (thread != null) {
					thread.interrupt();
				}
			}

			private String executeCommand(String command) throws Exception {
				if (command.equalsIgnoreCase("help")) {
					printHelp();
					return "";
				}
				if (command.equalsIgnoreCase("exit")) {
					printExit();
					return "";
				}
				command = StringUtils.trim(command);
				try {
					SshAuthParam sshAuthParam = JSONObject.parseObject(command, SshAuthParam.class);
					sshLogin(sshAuthParam);
				} catch (Exception e1) {
					// 打印解析失败提示
					printParseFail(command);
				}
				return "";
			}

			private void sshLogin(SshAuthParam sshAuthParam) throws Exception {
				logger.info(">>>登录<<< {}", sshAuthParam.toString());
				SshSession sshSession = new SshSession(sshAuthParam);
				sshSession.connect();
				channelShell = SshChannelHelper.getChannelShell(sshSession, new Callback<String>() {
					@Override
					public void emit(String rawSid) {
						logger.info("ChannelShell closed callback! rawSid:{}", rawSid);
						bind.set(Boolean.FALSE);
					}
				});
				clientOutputStream.write("\r\nLogin Success!\r\n".getBytes(StandardCharsets.UTF_8));
				clientOutputStream.flush();
				new Thread(() -> {
					bind.set(Boolean.TRUE);
					while (true) {
						try {
							InputStream invertedOut = channelShell.getInvertedOut();
							int s;
							while (!channelShell.isClosed() && (s = invertedOut.read()) != -1) {
								clientOutputStream.write(s);
								clientOutputStream.flush();
							}
						} catch (Exception e) {
							logger.error("ChannelShell read error!", e);
						}
					}
				}).start();
			}

		}
	}
}


