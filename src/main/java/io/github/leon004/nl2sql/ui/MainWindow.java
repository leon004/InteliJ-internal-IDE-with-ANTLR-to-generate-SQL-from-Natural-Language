package io.github.leon004.nl2sql.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGradiantoDarkFuchsiaIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneLightIJTheme;
import io.github.leon004.nl2sql.codegen.JavaCrudGenerator;
import io.github.leon004.nl2sql.codegen.SchemaSummary;
import io.github.leon004.nl2sql.codegen.SqlGenerator;
import io.github.leon004.nl2sql.compiler.CompilationException;
import io.github.leon004.nl2sql.compiler.NaturalLanguageCompiler;
import io.github.leon004.nl2sql.db.DatabaseConfig;
import io.github.leon004.nl2sql.db.MySqlExporter;
import io.github.leon004.nl2sql.model.Language;
import io.github.leon004.nl2sql.model.Schema;
import io.github.leon004.nl2sql.model.SourceSpan;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

/**
 * The IDE window: a natural-language editor on the left and the generated SQL, schema and Java
 * code on the right. The program is re-validated as you type and errors are highlighted.
 *
 * <p>The window's own labels are shown in English or Spanish (menu Language / Idioma, remembered
 * between runs). Compiler messages always follow the language of the program being edited.
 */
public final class MainWindow extends JFrame {

    private static final Color OK_COLOR = new Color(0x3FA34D);
    private static final Color ERROR_COLOR = new Color(0xE5484D);
    private static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    private static final String PREF_UI_LANGUAGE = "uiLanguage";
    private static final Preferences PREFS = Preferences.userNodeForPackage(MainWindow.class);

    private final JTextArea editor = codeArea(true);
    private final JTextArea sqlOutput = codeArea(false);
    private final JTextArea schemaOutput = codeArea(false);
    private final JTextArea javaOutput = codeArea(false);
    private final JTabbedPane outputTabs = new JTabbedPane();
    private final JLabel editorTitle = panelTitle();
    private final JLabel outputTitle = panelTitle();
    private final JLabel status = new JLabel(" ");
    private final JFileChooser fileChooser = new JFileChooser();
    private final Highlighter.HighlightPainter errorPainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(0xE5, 0x48, 0x4D, 110));
    private final Timer validationTimer = new Timer(300, e -> validateSource());

    private final SqlGenerator sqlGenerator = new SqlGenerator();
    private final SchemaSummary schemaSummary = new SchemaSummary();
    private final JavaCrudGenerator javaGenerator = new JavaCrudGenerator();

    private final Action compileAction = action("", this::compile);
    private final Action exportAction = action("", this::exportToMySql);

    private Language ui = loadUiLanguage();
    private JToolBar toolBar;

    public MainWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 600));
        setPreferredSize(new Dimension(1280, 760));

        validationTimer.setRepeats(false);
        exportAction.setEnabled(false);

        add(buildWorkspace(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        applyUiLanguage();
        bindShortcuts();

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { sourceChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { sourceChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { sourceChanged(); }
        });

        loadExample(ui);
        pack();
    }

    // ---------------------------------------------------------------------------------------
    // UI language
    // ---------------------------------------------------------------------------------------

    /** Short for "pick the English or Spanish label for the current UI language". */
    private String t(String english, String spanish) {
        return ui.pick(spanish, english);
    }

    private void setUiLanguage(Language language) {
        ui = language;
        PREFS.put(PREF_UI_LANGUAGE, language.name());
        applyUiLanguage();
    }

    /** (Re)builds every label that depends on the UI language. */
    private void applyUiLanguage() {
        setTitle(t("NL2SQL IDE: natural language to SQL & Java", "NL2SQL IDE: lenguaje natural a SQL y Java"));

        compileAction.putValue(Action.NAME, t("Compile", "Compilar"));
        compileAction.putValue(Action.SHORT_DESCRIPTION, t("Compile to SQL and Java (Ctrl+Enter)", "Compilar a SQL y Java (Ctrl+Enter)"));
        exportAction.putValue(Action.NAME, t("Export to MySQL", "Exportar a MySQL"));
        exportAction.putValue(Action.SHORT_DESCRIPTION, t("Run the generated SQL on MySQL", "Ejecutar el SQL generado en MySQL"));

        setJMenuBar(buildMenuBar());
        if (toolBar != null) {
            remove(toolBar);
        }
        toolBar = buildToolBar();
        add(toolBar, BorderLayout.NORTH);

        editorTitle.setText(t("Natural language", "Lenguaje natural"));
        outputTitle.setText(t("Generated code", "Código generado"));
        outputTabs.setTitleAt(1, t("Schema", "Esquema"));

        revalidate();
        repaint();
    }

    private static Language loadUiLanguage() {
        try {
            return Language.valueOf(PREFS.get(PREF_UI_LANGUAGE, Language.ENGLISH.name()));
        } catch (IllegalArgumentException e) {
            return Language.ENGLISH;
        }
    }

    // ---------------------------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------------------------

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu(t("File", "Archivo"));
        file.add(action(t("Open…", "Abrir…"), this::openFile))
                .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_MASK));
        file.add(action(t("Save program…", "Guardar programa…"), this::saveProgram))
                .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MASK));
        file.addSeparator();
        file.add(action(t("Save SQL…", "Guardar SQL…"), () -> saveText(sqlOutput.getText(), "script.sql")));
        file.add(action(t("Save Java…", "Guardar Java…"),
                () -> saveText(javaOutput.getText(), JavaCrudGenerator.CLASS_NAME + ".java")));
        file.addSeparator();
        file.add(action(t("Load English example", "Cargar ejemplo en inglés"), () -> loadExample(Language.ENGLISH)));
        file.add(action(t("Load Spanish example", "Cargar ejemplo en español"), () -> loadExample(Language.SPANISH)));
        bar.add(file);

        JMenu build = new JMenu(t("Build", "Compilar"));
        build.add(compileAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, MENU_MASK));
        build.add(exportAction);
        bar.add(build);

        JMenu theme = new JMenu(t("Theme", "Tema"));
        theme.add(themeAction(t("Dark purple", "Púrpura oscuro"), FlatDarkPurpleIJTheme::new));
        theme.add(themeAction(t("Dark", "Oscuro"), FlatOneDarkIJTheme::new));
        theme.add(themeAction(t("Light", "Claro"), FlatAtomOneLightIJTheme::new));
        theme.add(themeAction(t("Fuchsia", "Fucsia"), FlatGradiantoDarkFuchsiaIJTheme::new));
        theme.add(themeAction(t("Orange", "Naranja"), FlatArcOrangeIJTheme::new));
        bar.add(theme);

        JMenu language = new JMenu("Language / Idioma");
        ButtonGroup group = new ButtonGroup();
        for (Language option : Language.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(option == Language.ENGLISH ? "English" : "Español");
            item.setSelected(option == ui);
            item.addActionListener(e -> setUiLanguage(option));
            group.add(item);
            language.add(item);
        }
        bar.add(language);

        JMenu help = new JMenu(t("Help", "Ayuda"));
        help.add(action(t("Language reference", "Referencia del lenguaje"), this::showLanguageReference));
        help.add(action(t("Exporting to MySQL", "Exportar a MySQL"), this::showExportHelp));
        help.add(action(t("About", "Acerca de"), this::showAbout));
        bar.add(help);
        return bar;
    }

    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(action(t("Open", "Abrir"), this::openFile));
        bar.add(action(t("Save", "Guardar"), this::saveProgram));
        bar.addSeparator();
        JButton compile = bar.add(compileAction);
        compile.putClientProperty("JButton.buttonType", "default");
        bar.add(exportAction);
        bar.addSeparator();
        bar.add(action(t("Clear", "Limpiar"), this::clearAll));
        bar.add(action(t("Example", "Ejemplo"), () -> loadExample(ui)));
        return bar;
    }

    private JComponent buildWorkspace() {
        JScrollPane editorScroll = new JScrollPane(editor);
        editorScroll.setRowHeaderView(new LineNumberGutter(editor));

        JPanel left = titled(editorTitle, editorScroll);

        outputTabs.addTab("SQL", new JScrollPane(sqlOutput));
        outputTabs.addTab("Schema", new JScrollPane(schemaOutput));
        outputTabs.addTab("Java (" + JavaCrudGenerator.CLASS_NAME + ".java)", new JScrollPane(javaOutput));
        JPanel right = titled(outputTitle, outputTabs);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.45);
        split.setContinuousLayout(true);
        split.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return split;
    }

    private JComponent buildStatusBar() {
        status.setBorder(BorderFactory.createEmptyBorder(4, 12, 6, 12));
        return status;
    }

    private void bindShortcuts() {
        KeyStroke ctrlEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
        editor.getInputMap().put(ctrlEnter, "compile");
        editor.getActionMap().put("compile", compileAction);
    }

    // ---------------------------------------------------------------------------------------
    // Compiler integration (messages follow the program's language)
    // ---------------------------------------------------------------------------------------

    private void sourceChanged() {
        exportAction.setEnabled(false);
        validationTimer.restart();
    }

    /** Live validation: highlights the first error without touching the outputs. */
    private void validateSource() {
        try {
            Schema schema = NaturalLanguageCompiler.parse(editor.getText());
            clearErrorHighlight();
            showStatus(schema.language().pick(
                    "✔ Programa válido: " + schema.tables().size() + " tabla(s). Presiona " + compileAction.getValue(Action.NAME) + ".",
                    "✔ Valid program: " + schema.tables().size() + " table(s). Press " + compileAction.getValue(Action.NAME) + "."),
                    OK_COLOR);
        } catch (CompilationException e) {
            highlightError(e);
        }
    }

    private void compile() {
        validationTimer.stop();
        try {
            Schema schema = NaturalLanguageCompiler.parse(editor.getText());
            clearErrorHighlight();
            sqlOutput.setText(sqlGenerator.generate(schema));
            schemaOutput.setText(schemaSummary.describe(schema));
            javaOutput.setText(javaGenerator.generate(schema));
            sqlOutput.setCaretPosition(0);
            javaOutput.setCaretPosition(0);
            exportAction.setEnabled(true);
            showStatus(schema.language().pick(
                    "✔ Compilado: base de datos '" + schema.databaseName() + "' con " + schema.tables().size() + " tabla(s).",
                    "✔ Compiled: database '" + schema.databaseName() + "' with " + schema.tables().size() + " table(s)."),
                    OK_COLOR);
        } catch (CompilationException e) {
            exportAction.setEnabled(false);
            highlightError(e);
            editor.requestFocusInWindow();
        }
    }

    private void highlightError(CompilationException e) {
        clearErrorHighlight();
        SourceSpan span = e.span();
        if (span.isKnown()) {
            int length = editor.getDocument().getLength();
            int start = Math.min(span.startOffset(), length);
            int end = Math.min(Math.max(span.endOffset(), start + 1), length);
            try {
                editor.getHighlighter().addHighlight(start, end, errorPainter);
            } catch (BadLocationException ignored) {
                // The text changed between parsing and highlighting; the next validation fixes it.
            }
        }
        showStatus("✖ " + e.describe(), ERROR_COLOR);
    }

    private void clearErrorHighlight() {
        editor.getHighlighter().removeAllHighlights();
    }

    private void exportToMySql() {
        String sql = sqlOutput.getText();
        DatabaseConfig config = DatabaseConfig.fromEnvironment();
        int answer = JOptionPane.showConfirmDialog(this,
                t("The generated SQL will run on:", "Se ejecutará el SQL generado en:") + "\n"
                        + config.url() + "  (" + t("user", "usuario") + ": " + config.user() + ")\n\n"
                        + t("Continue?", "¿Continuar?"),
                t("Export to MySQL", "Exportar a MySQL"), JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        exportAction.setEnabled(false);
        showStatus(t("Connecting to MySQL…", "Conectando a MySQL…"), null);

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return new MySqlExporter(config).export(sql);
            }

            @Override
            protected void done() {
                exportAction.setEnabled(true);
                try {
                    int count = get();
                    showStatus(t("✔ Exported to MySQL: " + count + " statement(s) executed.",
                            "✔ Exportado a MySQL: " + count + " sentencia(s) ejecutadas."), OK_COLOR);
                    JOptionPane.showMessageDialog(MainWindow.this,
                            t("Database created successfully.", "Base de datos creada con éxito."));
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    showStatus("✖ " + t("Export failed: ", "Error al exportar: ") + cause.getMessage(), ERROR_COLOR);
                    JOptionPane.showMessageDialog(MainWindow.this,
                            t("Could not export to MySQL:", "No se pudo exportar a MySQL:") + "\n" + cause.getMessage()
                                    + "\n\n" + t("See Help → Exporting to MySQL to configure the connection.",
                                    "Revisa Ayuda → Exportar a MySQL para configurar la conexión."),
                            t("Connection error", "Error de conexión"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------------------------------
    // Files
    // ---------------------------------------------------------------------------------------

    private void openFile() {
        fileChooser.setFileFilter(new FileNameExtensionFilter(t("Programs (.txt, .nlsql)", "Programas (.txt, .nlsql)"), "txt", "nlsql"));
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = fileChooser.getSelectedFile().toPath();
        try {
            editor.setText(Files.readString(path, StandardCharsets.UTF_8));
            editor.setCaretPosition(0);
            setTitle("NL2SQL IDE — " + path.getFileName());
        } catch (IOException e) {
            showError(t("Could not open the file", "No se pudo abrir el archivo"), e);
        }
    }

    private void saveProgram() {
        saveText(editor.getText(), t("program.txt", "programa.txt"));
    }

    private void saveText(String text, String suggestedName) {
        fileChooser.resetChoosableFileFilters();
        fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), suggestedName));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = fileChooser.getSelectedFile().toPath();
        if (Files.exists(path) && JOptionPane.showConfirmDialog(this,
                path.getFileName() + t(" already exists. Replace it?", " ya existe. ¿Reemplazarlo?"),
                t("Save", "Guardar"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            Files.writeString(path, text, StandardCharsets.UTF_8);
            showStatus(t("Saved to ", "Guardado en ") + path, OK_COLOR);
        } catch (IOException e) {
            showError(t("Could not save the file", "No se pudo guardar el archivo"), e);
        }
    }

    private void loadExample(Language language) {
        String resource = language == Language.ENGLISH ? "example.txt" : "ejemplo.txt";
        try (InputStream in = MainWindow.class.getResourceAsStream("/io/github/leon004/nl2sql/" + resource)) {
            if (in != null) {
                editor.setText(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                editor.setCaretPosition(0);
            }
        } catch (IOException e) {
            showError(t("Could not load the example", "No se pudo cargar el ejemplo"), e);
        }
    }

    private void clearAll() {
        editor.setText("");
        sqlOutput.setText("");
        schemaOutput.setText("");
        javaOutput.setText("");
        clearErrorHighlight();
        showStatus(" ", null);
    }

    // ---------------------------------------------------------------------------------------
    // Help dialogs
    // ---------------------------------------------------------------------------------------

    private void showLanguageReference() {
        JTextArea text = codeArea(false);
        text.setText("""
                Structure / Estructura
                ----------------------
                English          Español                SQL
                create <db>      crear <base>           CREATE DATABASE <db>
                use <db>         usar <base>            USE <db>
                table <t> begin  tabla <t> inicio       CREATE TABLE <t> (<t>_key INT AUTO_INCREMENT PRIMARY KEY, …
                    <col> <type>     <columna> <tipo>       <col> <SQL type>
                end              fin                    )
                close            cerrar                 end of program / fin del programa

                Data types / Tipos de dato
                --------------------------
                number     numeros     INT
                text       letras      VARCHAR(200)
                decimal    decimal     DECIMAL(10,2)
                date       fecha       DATE
                boolean    condicion   BOOLEAN
                foreign    foranea     <t>_key INT + FOREIGN KEY → <t>(<t>_key)
                                       (table must be declared earlier / la tabla debe declararse antes)

                Notes / Notas
                -------------
                • You can mix English and Spanish. Errors use the program's main language.
                  Puedes mezclar inglés y español. Los errores salen en el idioma principal del programa.
                • Keywords are case-insensitive (TABLE = table = Tabla).
                  Las palabras clave no distinguen mayúsculas.
                • # starts a comment / # inicia un comentario.
                • Ctrl+Enter compiles / compila.
                """);
        text.setColumns(96);
        text.setRows(30);
        JOptionPane.showMessageDialog(this, new JScrollPane(text),
                t("Language reference", "Referencia del lenguaje"), JOptionPane.PLAIN_MESSAGE);
    }

    private void showExportHelp() {
        JOptionPane.showMessageDialog(this, t("""
                "Export to MySQL" runs the generated SQL on a MySQL server.
                Configure the connection with environment variables (or -D when launching):

                  NL2SQL_DB_URL       (default jdbc:mysql://localhost:3306/)
                  NL2SQL_DB_USER      (default root)
                  NL2SQL_DB_PASSWORD  (default empty)

                Then open the database in MySQL Workbench or phpMyAdmin.""", """
                "Exportar a MySQL" ejecuta el SQL generado en un servidor MySQL.
                Configura la conexión con variables de entorno (o -D al ejecutar):

                  NL2SQL_DB_URL       (por defecto jdbc:mysql://localhost:3306/)
                  NL2SQL_DB_USER      (por defecto root)
                  NL2SQL_DB_PASSWORD  (por defecto vacío)

                Después puedes ver la base de datos en MySQL Workbench o phpMyAdmin."""),
                t("Exporting to MySQL", "Exportar a MySQL"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this, t("""
                NL2SQL IDE
                Natural-language (English / Spanish) to SQL and Java compiler, built with ANTLR 3.

                Leonardo Calderón Sastre — github.com/leon004""", """
                NL2SQL IDE
                Compilador de lenguaje natural (inglés / español) a SQL y Java, construido con ANTLR 3.

                Leonardo Calderón Sastre — github.com/leon004"""),
                t("About", "Acerca de"), JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private Action themeAction(String name, Supplier<FlatLaf> theme) {
        return action(name, () -> {
            FlatLaf.setup(theme.get());
            FlatLaf.updateUI();
        });
    }

    private void showStatus(String message, Color color) {
        status.setText(message);
        status.setForeground(color != null ? color : UIManager.getColor("Label.foreground"));
    }

    private void showError(String title, Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private static JLabel panelTitle() {
        JLabel label = new JLabel();
        label.putClientProperty("FlatLaf.styleClass", "h4");
        label.setBorder(BorderFactory.createEmptyBorder(4, 2, 6, 2));
        return label;
    }

    private static JPanel titled(JLabel title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(title, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private static JTextArea codeArea(boolean editable) {
        JTextArea area = new JTextArea();
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setTabSize(4);
        area.setEditable(editable);
        area.setMargin(new Insets(6, 8, 6, 8));
        return area;
    }

    private static Action action(String name, Runnable body) {
        return new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                body.run();
            }
        };
    }
}
