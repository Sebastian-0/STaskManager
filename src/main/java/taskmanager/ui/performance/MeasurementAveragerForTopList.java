/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.ui.performance;

import config.Config;
import taskmanager.Measurements;
import taskmanager.Process;
import taskmanager.SystemInformation.TopList;
import taskmanager.SystemInformation.TopList.Entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MeasurementAveragerForTopList extends MeasurementAverager<TopList> {
	private static final float MISSING_PROCESS_USAGE_FRACTION = 0.1f;

	public MeasurementAveragerForTopList(Measurements<TopList> measurements) {
		super(measurements);
	}

	@Override
	protected TopList computeAverage(Iterator<TopList> iterator, int stepSize) {
		TopList latestTopList = TopList.EMPTY;
		if (iterator != null && iterator.hasNext()) {
			List<TopList> topLists = new ArrayList<>(stepSize);
			TopList last = null;
			for (int i = 0; i < stepSize; i++) {
				TopList topList = iterator.next();
				if (topList != last) {
					topLists.add(topList);
					last = topList;
				}
			}
			latestTopList = averageOf(topLists);
		}
		return latestTopList;
	}


	public static TopList averageOf(List<TopList> topLists) {

		if (topLists.size() == 1) {
			return topLists.get(0);
		}
		if (topLists.size() == 0) {
			return TopList.EMPTY;
		}

		Map<Long, Long> idToTotal = new HashMap<>();
		Map<Long, Process> idToProcess = topLists.stream()
				.flatMap(e -> Arrays.stream(e.entries))
				.map(e -> e.process)
				.distinct()
				.collect(Collectors.toMap(p -> p.uniqueId, p -> p));

		if (idToProcess.isEmpty()) {
			return TopList.EMPTY;
		}

		Set<Long> seenIds = new HashSet<>();
		for (TopList topList : topLists) {
			seenIds.clear();
			long totalUsageInTopList = 0;
			for (Entry entry : topList.entries) {
				long id = entry.process.uniqueId;
				idToTotal.put(id, entry.value + idToTotal.getOrDefault(id, 0L));
				totalUsageInTopList += entry.value;
				seenIds.add(id);
			}

			for (Long id : idToProcess.keySet()) {
				if (!seenIds.contains(id)) {
					idToTotal.put(id, (long) (totalUsageInTopList * 0.1 + idToTotal.getOrDefault(id, 0L)));
				}
			}
		}

		List<Map.Entry<Long, Long>> entries = new ArrayList<>(idToTotal.entrySet());
		entries.sort((a, b) -> signum(b.getValue() - a.getValue()));

		int topListSize = Math.min(Config.getInt(Config.KEY_METRIC_TOP_LIST_SIZE), idToProcess.size());
		TopList topList = new TopList(topListSize);
		for (int i = 0; i < topListSize; i++) {
			long id = entries.get(i).getKey();
			long score = entries.get(i).getValue() / topLists.size();
			topList.entries[i] = new Entry(score, idToProcess.get(id));
		}

		return topList;
	}

	public static TopList weightedAverageOf(TopList l1, TopList l2, float factor) {
		if (l1 == TopList.EMPTY) {
			return l2;
		} else if (l2 == TopList.EMPTY) {
			return l1;
		}

		Map<Long, Long> ids1 = Arrays.stream(l1.entries).collect(Collectors.toMap(e -> e.process.uniqueId, e -> e.value));
		Map<Long, Long> ids2 = Arrays.stream(l2.entries).collect(Collectors.toMap(e -> e.process.uniqueId, e -> e.value));

		long sum1 = ids1.values().stream().reduce(0L, Long::sum);
		long sum2 = ids2.values().stream().reduce(0L, Long::sum);

		List<TopList.Entry> allEntries = new ArrayList<>();
		for (TopList.Entry e1 : l1.entries) {
			long left = e1.value;
			long right = ids2.getOrDefault(e1.process.uniqueId, (long) (sum2 * MISSING_PROCESS_USAGE_FRACTION));
			allEntries.add(new TopList.Entry((long) (left * (1 - factor) + right * factor), e1.process));
		}
		for (TopList.Entry e2 : l2.entries) {
			if (!ids1.containsKey(e2.process.uniqueId)) {
				long left = (long) (sum1 * MISSING_PROCESS_USAGE_FRACTION);
				long right = e2.value;
				allEntries.add(new TopList.Entry((long) (left * (1 - factor) + right * factor), e2.process));
			}
		}
		allEntries.sort((e1, e2) -> signum(e2.value - e1.value));

		int topListSize = Config.getInt(Config.KEY_METRIC_TOP_LIST_SIZE);
		TopList topList = new TopList(Math.min(topListSize, allEntries.size()));
		for (int i = 0; i < topListSize; i++) {
			topList.entries[i] = allEntries.get(i);
		}
		return topList;
	}

	private static int signum(long value) { // TODO Duplicated from InformationLoader
		if (value > 0) {
			return 1;
		} else if (value < 0) {
			return -1;
		}
		return 0;
	}
}
