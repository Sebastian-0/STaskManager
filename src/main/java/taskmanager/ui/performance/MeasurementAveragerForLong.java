package taskmanager.ui.performance;

import taskmanager.Measurements;

import java.util.Iterator;

public class MeasurementAveragerForLong extends MeasurementAverager<Long> {

	public MeasurementAveragerForLong(Measurements<Long> measurements) {
		super(measurements);
	}

	@Override
	protected Long computeAverage(Iterator<Long> iterator, int stepSize) {
		if (iterator != null && iterator.hasNext()) {
			long total = 0;
			for (int i = 0; i < stepSize; i++) {
				total += iterator.next();
			}
			return total / stepSize;
		}
		return 0L;
	}
}
