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
import io.github.leon004.nl2sql.model.Schema;
import io.github.leon004.nl2sql.model.SourceSpan;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
 */
public final class MainWindow extends JFrame {

    private static final String TITLE = "NL2SQL IDE — Generador de código SQL y Java";
    private static final Color OK_COLOR = new Color(0x3FA34D);
    private static final Color ERROR_COLOR = new Color(0xE5484D);
    private static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    private final JTextArea editor = codeArea(true);
    private final JTextArea sqlOutput = codeArea(false);
    private final JTextArea schemaOutput = codeArea(false);
    private final JTextArea javaOutput = codeArea(false);
    private final JTabbedPane outputTabs = new JTabbedPane();
    private final JLabel status = new JLabel(" ");
    private final JFileChooser fileChooser = new JFileChooser();
    private final Highlighter.HighlightPainter errorPainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(0xE5, 0x48, 0x4D, 110));
    private final Timer validationTimer = new Timer(300, e -> validateSource());

    private final SqlGenerator sqlGenerator = new SqlGenerator();
    private final SchemaSummary schemaSummary = new SchemaSummary();
    private final JavaCrudGenerator javaGenerator = new JavaCrudGenerator();

    private final Action compileAction = action("Compilar", "Compilar a SQL y Java (Ctrl+Enter)", this::compile);
    private final Action exportAction = action("Exportar a MySQL", "Ejecutar el SQL generado en MySQL", this::exportToMySql);

    public MainWindow() {
        super(TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 600));
        setPreferredSize(new Dimension(1280, 760));

        validationTimer.setRepeats(false);
        exportAction.setEnabled(false);

        setJMenuBar(buildMenuBar());
        add(buildToolBar(), BorderLayout.NORTH);
        add(buildWorkspace(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        bindShortcuts();

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { sourceChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { sourceChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { sourceChanged(); }
        });

        loadExample();
        pack();
    }

    // ---------------------------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------------------------

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("Archivo");
        file.add(action("Abrir…", null, this::openFile)).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_MASK));
        file.add(action("Guardar programa…", null, () -> saveText(editor.getText(), "programa.txt")))
                .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MASK));
        file.addSeparator();
        file.add(action("Guardar SQL…", null, () -> saveText(sqlOutput.getText(), "script.sql")));
        file.add(action("Guardar Java…", null, () -> saveText(javaOutput.getText(), JavaCrudGenerator.CLASS_NAME + ".java")));
        file.addSeparator();
        file.add(action("Cargar ejemplo (español)", null, () -> loadExample("ejemplo.txt")));
        file.add(action("Load example (English)", null, () -> loadExample("example.txt")));
        bar.add(file);

        JMenu build = new JMenu("Compilar");
        build.add(compileAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, MENU_MASK));
        build.add(exportAction);
        bar.add(build);

        JMenu theme = new JMenu("Tema");
        theme.add(themeAction("Púrpura oscuro", FlatDarkPurpleIJTheme::new));
        theme.add(themeAction("Oscuro", FlatOneDarkIJTheme::new));
        theme.add(themeAction("Claro", FlatAtomOneLightIJTheme::new));
        theme.add(themeAction("Fucsia", FlatGradiantoDarkFuchsiaIJTheme::new));
        theme.add(themeAction("Naranja", FlatArcOrangeIJTheme::new));
        bar.add(theme);

        JMenu help = new JMenu("Ayuda");
        help.add(action("Referencia del lenguaje", null, this::showLanguageReference));
        help.add(action("Exportar a MySQL", null, this::showExportHelp));
        help.add(action("Acerca de", null, this::showAbout));
        bar.add(help);
        return bar;
    }

    private JToolBar buildToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(action("Abrir", "Abrir un programa (.txt)", this::openFile));
        toolBar.add(action("Guardar", "Guardar el programa", () -> saveText(editor.getText(), "programa.txt")));
        toolBar.addSeparator();
        JButton compile = toolBar.add(compileAction);
        compile.putClientProperty("JButton.buttonType", "default");
        toolBar.add(exportAction);
        toolBar.addSeparator();
        toolBar.add(action("Limpiar", "Borrar el editor y las salidas", this::clearAll));
        toolBar.add(action("Ejemplo", "Cargar un programa de ejemplo", this::loadExample));
        return toolBar;
    }

    private JComponent buildWorkspace() {
        JScrollPane editorScroll = new JScrollPane(editor);
        editorScroll.setRowHeaderView(new LineNumberGutter(editor));

        JPanel left = titled("Lenguaje natural", editorScroll);

        outputTabs.addTab("SQL", new JScrollPane(sqlOutput));
        outputTabs.addTab("Esquema", new JScrollPane(schemaOutput));
        outputTabs.addTab("Java (" + JavaCrudGenerator.CLASS_NAME + ".java)", new JScrollPane(javaOutput));
        JPanel right = titled("Código generado", outputTabs);

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
    // Compiler integration
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
                    "✔ Programa válido: " + schema.tables().size() + " tabla(s). Presiona Compilar.",
                    "✔ Valid program: " + schema.tables().size() + " table(s). Press Compilar to compile."), OK_COLOR);
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
                "Se ejecutará el SQL generado en:\n" + config.url() + "  (usuario: " + config.user() + ")\n\n¿Continuar?",
                "Exportar a MySQL", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        exportAction.setEnabled(false);
        showStatus("Conectando a MySQL…", UIManager.getColor("Label.foreground"));

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
                    showStatus("✔ Exportado a MySQL: " + count + " sentencia(s) ejecutadas.", OK_COLOR);
                    JOptionPane.showMessageDialog(MainWindow.this, "Base de datos creada con éxito.");
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    showStatus("✖ Error al exportar: " + cause.getMessage(), ERROR_COLOR);
                    JOptionPane.showMessageDialog(MainWindow.this,
                            "No se pudo exportar a MySQL:\n" + cause.getMessage()
                                    + "\n\nRevisa Ayuda → Exportar a MySQL para configurar la conexión.",
                            "Error de conexión", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------------------------------
    // Files
    // ---------------------------------------------------------------------------------------

    private void openFile() {
        fileChooser.setFileFilter(new FileNameExtensionFilter("Programas (.txt, .nlsql)", "txt", "nlsql"));
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = fileChooser.getSelectedFile().toPath();
        try {
            editor.setText(Files.readString(path, StandardCharsets.UTF_8));
            editor.setCaretPosition(0);
            setTitle(TITLE + " — " + path.getFileName());
        } catch (IOException e) {
            showError("No se pudo abrir el archivo", e);
        }
    }

    private void saveText(String text, String suggestedName) {
        fileChooser.resetChoosableFileFilters();
        fileChooser.setSelectedFile(new java.io.File(fileChooser.getCurrentDirectory(), suggestedName));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = fileChooser.getSelectedFile().toPath();
        if (Files.exists(path) && JOptionPane.showConfirmDialog(this,
                path.getFileName() + " ya existe. ¿Reemplazarlo?", "Guardar",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            Files.writeString(path, text, StandardCharsets.UTF_8);
            showStatus("Guardado en " + path, OK_COLOR);
        } catch (IOException e) {
            showError("No se pudo guardar el archivo", e);
        }
    }

    private void loadExample() {
        loadExample("ejemplo.txt");
    }

    private void loadExample(String resource) {
        try (InputStream in = MainWindow.class.getResourceAsStream("/io/github/leon004/nl2sql/" + resource)) {
            if (in != null) {
                editor.setText(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                editor.setCaretPosition(0);
            }
        } catch (IOException e) {
            showError("No se pudo cargar el ejemplo", e);
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
                Estructura / Structure
                ----------------------
                Español                English          SQL
                crear <base>           create <db>      CREATE DATABASE <db>
                usar <base>            use <db>         USE <db>
                tabla <t> inicio       table <t> begin  CREATE TABLE <t> (<t>_key INT AUTO_INCREMENT PRIMARY KEY, …
                    <columna> <tipo>       <col> <type>     <col> <SQL type>
                fin                    end              )
                cerrar                 close            fin del programa / end of program

                Tipos de dato / Data types
                --------------------------
                numeros     number     INT
                letras      text       VARCHAR(200)
                decimal     decimal    DECIMAL(10,2)
                fecha       date       DATE
                condicion   boolean    BOOLEAN
                foranea     foreign    <t>_key INT + FOREIGN KEY → <t>(<t>_key)
                                       (la tabla debe declararse antes / table must be declared earlier)

                Notas / Notes
                -------------
                • Puedes mezclar español e inglés. Los errores salen en el idioma principal del programa.
                  You can mix Spanish and English. Errors use the program's main language.
                • Las palabras clave no distinguen mayúsculas (Tabla = tabla = TABLE).
                  Keywords are case-insensitive.
                • # inicia un comentario / # starts a comment.
                • Ctrl+Enter compila / compiles.
                """);
        text.setColumns(96);
        text.setRows(30);
        JOptionPane.showMessageDialog(this, new JScrollPane(text), "Referencia del lenguaje", JOptionPane.PLAIN_MESSAGE);
    }

    private void showExportHelp() {
        JOptionPane.showMessageDialog(this, """
                "Exportar a MySQL" ejecuta el SQL generado en un servidor MySQL.
                Configura la conexión con variables de entorno (o -D al ejecutar):

                  NL2SQL_DB_URL       (por defecto jdbc:mysql://localhost:3306/)
                  NL2SQL_DB_USER      (por defecto root)
                  NL2SQL_DB_PASSWORD  (por defecto vacío)

                Después puedes ver la base de datos en MySQL Workbench o phpMyAdmin.""",
                "Exportar a MySQL", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this, """
                NL2SQL IDE
                Compilador de lenguaje natural (español / English) a SQL y Java, construido con ANTLR 3.
                Natural-language (Spanish / English) to SQL and Java compiler, built with ANTLR 3.

                Leonardo Calderón Sastre — github.com/leon004""",
                "Acerca de", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private Action themeAction(String name, Supplier<FlatLaf> theme) {
        return action(name, null, () -> {
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

    private static JPanel titled(String title, JComponent content) {
        JLabel label = new JLabel(title);
        label.putClientProperty("FlatLaf.styleClass", "h4");
        label.setBorder(BorderFactory.createEmptyBorder(4, 2, 6, 2));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private static JTextArea codeArea(boolean editable) {
        JTextArea area = new JTextArea();
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setTabSize(4);
        area.setEditable(editable);
        area.setMargin(new java.awt.Insets(6, 8, 6, 8));
        return area;
    }

    private static Action action(String name, String tooltip, Runnable body) {
        Action action = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                body.run();
            }
        };
        if (tooltip != null) {
            action.putValue(Action.SHORT_DESCRIPTION, tooltip);
        }
        return action;
    }
}
