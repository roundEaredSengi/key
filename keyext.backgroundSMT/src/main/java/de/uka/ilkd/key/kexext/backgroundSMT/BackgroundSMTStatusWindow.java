package de.uka.ilkd.key.kexext.backgroundSMT;

import de.uka.ilkd.key.proof.Node;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;

public class BackgroundSMTStatusWindow extends JDialog {

    private static final Dimension DIM = new Dimension(500, 600);
    private static final String TITLE = "Background SMT Status";

    private final HashMap<BackgroundSolverRunner, BackgroundSMTRunnerStatus> status = new HashMap<>();

    public BackgroundSMTStatusWindow(Window parent) {
        super(parent);
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
        this.setSize(DIM);
        this.setLocationRelativeTo(parent);
        this.setTitle(TITLE);
    }

    public void refresh(BackgroundSolverRunner runner) {
        this.setTitle(TITLE + ": " + runner.getProof().name());
        this.setContentPane(getStatus(runner));
        this.repaint();
    }

    private BackgroundSMTRunnerStatus getStatus(BackgroundSolverRunner runner) {
        return status.computeIfAbsent(runner, BackgroundSMTRunnerStatus::new);
    }

    static class BackgroundSMTRunnerStatus extends JPanel implements RunnerListener {

        private final Map<Node, JCheckBox> nodeBoxes = new HashMap<>();
        private final JTable finishedNodeTable;
        private final BackgroundSolverRunner runner;
        private final TableModel model;
        private final JCheckBox selectAllBox;
        private final JButton applyButton;

        public BackgroundSMTRunnerStatus(BackgroundSolverRunner runner) {
            this.runner = runner;
            this.selectAllBox = new JCheckBox("Select All");
            this.applyButton = new JButton("Apply");
            this.model = new AbstractTableModel() {

                @Override
                public int getColumnCount() { return 2; }

                @Override
                public int getRowCount() { return runner.getSolvedNodes().size();}

                @Override
                public Component getValueAt(int row, int col) {
                    if (row > runner.getSolvedNodes().size()) {
                        // TODO logger
                        return null;
                    }
                    if (col == 0) {
                        JTextField node = new JTextField("Node " + runner.getSolvedNodes().get(row).getStepIndex()) {
                            @Override
                            public String toString() {
                                return getText();
                            }
                        };
                        node.setEditable(false);
                        return node;
                    } else {
                        JCheckBox checkBox = nodeBoxes.get(runner.getSolvedNodes().get(row));
                        if (checkBox == null) {
                            checkBox = new JCheckBox();
                            checkBox.setSelected(true);
                            nodeBoxes.put(runner.getSolvedNodes().get(row), checkBox);
                        }
                        return checkBox;
                    }
                }

                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return columnIndex != 0;
                }
            };
            this.finishedNodeTable = new JTable(model);
            finishedNodeTable.getColumnModel().getColumn(1).setCellRenderer(
                new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus,
                                                               int row, int column) {

                    return (Component) model.getValueAt(row, column);
                }
            });
            finishedNodeTable.getColumnModel().getColumn(0).setHeaderValue("Proved Node");
            finishedNodeTable.getColumnModel().getColumn(1).setHeaderValue("Select Nodes To Close");
            finishedNodeTable.getColumnModel().getColumn(1).setCellEditor(new TableCellEditor() {
                @Override
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    return (Component) model.getValueAt(row, column);
                }

                @Override
                public Object getCellEditorValue() {
                    return null;
                }

                @Override
                public boolean isCellEditable(EventObject anEvent) {
                    return true;
                }

                @Override
                public boolean shouldSelectCell(EventObject anEvent) {
                    return false;
                }

                @Override
                public boolean stopCellEditing() {
                    return true;
                }

                @Override
                public void cancelCellEditing() {

                }

                @Override
                public void addCellEditorListener(CellEditorListener l) {

                }

                @Override
                public void removeCellEditorListener(CellEditorListener l) {

                }
            });
            JScrollPane sp = new JScrollPane();
            sp.setViewportView(finishedNodeTable);
            this.add(sp, BorderLayout.CENTER);
            JMenuBar mb = new JMenuBar();
            mb.add(selectAllBox, BorderLayout.EAST);
            mb.add(applyButton, BorderLayout.EAST);
            this.add(mb, BorderLayout.SOUTH);
            runner.addListener(this);
        }

        @Override
        public void runnerRefresh() {
            finishedNodeTable.repaint();
            this.repaint();
        }
    }

}
