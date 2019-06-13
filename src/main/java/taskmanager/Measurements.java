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
