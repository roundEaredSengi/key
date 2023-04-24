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
import de.uka.ilkd.key.proof.*;
import de.uka.ilkd.key.proof.mgt.ProofEnvironmentEvent;
import de.uka.ilkd.key.proof.mgt.ProofEnvironmentListener;
import de.uka.ilkd.key.rule.IBuiltInRuleApp;
import de.uka.ilkd.key.settings.DefaultSMTSettings;
import de.uka.ilkd.key.settings.ProofIndependentSettings;
import de.uka.ilkd.key.smt.RuleAppSMT;
import de.uka.ilkd.key.smt.SMTSettings;
import de.uka.ilkd.key.smt.SMTSolverResult;
import de.uka.ilkd.key.smt.solvertypes.SolverType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * An extension to run SMT solvers automatically in the background without the user explicitly
 * starting them.
 * This is supposed to avoid situations where an SMT solver could close a goal relatively fast
 * while the KeY strategy still fails and the user would have to do some more interactive steps.
 *
 * The extension adds a status window which shows all the goals that have been closed in the
 * background as well as buttons to apply the respective results per goal.
 */
@KeYGuiExtension.Info(experimental = false, name = "BackgroundSMT")
public class BackgroundSMTExtension implements KeYGuiExtension, KeYGuiExtension.Startup,
    KeYGuiExtension.Settings, KeYGuiExtension.StatusLine, KeYSelectionListener, RunnerListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundSMTExtension.class);

    /**
     * One background SMT runner per proof.
     */
    private final Set<BackgroundSolverRunner> runners = new HashSet<>();

    /**
     * The background SMT timeout set via {@link BackgroundSMTSettingsProvider}.
     */
    private double timeout;
    /**
     * The background SMT solvers set via {@link BackgroundSMTSettingsProvider}.
     * All of them are run on every background SMT problem that is launched.
     */
    private Collection<SolverType> solvers = new ArrayList<>();

    private KeYMediator mediator;

    private BackgroundSolverRunner runner;

    private BackgroundSMTStyler styler;

    private BackgroundSMTStatusWindow statusWindow;

    private BackgroundSMTSettingsProvider settingsProvider;

    private JButton statusButton = new JButton();

    private void setRunner(BackgroundSolverRunner runner) {
        // Refresh the status window to show the current runner's status.
        this.runner = runner;
        runner.tryForOpenGoals();
        statusWindow.refresh(runner);
    }

    private void newRunner(Proof proof) {
        DefaultSMTSettings defaultSMTSettings =
            new DefaultSMTSettings(proof.getSettings().getSMTSettings(),
            ProofIndependentSettings.DEFAULT_INSTANCE.getSMTSettings(),
            proof.getSettings().getNewSMTSettings(), proof);
        defaultSMTSettings.addListener(settingsProvider);
        BackgroundSMTSettings settings = new BackgroundSMTSettings(defaultSMTSettings);
        settings.setSolverTypes(settingsProvider.getSolverTypes());
        settings.setTimeout(settingsProvider.getTimeout());
        BackgroundSolverRunner newRunner = new BackgroundSolverRunner(proof, settings);
        this.mediator.getUI().getProofControl().addAutoModeListener(newRunner);
        runners.add(newRunner);
        setRunner(newRunner);
        statusWindow.repaint();
        runner.addListener(this);
    }


    /**
     * Create a button in the status bar that opens a {@link BackgroundSMTStatusWindow}
     * showing the status of the {@link BackgroundSolverRunner}s of the current proof.
     */
    @Override
    public void init(MainWindow window, KeYMediator mediator) {
        this.styler = new BackgroundSMTStyler(this);
        window.getProofTreeView().getRenderer().add(styler);
        this.mediator = mediator;
        mediator.addKeYSelectionListener(this);
        this.settingsProvider = (BackgroundSMTSettingsProvider) getSettings();
        try {
            settingsProvider.applySettings(window);
        } catch (InvalidSettingsInputException e) {
            LOGGER.warn("Invalid background SMT settings, using defaults.");
        }
        this.statusWindow = new BackgroundSMTStatusWindow(window);
        statusButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (runner == null) {
                    return;
                }
                statusWindow.refresh(runner);
                statusWindow.setVisible(true);
            }
        });
        statusButton.setForeground(Color.BLACK);
        statusButton.setEnabled(false);
        statusButton.setText("bSMT");
        if (mediator.getSelectedProof() != null) {
            newRunner(mediator.getSelectedProof());
        }
    }

    /**
     * Check whether the currently active runner has UNSAT results for the given node.
     * @param node
     * @return true iff the runner's {@link BackgroundSolverRunner#getSolvedNodes()} contains the
     *          given node
     */
    public boolean canApply(Node node) {
        if (runner == null) {
            return false;
        }
        return !runner.getProof().closed() && runner.getSolvedNodes().contains(node);
    }

    public void refreshSettings() {
        for (BackgroundSolverRunner r : runners) {
            DefaultSMTSettings defaultSMTSettings =
                new DefaultSMTSettings(r.getProof().getSettings().getSMTSettings(),
                ProofIndependentSettings.DEFAULT_INSTANCE.getSMTSettings(),
                r.getProof().getSettings().getNewSMTSettings(), r.getProof());
            defaultSMTSettings.addListener(settingsProvider);
            BackgroundSMTSettings settings = new BackgroundSMTSettings(defaultSMTSettings);
            settings.setTimeout(settingsProvider.getTimeout());
            settings.setSolverTypes(settingsProvider.getSolverTypes());
            r.refreshSettings(settings);
        }
    }

    /**
     * Apply the current runner to the given node.
     * Prune back the current proof to the given node if it is not a goal.
     * If the node does not belong to the current runner's proof or has not been solved yet,
     * nothing will happen.
     * @param node the node to apply background SMT results to
     */
    public void applyRunner(Node node) {
        // TODO use logger
        if (runner == null || !runner.getProof().find(node)
            || !runner.getSolvedNodes().contains(node)) {
            return;
        }
        // Prune back if the node is not a goal
        if (!runner.getProof().isGoal(node)) {
            runner.getProof().pruneProof(node);
        }
        Goal nodeGoal = runner.getProof().getGoal(node);
        // Apply SMT rule to the node/goal.
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
    public void selectedNodeChanged(KeYSelectionEvent e) {
        selectedProofChanged(e);
    }

    @Override
    public void selectedProofChanged(KeYSelectionEvent e) {
        Proof proof = e.getSource().getSelectedProof();
        if (runner != null && proof == runner.getProof()) {
            // If the current runner already belongs to the selected proof, no changes are needed.
            return;
        }
        // The current runner can be stopped if another one is needed.
        // TODO really just stop the runner here?
        //  What if it is close to finishing a proof and we just wanted to look at another proof shortly?
        if (runner != null && proof != runner.getProof()) {
            runner.stopLaunchedSolvers();
        }
        // If there already is a runner for the selected proof, make that active.
        Optional<BackgroundSolverRunner> existingRunner = runners.stream()
            .filter(r -> r.getProof() == proof).findFirst();
        if (existingRunner.isEmpty()) {
            newRunner(proof);
        } else {
            setRunner(existingRunner.get());
        }

    }

    /**
     * Refresh the status button and the proof tree view to show the current runner's
     * background SMT results.
     */
    @Override
    public void runnerRefresh() {
        if (runner.getProof().closed()) {
            runners.remove(runner);
        }
        MainWindow.getInstance().getProofTreeView().repaint();
        statusButton.setForeground(runner != null
            && runner.getSolvedNodes().isEmpty() ? Color.BLACK : Color.BLUE);
        statusButton.setEnabled(runner != null && !runner.getSolvedNodes().isEmpty());
    }

    @Override
    public SettingsProvider getSettings() {
        if (settingsProvider == null) {
            settingsProvider = new BackgroundSMTSettingsProvider(this);
        }
        return settingsProvider;
    }

    @Override
    public List<JComponent> getStatusLineComponents() {
        return List.of(statusButton);
    }

}
