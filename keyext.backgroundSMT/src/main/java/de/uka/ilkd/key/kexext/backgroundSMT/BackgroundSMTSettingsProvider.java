package de.uka.ilkd.key.kexext.backgroundSMT;

import de.uka.ilkd.key.core.Main;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.settings.InvalidSettingsInputException;
import de.uka.ilkd.key.gui.settings.SettingsPanel;
import de.uka.ilkd.key.gui.settings.SettingsProvider;
import de.uka.ilkd.key.gui.settings.Validator;
import de.uka.ilkd.key.gui.smt.settings.SMTSettingsProvider;
import de.uka.ilkd.key.settings.DefaultSMTSettings;
import de.uka.ilkd.key.settings.SettingsListener;
import de.uka.ilkd.key.smt.SMTSettings;
import de.uka.ilkd.key.smt.solvertypes.SolverType;
import de.uka.ilkd.key.smt.solvertypes.SolverTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static de.uka.ilkd.key.gui.smt.settings.SMTSettingsProvider.BUNDLE;
import static javax.swing.BoxLayout.Y_AXIS;

public class BackgroundSMTSettingsProvider extends SettingsPanel implements SettingsProvider,
    SettingsListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundSMTSettingsProvider.class);

    private static final String info = "Modification of SMT solvers that run automatically in the"
        + "background in order to speed up SMT applications.";

    private static final String BSMT = "Background SMT";

    private final Set<SolverType> checkedTypes = new HashSet<>();

    private double timeout;

    private final JTextField currentlySet;

    private final HashMap<SolverType, JCheckBox> activeCheckboxes = new HashMap<>();
    private final HashMap<SolverType, JCheckBox> allCheckboxes = new HashMap<>();

    private final JCheckBox selectAll;

    private final BackgroundSMTExtension extension;

    public BackgroundSMTSettingsProvider(BackgroundSMTExtension extension) {
        this.extension = extension;
        this.timeout = 1.0;
        this.currentlySet = createTextField("", emptyValidator());
        this.selectAll = createCheckBox("Select All", true, emptyValidator());
        currentlySet.setEditable(false);
        pNorth.add(createInfoArea(info));
        addSeparator("Choose background solvers");
        Box box = new Box(Y_AXIS);
        for (SolverType type : SolverTypes.getSolverTypes().stream().filter(
            t -> (t != SolverTypes.Z3_CE_SOLVER)
                && (Main.isExperimentalMode() || !SolverTypes.getLegacySolvers().contains(t)))
            .collect(Collectors.toList())) {
            JCheckBox checkBox = createCheckBox(type.getName(), true, emptyValidator());
            if (type.isInstalled(true)) {
                checkedTypes.add(type);
                activeCheckboxes.put(type, checkBox);
            } else {
                checkBox.setSelected(false);
                checkBox.setEnabled(false);
            }
            allCheckboxes.put(type, checkBox);
            checkBox.addChangeListener(e -> {
                if (checkBox.isSelected() && type.isInstalled(true)) {
                    checkedTypes.add(type);
                } else {
                    checkedTypes.remove(type);
                }
                selectAll.setSelected(activeCheckboxes.size() == checkedTypes.size());
            });
            box.add(checkBox);
        }
        selectAll.addActionListener(e -> {
            boolean selected = selectAll.isSelected();
            for (Map.Entry<SolverType, JCheckBox> entry : activeCheckboxes.entrySet()) {
                entry.getValue().setSelected(entry.getValue().isEnabled() && selected);
            }
        });
        box.add(new JSeparator());
        box.add(selectAll);
        addTitledComponent("", box, "");

        addSeparator("Choose background solver timeout");
        // Timeout field:
        var model = new SpinnerNumberModel(timeout, -1.0, 5.0, 1.0);
        var jsp = createNumberTextField(model, emptyValidator());
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(jsp, "#.###");
        editor.getFormat().setRoundingMode(RoundingMode.FLOOR);
        jsp.setEditor(editor);
        jsp.addChangeListener(e -> {
            timeout = ((Number) jsp.getValue()).longValue();
        });
        addTitledComponent("Timeout", jsp, "background solver timeout");
        addSeparator("");
        addTitledComponent("Current settings", currentlySet, "currently active settings");
        refreshText();
    }

    @Override
    public String getDescription() {
        return BSMT;
    }

    @Override
    public JComponent getPanel(MainWindow window) {
        return this;
    }

    @Override
    public void applySettings(MainWindow window) throws InvalidSettingsInputException {
        extension.refreshSettings();
        refreshText();
    }

    @Override
    public int getPriorityOfSettings() {
        return new SMTSettingsProvider().getPriorityOfSettings() + 1;
    }

    private void refreshText() {
        StringBuilder builder = new StringBuilder("Timeout: " + timeout + " sec; Solvers: {");
        int counter = 0;
        for (SolverType type : checkedTypes) {
            counter++;
            builder.append(type.getName());
            builder.append(counter < checkedTypes.size() ? "; " : "");
        }
        builder.append("}");
        currentlySet.setText(builder.toString());
    }

    public long getTimeout() {
        return (long) (timeout * 1000.0);
    }

    public Collection<SolverType> getSolverTypes() {
        return new ArrayList<>(checkedTypes);
    }

    @Override
    public void settingsChanged(EventObject e) {
        activeCheckboxes.clear();
        boolean allSelected = true;
        for (Map.Entry<SolverType, JCheckBox> entry : allCheckboxes.entrySet()) {
            entry.getValue().setEnabled(entry.getKey().isInstalled(true));
            if (entry.getValue().isEnabled()) {
                activeCheckboxes.put(entry.getKey(), entry.getValue());
                allSelected = allSelected && entry.getValue().isSelected();
            } else {
                entry.getValue().setSelected(false);
            }
        }
        selectAll.setSelected(allSelected);
        repaint();
    }
}
