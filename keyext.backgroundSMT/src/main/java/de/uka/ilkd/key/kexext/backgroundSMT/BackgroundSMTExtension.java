package de.uka.ilkd.key.kexext.backgroundSMT;

import de.uka.ilkd.key.core.KeYMediator;
import de.uka.ilkd.key.core.KeYSelectionEvent;
import de.uka.ilkd.key.core.KeYSelectionListener;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.extension.api.KeYGuiExtension;
import de.uka.ilkd.key.gui.fonticons.FontAwesomeSolid;
import de.uka.ilkd.key.gui.fonticons.IconFactory;
import de.uka.ilkd.key.gui.fonticons.IconFontProvider;
import de.uka.ilkd.key.gui.settings.InvalidSettingsInputException;
import de.uka.ilkd.key.gui.settings.SettingsProvider;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.rule.IBuiltInRuleApp;
import de.uka.ilkd.key.smt.RuleAppSMT;
import de.uka.ilkd.key.smt.SMTSolverResult;
import de.uka.ilkd.key.smt.solvertypes.SolverType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

@KeYGuiExtension.Info(experimental = false, name = "BackgroundSMT")
public class BackgroundSMTExtension implements KeYGuiExtension, KeYGuiExtension.Startup,
    KeYGuiExtension.Settings, KeYGuiExtension.StatusLine, KeYSelectionListener {

    private static final Icon ICON = IconFactory.get(
        new IconFontProvider(FontAwesomeSolid.FAST_FORWARD), 12);

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundSMTExtension.class);

    private final Set<BackgroundSolverRunner> runners = new HashSet<>();

    private double timeout;
    private Collection<SolverType> solvers = new ArrayList<>();

    private KeYMediator mediator;

    private BackgroundSolverRunner runner;

    private BackgroundSMTStyler styler;

    private JButton statusButton = new JButton();

    @Override
    public void init(MainWindow window, KeYMediator mediator) {
        this.styler = new BackgroundSMTStyler(this);
        window.getProofTreeView().getRenderer().add(styler);
        this.mediator = mediator;
        mediator.addKeYSelectionListener(this);
        try {
            new BackgroundSMTSettingsProvider(this).applySettings(window);
        } catch (InvalidSettingsInputException e) {
            LOGGER.warn("Invalid background SMT settings, using defaults.");
        }
        statusButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new BackgroundSMTStatusWindow().setVisible(true);
            }
        });
        statusButton.setIcon(ICON);
    }

    public boolean canApply(Node node) {
        Optional<SMTSolverResult> result = runner.getCachedResult(node.sequent());
        return runner != null
            && node.childrenCount() == 0
            && !node.isClosed()
            && result.isPresent()
            && result.get().isValid() == SMTSolverResult.ThreeValuedTruth.VALID;
    }

    public void setRunner(BackgroundSolverRunner runner) {
        /*if (!runnerStatus.keySet().contains(runner)) {
            runnerStatus.put(runner, !runner.getSolvedProblems().isEmpty());
        }*/
        runners.add(runner);
        runner.setSolvers(solvers);
        runner.setTimeout(timeout);
        this.runner = runner;
    }

    @Override
    public void selectedNodeChanged(KeYSelectionEvent e) {
        Proof proof = e.getSource().getSelectedProof();
        // The current runner can be stopped if another one is needed.
        // TODO really just stop the runner here?
        //  What if it is close to finishing a proof and we just wanted to look at another proof shortly?
        if (runner != null && proof != runner.getProof()) {
            runner.stopLaunchedSolvers();
        }
        // If there already is a runner for the selected proof, make that active.
        Optional<BackgroundSolverRunner> existingRunner = runners.stream().filter(r -> r.getProof() == proof).findFirst();
        if (existingRunner.isEmpty()) {
            setRunner(new BackgroundSolverRunner(this, proof, mediator));
        }
    }

    @Override
    public void selectedProofChanged(KeYSelectionEvent e) {
        Proof proof = e.getSource().getSelectedProof();
        // The current runner can be stopped if another one is needed.
        // TODO really just stop the runner here?
        //  What if it is close to finishing a proof and we just wanted to look at another proof shortly?
        if (runner != null && proof != runner.getProof()) {
            runner.stopLaunchedSolvers();
        }
        // If there already is a runner for the selected proof, make that active.
        Optional<BackgroundSolverRunner> existingRunner = runners.stream().filter(r -> r.getProof() == proof).findFirst();
        if (existingRunner.isEmpty()) {
            setRunner(new BackgroundSolverRunner(this, proof, mediator));
        }
    }

    public void refresh() {
        MainWindow.getInstance().getProofTreeView().repaint();
    }

    public void applyRunner(Node node) {
        if (runner == null || mediator.getSelectedProof() != runner.getProof() || !runner.getProof().find(node)) {
            return;
        }
        Optional<SMTSolverResult> result = runner.getCachedResult(node.sequent());
        if (result.isEmpty() || result.get().isValid() != SMTSolverResult.ThreeValuedTruth.VALID) {
            return;
        }
        if (!mediator.getSelectedProof().isGoal(node)) {
            mediator.getSelectedProof().pruneProof(node);
        }
        Goal nodeGoal = mediator.getSelectedProof().getGoal(node);
        KeYMediator mediator = MainWindow.getInstance().getMediator();
        mediator.stopInterface(true);
        try {
            IBuiltInRuleApp app =
                            RuleAppSMT.rule.createApp(null).setTitle(node.name());
                    nodeGoal.apply(app);
        } finally {
            mediator.startInterface(true);
        }
    }

    @Override
    public SettingsProvider getSettings() {
        return new BackgroundSMTSettingsProvider(this);
    }

    public void setTimeout(double timeout) {
        this.timeout = timeout;
        for (BackgroundSolverRunner r : runners) {
            r.setTimeout(timeout);
        }
    }

    public void setSolvers(Collection<SolverType> checkedTypes) {
        this.solvers.clear();
        this.solvers.addAll(checkedTypes);
        for (BackgroundSolverRunner r : runners) {
            r.setSolvers(checkedTypes);
        }
    }

    @Override
    public List<JComponent> getStatusLineComponents() {
        return List.of(statusButton);
    }
}
