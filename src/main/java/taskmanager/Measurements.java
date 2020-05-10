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

package taskmanager;

import java.util.Iterator;

public interface Measurements<T> {
	void copyFrom(Measurements<T> other);
	void copyDelta(Measurements<T> other);

	void addValue(T value);

	T newest();
	T oldest();
	T max();
	T min();

	Iterator<T> getRangeIterator(int startIndex, int endIndex);
	int size();
	int realSize();
}
