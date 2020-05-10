/*
 * Copyright (c) 2020. Sebastian Hjelm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * See LICENSE for further details.
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