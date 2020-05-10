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

package taskmanager.ui.performance;

import java.util.ArrayList;
import java.util.List;

public class TimelineGroup {
	private final List<TimelineGraphPanel> timelines;
	private boolean areLinked;
	
	public TimelineGroup() {
		timelines = new ArrayList<>();
	}
	
	public void add(TimelineGraphPanel timeline) {
		timelines.add(timeline);
		timeline.addToGroup(this);
	}
	
	public void setLinked(boolean areLinked) {
		this.areLinked = areLinked;
	}
	
	protected void changed(TimelineGraphPanel source, int startIdx, int endIdx) {
		if (areLinked) {
			for (TimelineGraphPanel timeline : timelines) {
				if (timeline != source) {
					timeline.updateIndices(startIdx, endIdx);
				}
			}
		}
	}	
}