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

import java.text.MessageFormat;

public class CpuTest {
	public static void main(String[] args) {
		final int cores = Runtime.getRuntime().availableProcessors();
		System.out.println(MessageFormat.format("{0} hello", "10"));

		for (int i = 0; i < cores; i++) {
			new Thread(() -> { while (true); }).start();
		}
	}
}
