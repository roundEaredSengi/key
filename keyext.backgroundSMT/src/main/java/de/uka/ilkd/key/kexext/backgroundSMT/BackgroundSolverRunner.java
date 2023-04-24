package de.uka.ilkd.key.kexext.backgroundSMT;

import de.uka.ilkd.key.control.AutoModeListener;
import de.uka.ilkd.key.proof.*;
import de.uka.ilkd.key.smt.*;
import de.uka.ilkd.key.smt.solvertypes.SolverType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class BackgroundSolverRunner implements SolverLauncherListener, RuleAppListener,
    AutoModeListener, ProofTreeListener{

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundSolverRunner.class);
    /**
     * The proof this runner belongs to.
     */
    private final Proof proof;
    /**
     * The SMT problems launched by this runner.
     */
    private final List<BackgroundSMTProblem> problems = new ArrayList<>();
    /**
     * All currently running threads started by this runner.
     */
    private final Collection<Thread> threads = new ArrayList<>();
    private final Set<SolverLauncher> launchers = Collections.synchronizedSet(new HashSet<>());
    private final Collection<RunnerListener> listeners =
        Collections.synchronizedCollection(new ArrayList<>(2));

    private final Set<Node> alreadyRan = Collections.synchronizedSet(new HashSet<>());
    private final Set<Node> solvedNodes = Collections.synchronizedSet(new HashSet<>());

    private volatile boolean waitForAutomodeFinished = false;
    private BackgroundSMTSettings settings;



    /**
     * Create a new launcher that starts solvers of the types according to {@link #settings}
     * for the given SMT problems.
     * @param smtProblems the SMT problems for which to start SMT solvers
     */
    private void launch(Collection<SMTProblem> smtProblems) {
        // Launch the background SMT solvers on the new SMT problems.
        // TODO solver instances are never killed (should this be handled here or in the SolverLauncher?)
        // TODO handle SMTTranslator exceptions here?
        synchronized (settings) {
            SolverLauncher launcher = new SolverLauncher(settings);
            synchronized (launchers) {
                launchers.add(launcher);
            }
            smtProblems.removeAll(smtProblems.stream().filter(
                p -> alreadyRan.contains(p.getGoal().node())).collect(Collectors.toList()));
            for (SMTProblem problem : smtProblems) {
                problems.add(new BackgroundSMTProblem(problem, this, launcher));
                if (problem.getGoal() != null) {
                    alreadyRan.add(problem.getGoal().node());
                }
            }
            launcher.addListener(this);
            Thread thread = new Thread(() -> {
                launcher.launch(settings.getTypes(), smtProblems, proof.getServices());
            }, "BackgroundSMT");
            LOGGER.info("Background SMT launched for " + smtProblems.size() + " SMT problems on "
                + proof.name());
            threads.add(thread);
            thread.start();
        }
    }

    public void tryForOpenGoals() {
        List<SMTProblem> openGoals = new ArrayList<>();
        for (Goal goal : proof.openGoals()) {
            if (!getSolvedNodes().contains(goal.node())) {
                openGoals.add(new SMTProblem(goal));
            }
        }
        launch(openGoals);
    }

    /**
     * Creates a new background SMT solver runner for the given proof.
     * Launches background SMT solvers for all current goals of the given proof.
     * @param proof the proof which this runner listens to
     */
    public BackgroundSolverRunner(Proof proof, BackgroundSMTSettings settings) {
        this.proof = proof;
        proof.addProofTreeListener(this);
        this.proof.addRuleAppListener(this);
        this.settings = settings;
        // Launch SMT solvers for all current goals of the proof.
        /*launch(proof.getSubtreeGoals(proof.root()).stream()
            .map(g -> new SMTProblem(g)).collect(Collectors.toList()));
        LOGGER.info("Initial background SMT launch for open goals of " + proof.name());*/
    }

    /**
     *
     * @return the proof this runner belongs to
     */
    public Proof getProof() {
        return proof;
    }

    /**
     * Stop all SMT solver instances launched by this runner.
     */
    public void stopLaunchedSolvers() {
        for (SolverLauncher launcher : launchers) {
            launcher.stop();
        }
        launchers.clear();
        LOGGER.info("Stopped all running background SMT solvers on " + proof.name());
    }

    /**
     * When a rule was applied (macros counting as one rule), try to launch SMT solvers
     * on the new goals.
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
        launch(e.getNewGoals().stream().map(g -> new SMTProblem(g)).collect(Collectors.toList()));
        LOGGER.info("Background SMT launched for " + e.getNewGoals().size()
            + " new goal(s) created by interactive rule app on " + proof.name());
    }

    /**
     * The runner shouldn't start solvers for every intermediate state created by auto mode
     * and proof macros.
     */
    @Override
    public void autoModeStarted(ProofEvent e) {
        if (e.getSource() != proof) {
            return;
        }
        waitForAutomodeFinished = true;
        LOGGER.info("Background SMT runner is waiting for automode on " + proof.name()
            + " to finish.");
    }

    /**
     * Once the auto mode stopped, launch background SMT solvers on all the goals that are not
     * currently running.
     */
    @Override
    public void autoModeStopped(ProofEvent e) {
        if (e.getSource() != proof) {
            return;
        }
        waitForAutomodeFinished = false;
        // TODO
        tryForOpenGoals();
    }


    /**
     * The runner needs to listen to the launchers it started and if one of them has results,
     * the {@link ApplyBackgroundSolverAction} linked with the solver must be activated in
     * order to allow application of the results.
     */

    @Override
    public void launcherStopped(SolverLauncher launcher, Collection<SMTSolver> finishedSolvers) {
        solvedNodes.addAll(problems.stream()
            .filter(p -> p.getProblem().getFinalResult().isValid()
                == SMTSolverResult.ThreeValuedTruth.VALID)
            .map(p -> p.getProblem().getGoal().node())
            .collect(Collectors.toList()));
        for (RunnerListener listener : listeners) {
            listener.runnerRefresh();
        }
    }

    @Override
    public void launcherStarted(Collection<SMTProblem> problems, Collection<SolverType> solverTypes,
                                SolverLauncher launcher) {
        launchers.add(launcher);
        for (RunnerListener listener : listeners) {
            listener.runnerRefresh();
        }
    }

    @Override
    public void proofClosed(ProofTreeEvent e) {
        if (e.getSource() == proof) {
            stopLaunchedSolvers();
            for (RunnerListener listener : listeners) {
                listener.runnerRefresh();
            }
        }
    }

    public void refreshSettings(BackgroundSMTSettings newSettings) {
        synchronized (settings) {
            this.settings = newSettings;
        }
        stopLaunchedSolvers();
        alreadyRan.clear();
        LOGGER.info("Resetting background SMT processes for " + proof.name()
            + " after refreshing the background SMT settings.");
    }

    public void addListener(RunnerListener listener) {
        this.listeners.add(listener);
    }

    /**
     *
     * @return the nodes solved (unsat) by SMT solver instances started by this runner
     */
    public List<Node> getSolvedNodes() {
        return new ArrayList<>(solvedNodes);
    }

}
