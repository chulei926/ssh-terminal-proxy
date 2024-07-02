package com.leichu.ssh.terminal.proxy.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 线程池工具类.
 * <p>
 * 所有的自定义异步任务，都可以提交至此线程池。<br>
 * </p>
 *
 * @author chul 2022-08-19.
 */
public class ThreadPoolHelper {

	private static final Logger logger = LoggerFactory.getLogger(ThreadPoolHelper.class);

	private static volatile ThreadPoolHelper instance = null;

	private static List<ThreadPoolTaskExecutor> executors = new CopyOnWriteArrayList<>();

	static {
		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook(new Thread(() -> {
			if (!executors.isEmpty()) {
				for (ThreadPoolTaskExecutor executor : executors) {
					executor.shutdown();
				}
			}
			logger.info("Custom thread pool is closed！");
		}));
	}

	private ThreadPoolHelper() {
	}

	public static ThreadPoolHelper getInstance() {
		if (null == instance) {
			synchronized (ThreadPoolHelper.class) {
				if (null == instance) {
					instance = new ThreadPoolHelper();
				}
			}
		}
		return instance;
	}

	public ThreadPoolTaskExecutor createSimplePool(int coreSize, int maxSize, int capacity, String threadPrefix) {
		ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setCorePoolSize(coreSize);
		threadPoolTaskExecutor.setMaxPoolSize(maxSize);
		threadPoolTaskExecutor.setQueueCapacity(capacity);
		threadPoolTaskExecutor.setThreadNamePrefix(null == threadPrefix ? "CUSTOM-THREAD" : threadPrefix);
		threadPoolTaskExecutor.setRejectedExecutionHandler((r, executor) -> {
			try {
				executor.getQueue().put(r);
			} catch (Exception e) {
				logger.error("RejectedExecutionHandler error!", e);
			}
		});
		threadPoolTaskExecutor.initialize();
		return threadPoolTaskExecutor;
	}


}
