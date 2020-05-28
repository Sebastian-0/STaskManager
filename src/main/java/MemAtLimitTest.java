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
import java.util.ArrayList;
import java.util.List;

public class MemAtLimitTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(MemAtLimitTest.class);

	public static void main(String[] args) throws InterruptedException {
		LOGGER.info("Double {} {}", 1, 1);
		System.out.println(MessageFormat.format("{0} hello", "10"));

		List<byte[]> waste = new ArrayList<>();
		while(true) {
			waste.add(new byte[400_000]);
			Thread.sleep(1);
		}
	}
}
