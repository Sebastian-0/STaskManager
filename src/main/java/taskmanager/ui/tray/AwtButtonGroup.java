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

package taskmanager.ui.tray;

import java.util.ArrayList;
import java.util.List;

public class AwtButtonGroup {
	private List<AwtRadioButtonMenuItem> buttons;

	public AwtButtonGroup() {
		buttons = new ArrayList<>();
	}

	public void addButtons(AwtRadioButtonMenuItem... buttons) {
		for (AwtRadioButtonMenuItem button : buttons) {
			this.buttons.add(button);
			button.setButtonGroup(this);
		}
	}

	public void deselectExcept(AwtRadioButtonMenuItem target) {
		for (AwtRadioButtonMenuItem button : buttons) {
			if (button != target) {
				button.setState(false);
			}
		}
	}
}
