package io.github.leon004.nl2sql.ui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Line numbers shown in the editor's row header. */
final class LineNumberGutter extends JComponent {

    private final JTextArea editor;

    LineNumberGutter(JTextArea editor) {
        this.editor = editor;
        setFont(editor.getFont());
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(); }
        });
    }

    private void refresh() {
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(editor.getFont());
        int digits = Math.max(3, String.valueOf(editor.getLineCount()).length());
        Insets in = editor.getInsets();
        return new Dimension(fm.charWidth('0') * digits + 16,
                editor.getLineCount() * fm.getHeight() + in.top + in.bottom);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Rectangle clip = g.getClipBounds();
        g.setColor(UIManager.getColor("Panel.background"));
        g.fillRect(clip.x, clip.y, clip.width, clip.height);
        g.setFont(editor.getFont());
        g.setColor(UIManager.getColor("Label.disabledForeground"));

        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();
        int top = editor.getInsets().top;
        int first = Math.max(0, (clip.y - top) / lineHeight);
        int last = Math.min(editor.getLineCount() - 1, (clip.y + clip.height - top) / lineHeight);
        for (int line = first; line <= last; line++) {
            String label = String.valueOf(line + 1);
            int x = getWidth() - fm.stringWidth(label) - 8;
            int y = top + line * lineHeight + fm.getAscent();
            g.drawString(label, x, y);
        }
    }
}
