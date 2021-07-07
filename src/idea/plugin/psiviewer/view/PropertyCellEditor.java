package idea.plugin.psiviewer.view;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.util.PlatformIcons;
import org.apache.batik.util.Messages;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

/**
 * PropertyCellEditor for dummy edit the table cell value and allow copy partical content of the string.
 *
 * @author beansoft@126.com
 */
public class PropertyCellEditor implements TableCellEditor, ActionListener {
    JButton button = new JButton(PlatformIcons.COPY_ICON); //$NON-NLS-1$
    JTextField textField = new JTextField();

    JPanel editPanel = new JPanel();

    Component parent;

    Object eventSource;

    Object value;

    /** Event listeners */
    protected EventListenerList listenerList = new EventListenerList();

    transient protected ChangeEvent changeEvent = null;

    public PropertyCellEditor() {
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        button.setOpaque(false);
        button.setPreferredSize(new Dimension(16, 16));
        button.addActionListener(this);
        button.setToolTipText("Copy to Clipboard");
        textField.addActionListener(this);
        textField.setBorder(null);

        editPanel.setLayout(new BorderLayout());
        editPanel.add(textField, BorderLayout.CENTER);
        editPanel.add(button, BorderLayout.EAST);
    }

    public void actionPerformed(ActionEvent e) {
        eventSource = e.getSource();
        if (eventSource == textField) {
            stopCellEditing();
            return;
        }

        CopyPasteManager.getInstance().setContents(new StringSelection(getCellEditorValue() + ""));

    }

    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        this.parent = table;
        this.value = value;
        this.textField.setText(value + ""); //$NON-NLS-1$
        this.eventSource = null;
        textField.setFont(table.getFont());

        button.setVisible(true);

        return editPanel;
    }

    /** Returns the value contained in the editor without change anything* */
    public Object getCellEditorValue() {
        return value;
    }

    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    public void addCellEditorListener(CellEditorListener l) {
        listenerList.add(CellEditorListener.class, l);
    }

    public void removeCellEditorListener(CellEditorListener l) {
        listenerList.remove(CellEditorListener.class, l);
    }

    /*
     * Notify all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * parameters passed into the fire method.
     *
     * @see EventListenerList
     */
    protected void fireEditingCanceled() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CellEditorListener.class) {
                // Lazily create the event:
                if (changeEvent == null)
                    changeEvent = new ChangeEvent(this);
                ((CellEditorListener) listeners[i + 1])
                        .editingCanceled(changeEvent);
            }
        }
    }

    /*
     * Notify all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * parameters passed into the fire method.
     *
     * @see EventListenerList
     */
    protected void fireEditingStopped() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CellEditorListener.class) {
                // Lazily create the event:
                if (changeEvent == null)
                    changeEvent = new ChangeEvent(this);
                ((CellEditorListener) listeners[i + 1])
                        .editingStopped(changeEvent);
            }
        }
    }
}