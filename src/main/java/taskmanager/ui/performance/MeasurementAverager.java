/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.ui.performance;

import taskmanager.Measurements;

import java.util.Iterator;

public abstract class MeasurementAverager<T> {
	private Measurements<T> measurements;
	private Iterator<T> iterator;

	private int offset;
	private int numPoints;

	private int start;
	private int end;

	private int stepSize;

	public MeasurementAverager(Measurements<T> measurements) {
		this.measurements = measurements;
		this.stepSize = 1;
	}

	public void setInterval(int start, int end, int stepSize) {
		this.stepSize = stepSize;
		this.start = Math.max(start, stepSize);
		this.end = end;

		this.end -= (this.end - this.start) % stepSize;
	}

	public void reset() {
		reset(0);
	}

	public void reset(int idx) {
		int start = this.start - offset + idx * stepSize;
		int end = this.end - offset;

		iterator = measurements.getRangeIterator(start, end - 1);
		numPoints = (end - start) / stepSize - 1;
	}

	public T next() {
		return computeAverage(iterator, stepSize);
	}

	protected abstract T computeAverage(Iterator<T> iterator, int stepSize);

	public boolean hasNext() {
		return iterator != null && iterator.hasNext();
	}

	public int numPoints() {
		return numPoints;
	}

	public void shift(int off) {
//		offset = ++offset % stepSize;
		offset = off % stepSize;
	}
}
