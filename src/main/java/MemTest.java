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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

public class MemTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(MemTest.class);

	public static void main(String[] args) {
		LOGGER.info("Double {} {}", 1, 1);
		System.out.println(MessageFormat.format("{0} hello", "10"));

		double[] tmp = new double[1024 * 1024 * 1024 / 5];
		try {
			Thread.sleep(1000000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
