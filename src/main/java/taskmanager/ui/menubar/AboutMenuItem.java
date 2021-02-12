package taskmanager.ui.menubar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taskmanager.ui.callbacks.ApplicationCallback;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;

public class AboutMenuItem extends JMenuItem {
    private static final Logger LOGGER = LoggerFactory.getLogger(AboutMenuItem.class);

    private static final String ABOUT_TEXT = "<html>" +
            "This program was made by Sebastian Hjelm and is licensed under GPLv3.<br/>" +
            "Source code is available at {0}<br/>" +
            "<br/>" +
            "Powered by {1}" +
            "</html>";

    public AboutMenuItem(ApplicationCallback callback) {
        super("About");
        setMnemonic('A');
        addActionListener(e -> showAbout(callback.frame()));
    }

    private void showAbout(JFrame frame) {
        Object textToShow;
        if (Desktop.isDesktopSupported()) {
            String finalText = MessageFormat.format(ABOUT_TEXT,
                    "<a href=\"https://github.com/Sebastian-0/Taskmanager\">GitHub</a>",
                    "<a href=\"https://github.com/oshi/oshi\">OSHI</a>");
            JEditorPane editorPane = new JEditorPane("text/html", finalText);
            editorPane.addHyperlinkListener(event -> {
                try {
                    if (event.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                        Desktop.getDesktop().browse(event.getURL().toURI());
                    }
                } catch (IOException | URISyntaxException e) {
                    LOGGER.error("Failed to open URL", e);
                }
            });
            editorPane.setEditable(false);
            JLabel defaultStyle = new JLabel();
            editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            editorPane.setBackground(defaultStyle.getBackground());
            editorPane.setForeground(defaultStyle.getForeground());
            editorPane.setFont(defaultStyle.getFont());
            textToShow = editorPane;
        } else {
            textToShow = MessageFormat.format(ABOUT_TEXT,
                    "<a href=\"https://github.com/Sebastian-0/Taskmanager\">https://github.com/Sebastian-0/Taskmanager</a>",
                    "<a href=\"https://github.com/oshi/oshi\">OSHI</a>");
        }
        JOptionPane.showMessageDialog(frame, textToShow, "About Taskmanager", JOptionPane.INFORMATION_MESSAGE);
    }
}
