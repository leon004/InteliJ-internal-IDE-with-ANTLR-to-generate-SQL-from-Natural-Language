package io.github.leon004.nl2sql;

import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;
import io.github.leon004.nl2sql.ui.MainWindow;
import javax.swing.SwingUtilities;

/** Application entry point. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "NL2SQL IDE");
        SwingUtilities.invokeLater(() -> {
            FlatDarkPurpleIJTheme.setup();
            MainWindow window = new MainWindow();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
