package de.uka.ilkd.key.kexext.backgroundSMT;

import de.uka.ilkd.key.control.AutoModeListener;
import de.uka.ilkd.key.core.KeYMediator;
import de.uka.ilkd.key.logic.Sequent;
import de.uka.ilkd.key.proof.*;
import de.uka.ilkd.key.settings.DefaultSMTSettings;
import de.uka.ilkd.key.settings.ProofIndependentSettings;
import de.uka.ilkd.key.smt.*;
import de.uka.ilkd.key.smt.solvertypes.SolverType;
import de.uka.ilkd.key.smt.solvertypes.SolverTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class BackgroundSolverRunner implements SolverLauncherListener, RuleAppListener, AutoModeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundSolverRunner.class);

    private final Set<SolverType> solverTypes = new HashSet<>();
    private final Proof proof;
    private final KeYMediator mediator;
    private final List<BackgroundSMTProblem> problems = new ArrayList<>();

    private final Collection<Thread> threads = new ArrayList<>();

    private final Set<SolverLauncher> launchers = new HashSet<>();

    private final SMTSettings settings;

    private final BackgroundSMTExtension extension;

    private boolean waitForAutomodeFinished = false;

    private double timeout;

    public BackgroundSolverRunner(BackgroundSMTExtension extension, Proof proof, KeYMediator mediator) {
        this.extension = extension;
        this.proof = proof;
        this.proof.addRuleAppListener(this);
        this.mediator = mediator;
        this.mediator.getUI().getProofControl().addAutoModeListener(this);
        settings = new DefaultSMTSettings(proof.getSettings().getSMTSettings(),
                ProofIndependentSettings.DEFAULT_INSTANCE.getSMTSettings(),
                proof.getSettings().getNewSMTSettings(), proof);
        applyTo(proof.getSubtreeGoals(proof.root()).toList());
    }

    /*public Set<SMTProblem> getSolvedProblems() {
        return new HashSet<>(solvedProblems);
    }*/

    public Proof getProof() {
        return proof;
    }

    public void stopLaunchedSolvers() {
        for (SolverLauncher launcher : launchers) {
            launcher.stop();
        }
        launchers.clear();
    }

    /**
     * When a rule was applied (macros counting as one rule), try to launch SMT solvers
     * on the new goals.
     *
     * @param e the proof event containing the rule application.
     */
    @Override
    public void ruleApplied(ProofEvent e) {
        // Only do sth. if the rule was applied on the proof linked with this runner.
        if (e.getSource() != proof || waitForAutomodeFinished) {
            return;
        }
        // e.getNewGoals() should not be null as we are in the ruleApplied method(?)
        if (e.getNewGoals() == null) {
            return;
        }
        applyTo(e.getNewGoals().toList());
    }

    private void applyTo(Collection<Goal> goals) {
        // Find the new goals/SMT problems resulting from the rule application.
        Collection<SMTProblem> newProblems = new ArrayList<>();
        // Get the new goals/SMT problems resulting from the rule application.
        for (Goal goal : goals) {
            SMTProblem problem = new SMTProblem(goal);
            newProblems.add(problem);
        }
        if (solverTypes.isEmpty()) {
            return;
        }
        launch(newProblems);
    }

    private void launch(Collection<SMTProblem> smtProblems) {
        // Launch the background SMT solvers on the new SMT problems.
        // TODO solver instances are never killed (should this be handled here or in the SolverLauncher?)
        // TODO handle SMTTranslator exceptions here?
        SolverLauncher launcher = new SolverLauncher(settings);
        launchers.add(launcher);
        for (SMTProblem problem : smtProblems) {
            problems.add(new BackgroundSMTProblem(problem, this, launcher));
        }
        launcher.addListener(this);
        Thread thread = new Thread(() -> {
            launcher.launch(solverTypes, smtProblems, proof.getServices());
        }, "BackgroundSMT");
        threads.add(thread);
        thread.start();
    }


    /**
     * The runner shouldn't start solvers for every intermediate state created by auto mode and proof macros.
     */

    @Override
    public void autoModeStarted(ProofEvent e) {
        waitForAutomodeFinished = true;
    }

    @Override
    public void autoModeStopped(ProofEvent e) {
        waitForAutomodeFinished = false;
        applyTo(proof.openGoals().toList());
    }


    /**
     * The runner needs to listen to the launchers it started and if one of them has results,
     * the {@link ApplyBackgroundSolverAction} linked with the solver must be activated in
     * order to allow application of the results.
     */

    @Override
    public void launcherStopped(SolverLauncher launcher, Collection<SMTSolver> finishedSolvers) {
        launchers.remove(launcher);
        extension.refresh();
    }

    @Override
    public void launcherStarted(Collection<SMTProblem> problems, Collection<SolverType> solverTypes,
                                SolverLauncher launcher) {
        launchers.add(launcher);
    }

    public Optional<SMTSolverResult> getCachedResult(Sequent sequent) {
        for (BackgroundSMTProblem problem : problems) {
            if (problem.getProblem().getSequent().equals(sequent)) {
                SMTSolverResult result = problem.getProblem().getFinalResult();
                if (result.isValid() != SMTSolverResult.ThreeValuedTruth.UNKNOWN) {
                    return Optional.of(problem.getProblem().getFinalResult());
                }
            }
        }
        return Optional.empty();
    }

    public void setTimeout(double timeout) {
        this.timeout = timeout;
    }

    public void setSolvers(Collection<SolverType> checkedTypes) {
        this.solverTypes.clear();
        this.solverTypes.addAll(checkedTypes);
        tryForCurrentlyNotRunningGoals();
    }

    private void tryForCurrentlyNotRunningGoals() {
        List<SMTProblem> inactiveProblems = new ArrayList<>();
        for (Goal goal : proof.openGoals()) {
            if (getCachedResult(goal.sequent()).isEmpty()) {
                inactiveProblems.add(new SMTProblem(goal));
            }
        }
        launch(inactiveProblems);
    }
}
