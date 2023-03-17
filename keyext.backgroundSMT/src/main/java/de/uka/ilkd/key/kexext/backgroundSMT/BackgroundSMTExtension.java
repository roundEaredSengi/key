package de.uka.ilkd.key.kexext.backgroundSMT;

import de.uka.ilkd.key.core.KeYMediator;
import de.uka.ilkd.key.core.KeYSelectionEvent;
import de.uka.ilkd.key.core.KeYSelectionListener;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.extension.api.KeYGuiExtension;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.rule.IBuiltInRuleApp;
import de.uka.ilkd.key.smt.RuleAppSMT;
import de.uka.ilkd.key.smt.SMTSolverResult;

import java.util.*;

@KeYGuiExtension.Info(experimental = false, name = "BackgroundSMT")
public class BackgroundSMTExtension implements KeYGuiExtension, KeYGuiExtension.Startup, KeYSelectionListener {

    private final Set<BackgroundSolverRunner> runners = new HashSet<>();

    private KeYMediator mediator;

    private BackgroundSolverRunner runner;

    @Override
    public void init(MainWindow window, KeYMediator mediator) {
        window.getProofTreeView().getRenderer().add(new BackgroundSMTStyler(this));
        this.mediator = mediator;
        mediator.addKeYSelectionListener(this);
    }

    public boolean canApply(Node node) {
        return runner != null && runner.getCachedResult(node.sequent()).isPresent();
    }

    public void setRunner(BackgroundSolverRunner runner) {
        /*if (!runnerStatus.keySet().contains(runner)) {
            runnerStatus.put(runner, !runner.getSolvedProblems().isEmpty());
        }*/
        runners.add(runner);
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

}
