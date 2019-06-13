package taskmanager;

import config.Config;

import java.util.Iterator;

public class MeasurementContainer<T extends Comparable<T>> implements Measurements<T> {
	private int size;
	private T defaultValue;

	private Point oldest;
	private Point newest;

	private int time;

	public MeasurementContainer(T defaultValue) {
		this((int) (Config.getInt(Config.KEY_MAXIMUM_MEASURMENT_BUFFER_SIZE) * Config.getFloat(Config.KEY_UPDATE_RATE)), defaultValue);
	}

	public MeasurementContainer(int size, T defaultValue) {
		this.size = size;
		this.defaultValue = defaultValue;
		this.oldest = this.newest = new Point(defaultValue, -size);
		oldest.count = size;
	}

	@Override
	public void copyFrom(Measurements<T> otherRaw) {
		if (!(otherRaw instanceof MeasurementContainer<?>))
			throw new IllegalArgumentException("Argument must be a MeasurementContainer!");

		if (otherRaw.size() != size())
			throw new IllegalArgumentException("Sizes must be equal!");

		MeasurementContainer<T> other = (MeasurementContainer<T>) otherRaw;

		oldest = new Point(other.oldest);
		Point lastOtherPoint = other.oldest;
		Point lastPoint = oldest;
		while (lastOtherPoint.next != null) {
			Point newPoint = new Point(lastOtherPoint.next);
			lastPoint.next = newPoint;
			newPoint.previous = lastPoint;
			lastPoint = newPoint;
			lastOtherPoint = lastOtherPoint.next;
		}
		newest = lastPoint;
		time = other.time;
	}

	@Override
	public void copyDelta(Measurements<T> otherRaw) {
		if (!(otherRaw instanceof MeasurementContainer<?>))
			throw new IllegalArgumentException("Argument must be a MeasurementContainer!");

		if (otherRaw.size() != size())
			throw new IllegalArgumentException("Sizes must be equal!");

		MeasurementContainer<T> other = (MeasurementContainer<T>) otherRaw;

		Point newNewest = null;
		Point current = null;
		Point otherCurrent = other.newest;
		while (otherCurrent != null && otherCurrent.constructionTime != newest.constructionTime) {
			Point newPoint = new Point(otherCurrent);
			newPoint.next = current;
			if (current != null) {
				current.previous = newPoint;
			} else {
				newNewest = newPoint;
			}
			current = newPoint;
			otherCurrent = otherCurrent.previous;
		}

		if (otherCurrent == null) {
			oldest = current;
			newest = newNewest;
		} else {
			newest.count = otherCurrent.count;
			newest.next = current;
			if (current != null) {
				current.previous = newest;
			}

			while (oldest.constructionTime != other.oldest.constructionTime) {
				oldest = oldest.next;
			}
			oldest.previous = null;

			if (newNewest != null) {
				newest = newNewest;
			}
		}

		time = other.time;
	}

	@Override
	public synchronized void addValue(T value) {
		Point previous = newest;
		if (previous.value.equals(value)) {
			previous.count += 1;
		} else {
			Point newPoint = new Point(value, time);
			newPoint.count = 1;
			newest.next = newPoint;
			newPoint.previous = newest;
			newest = newPoint;
		}

		time += 1;

		pruneSingle();
	}

	private void pruneSingle() {
		if (time - (oldest.constructionTime + oldest.count) > size) {
			oldest = oldest.next;
			oldest.previous = null;
		}
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public int realSize() { // TODO Inefficient but currently only used to debug
		int size = 1;
		Point current = oldest;
		while (current != newest) {
			size++;
			current = current.next;
		}
		return size;
	}

	@Override
	public synchronized T newest() {
		return newest.value;
	}

	@Override
	public synchronized T oldest() {
		return oldest.value;
	}

	@Override
	public synchronized T max() { // TODO inefficient, use a monotone queue/heap
		T max = oldest.value;
		Point current = oldest.next;
		while (current != null) {
			if (max.compareTo(current.value) < 0) {
				max = current.value;
			}
			current = current.next;
		}
		return max;
	}

	@Override
	public synchronized T min() { // TODO inefficient, use a monotone queue/heap
		T min = oldest.value;
		Point current = oldest.next;
		while (current != null) {
			if (min.compareTo(current.value) > 0) {
				min = current.value;
			}
			current = current.next;
		}
		return min;
	}

	@Override
	public Iterator<T> getRangeIterator(int startIndex, int endIndex) {
		if (startIndex < 0 || startIndex >= size || endIndex < startIndex || endIndex >= size)
			throw new IllegalArgumentException("Indices out of range: [" + startIndex + ", " + endIndex + "], size: " + size);
		return new DataIterator(startIndex, endIndex);
	}


	private class DataIterator implements Iterator<T> {
		private Point lastPoint;
		private int start;
		private int end;
		private int index;

		public DataIterator(int start, int end) {
			this.start = start;
			this.end = end;
			index = 0;

			if (isBefore(oldest)) {
				lastPoint = new Point(defaultValue, time - size);
				lastPoint.count = oldest.constructionTime - lastPoint.constructionTime;
			} else {
				Point current = oldest;
				while (current != null) {
					if (!isAfter(current)) {
						lastPoint = current;
						break;
					}
					current = current.next;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return start + index <= end;
		}

		@Override
		public T next() {
			if (!hasNext())
				throw new IllegalStateException("The iterator is empty!");

			T dataPoint;

			synchronized (MeasurementContainer.this) {
				if (isAfter(lastPoint)) {
					lastPoint = lastPoint.next;
				}

				dataPoint = lastPoint.value;
			}

			index +=1;
			return dataPoint;
		}

		private boolean isBefore(Point point) {
			return start + index < size - (time - point.constructionTime);
		}

		private boolean isAfter(Point point) {
			return start + index >= size - (time - point.constructionTime) + point.count;
		}
	}


	private class Point {
		private final T value;
		private final int constructionTime;
		private int count;

		private Point next;
		private Point previous;

		public Point(T value, int constructionTime) {
			this.value = value;
			this.constructionTime = constructionTime;
		}

		public Point(Point other) {
			this.value = other.value;
			this.constructionTime = other.constructionTime;
			this.count = other.count;
		}
	}
}
