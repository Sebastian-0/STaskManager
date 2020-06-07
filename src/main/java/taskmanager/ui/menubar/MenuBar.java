package taskmanager.ui.menubar;

import taskmanager.ui.ApplicationCallback;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

public class MenuBar extends JMenuBar {
    public MenuBar(ApplicationCallback callback) {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        fileMenu.add(new ExitMenuItem(callback));
        add(fileMenu);

        ButtonGroup updateRateGroup = new ButtonGroup();
        JMenu updateRateMenu = new JMenu("Update rate");
        updateRateMenu.add(new UpdateRateRadioButtonMenuItem(updateRateGroup, 10));
        updateRateMenu.add(new UpdateRateRadioButtonMenuItem(updateRateGroup, 5));
        updateRateMenu.add(new UpdateRateRadioButtonMenuItem(updateRateGroup, 2));
        updateRateMenu.add(new UpdateRateRadioButtonMenuItem(updateRateGroup, 1));
        updateRateMenu.add(new UpdateRateRadioButtonMenuItem(updateRateGroup, 0.5));
        updateRateMenu.add(new CustomUpdateRateRadioButtonMenuItem(updateRateGroup));


        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');
        viewMenu.add(updateRateMenu);
        viewMenu.addSeparator();
        viewMenu.add(new MinimizeToTrayMenuItem(callback));
        viewMenu.add(new AlwaysOnTopMenuItem(callback));
        viewMenu.addSeparator();
        viewMenu.add(new ShowDeadProcessesMenuItem(callback));
        viewMenu.addSeparator();
        viewMenu.add(new LinkTimelinesMenuItem(callback));
        add(viewMenu);
    }
}
