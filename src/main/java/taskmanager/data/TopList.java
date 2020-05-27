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

package taskmanager.data;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class TopList implements Comparable<TopList> { // TODO Support TopLists of Short!
	public static final TopList EMPTY = new TopList(0);

	public final Entry[] entries;

	public TopList(int size) {
		this.entries = new Entry[size];
	}

	public static TopList of(Function<Process, Long> extractor, List<Process> processes, int length) {
		TopList topList = new TopList(Math.min(length, processes.size()));
		for (int i = 0; i < topList.entries.length; i++) {
			topList.entries[i] = new Entry(extractor.apply(processes.get(i)), processes.get(i));
		}
		return topList;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof TopList) {
			return Arrays.equals(entries, ((TopList) other).entries);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash((Object[]) entries);
	}

	@Override
	public int compareTo(TopList topList) {
		throw new UnsupportedOperationException("Can't compare two toplists!");
	}


	public static class Entry {
		public final long value;
		public final Process process;

		public Entry(long value, Process process) {
			this.value = value;
			this.process = process;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Entry) {
				Entry otherEntry = (Entry) obj;
				return value == otherEntry.value && process.uniqueId == otherEntry.process.uniqueId;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(value, process.uniqueId);
		}
	}
}
