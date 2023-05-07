package com.didalgo.intellij.chatgpt.ui;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBScrollBar;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.fields.ExpandableSupport;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.util.Function;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

public class ExpandableTextFieldExt extends ExpandableTextField {

    private final ExpandableSupport support;

    public ExpandableTextFieldExt() {
        this(
                text -> StringUtil.split(text, NewlineFilter.NEWLINE_REPLACEMENT.toString(), true, false),
                lines -> String.join(NewlineFilter.NEWLINE_REPLACEMENT.toString(), lines));
    }

    public ExpandableTextFieldExt(@NotNull Function<? super String, ? extends List<String>> parser, @NotNull Function<? super List<String>, String> joiner) {
        super(parser, joiner);
        Function<? super String, String> onShow = text -> StringUtil.join(parser.fun(text), "\n");
        Function<? super String, String> onHide = text -> joiner.fun(asList(StringUtil.splitByLines(text/*MOD AGAINST IDEA CORE*/, false/*END MOD*/)));
        support = new ExpandableSupport<JTextComponent>(this, onShow, onHide) {
            @NotNull
            @Override
            protected Content prepare(@NotNull JTextComponent field, @NotNull Function<? super String, @Nls String> onShow) {
                Font font = field.getFont();
                FontMetrics metrics = font == null ? null : field.getFontMetrics(font);
                int height = metrics == null ? 16 : metrics.getHeight();
                Dimension size = new Dimension(height * 32, height * 16);

                JTextArea area = createTextArea(onShow.fun(field.getText()), field.isEditable(), field.getBackground(), field.getForeground(), font);

                copyCaretPosition(field, area);

                JLabel label = createLabel(createCollapseExtension());
                label.setBorder(JBUI.Borders.empty(5, 0, 5, 5));

                JBScrollPane pane = new JBScrollPane(area);
                pane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
                pane.getVerticalScrollBar().add(JBScrollBar.LEADING, label);
                pane.getVerticalScrollBar().setBackground(area.getBackground());

                Insets insets = field.getInsets();
                Insets margin = field.getMargin();
                if (margin != null) {
                    insets.top += margin.top;
                    insets.left += margin.left;
                    insets.right += margin.right;
                    insets.bottom += margin.bottom;
                }

                JBInsets.addTo(size, insets);
                JBInsets.addTo(size, pane.getInsets());
                pane.setPreferredSize(size);
                pane.setViewportBorder(insets != null
                        ? createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right)
                        : createEmptyBorder());
                return new Content() {
                    @NotNull
                    @Override
                    public JComponent getContentComponent() {
                        return pane;
                    }

                    @Override
                    public JComponent getFocusableComponent() {
                        return area;
                    }

                    @Override
                    public void cancel(@NotNull Function<? super String, String> onHide) {
                        if (field.isEditable()) {
                            field.setText(onHide.fun(area.getText()));
                            copyCaretPosition(area, field);
                        }
                    }
                };
            }
        };
        setExtensions(createExtensions());
    }

    @Override
    public String getText() {
        return NewlineFilter.normalize(super.getText());
    }

    @Override
    public String getText(int offs, int len) throws BadLocationException {
        return NewlineFilter.normalize(super.getText(offs, len));
    }

    @Override
    public String getSelectedText() {
        return NewlineFilter.normalize(super.getSelectedText());
    }

    @NotNull
    protected List<ExtendableTextComponent.Extension> createExtensions() {
        return (support == null)? emptyList(): singletonList(support.createExpandExtension());
    }

    @Override
    public String getTitle() {
        return support.getTitle();
    }

    @Override
    public void setTitle(@NlsContexts.PopupTitle String title) {
        support.setTitle(title);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled) support.collapse();
        super.setEnabled(enabled);
    }

    @Override
    public void collapse() {
        support.collapse();
    }

    @Override
    public boolean isExpanded() {
        return support.isExpanded();
    }

    @Override
    public void expand() {
        support.expand();
    }

    private static void copyCaretPosition(JTextComponent source, JTextComponent destination) {
        try {
            destination.setCaretPosition(source.getCaretPosition());
        }
        catch (Exception ignored) { }
    }
}