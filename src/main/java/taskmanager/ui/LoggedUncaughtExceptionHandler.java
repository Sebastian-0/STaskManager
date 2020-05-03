/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;

public class LoggedUncaughtExceptionHandler implements UncaughtExceptionHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggedUncaughtExceptionHandler.class);

	@Override
	public void uncaughtException(Thread thread, Throwable throwable) {
		LOGGER.error("Uncaught exception in thread " + thread.getName(), throwable);
	}
}
