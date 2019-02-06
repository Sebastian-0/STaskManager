package taskmanager;

import config.Config;

import java.util.Iterator;

public class MeasurementContainer<T extends Comparable<T>> implements Measurements<T> {
	private int size;
	private T[] data;
	private int startIndex;

	public MeasurementContainer(T defaultValue) {
		this((int) (Integer.parseInt(Config.get(Config.KEY_MAXIMUM_MEASURMENT_BUFFER_SIZE)) * Double.parseDouble(Config.get(Config.KEY_UPDATE_RATE))), defaultValue);
	}

	@SuppressWarnings("unchecked")
	public MeasurementContainer(int size, T defaultValue) {
		this.size = size;
		data = (T[]) new Comparable[size];
		for (int i = 0; i < data.length; i++) {
			data[i] = defaultValue;
		}
	}

	@Override
	public void copyFrom(Measurements<T> otherRaw) {
		if (!(otherRaw instanceof MeasurementContainer<?>))
			throw new IllegalArgumentException("Argument must be a MeasurementContainer!");

		if (otherRaw.size() != size())
			throw new IllegalArgumentException("Sizes must be equal!");

		MeasurementContainer<T> other = (MeasurementContainer<T>) otherRaw;
		System.arraycopy(other.data, 0, data, 0, size);
		startIndex = other.startIndex;
	}

	@Override
	public void copyDelta(Measurements<T> otherRaw) {
		if (!(otherRaw instanceof MeasurementContainer<?>))
			throw new IllegalArgumentException("Argument must be a MeasurementContainer!");

		if (otherRaw.size() != size())
			throw new IllegalArgumentException("Sizes must be equal!");

		MeasurementContainer<T> other = (MeasurementContainer<T>) otherRaw;

		if (other.startIndex < startIndex) {
			System.arraycopy(other.data, startIndex, data, startIndex, size - startIndex);
			startIndex = 0;
		}
		System.arraycopy(other.data, startIndex, data, startIndex, other.startIndex - startIndex);
		startIndex = other.startIndex;
	}

	@Override
	public synchronized void addValue(T value) {
		data[startIndex] = value;
		startIndex = (startIndex + 1) % size;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public synchronized T newest() {
		return data[(size + startIndex - 1) % size];
	}

	@Override
	public synchronized T oldest() {
		return data[startIndex];
	}

	@Override
	public synchronized T max() { // TODO highly inefficient, use a monotone queue
		T max = data[0];
		for (int i = 1; i < data.length; i++) {
			if (max.compareTo(data[i]) < 0) {
				max = data[i];
			}
		}
		return max;
	}

	@Override
	public synchronized T min() { // TODO highly inefficient, use a monotone queue
		T max = data[0];
		for (int i = 1; i < data.length; i++) {
			if (max.compareTo(data[i]) > 0) {
				max = data[i];
			}
		}
		return max;
	}

	@Override
	public Iterator<T> getRangeIterator(int startIndex, int endIndex) {
		if (startIndex < 0 || startIndex >= size || endIndex < startIndex || endIndex >= size)
			throw new IllegalArgumentException("Indices out of range: [" + startIndex + ", " + endIndex + "], size: " + size);
		return new DataIterator(startIndex, endIndex);
	}


	private class DataIterator implements Iterator<T> {
		private int index;
		private int pointsLeft;

		public DataIterator(int start, int end) {
			index = (startIndex + start) % size;
			pointsLeft = end - start + 1;
		}

		@Override
		public boolean hasNext() {
			return pointsLeft > 0;
		}

		@Override
		public T next() {
			if (!hasNext())
				throw new IllegalStateException("The iterator is empty!");

			T dataPoint;
			synchronized (MeasurementContainer.this) {
				dataPoint = data[index];
			}
			index = (index + 1) % size;
			pointsLeft--;
			return dataPoint;
		}
	}
}
