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
