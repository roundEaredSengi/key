package de.uka.ilkd.key.kexext.backgroundSMT;

import de.uka.ilkd.key.gui.actions.KeyAction;
import de.uka.ilkd.key.proof.Node;

import java.awt.event.ActionEvent;
import java.util.*;

public class ApplyBackgroundSolverAction extends KeyAction {

    private BackgroundSolverRunner runner;

    private Set<BackgroundSolverRunner> runners = new HashSet<>();
    private Map<BackgroundSolverRunner, Boolean> runnerStatus = new HashMap<>();

    private final Node treeNode;

    private final BackgroundSMTExtension extension;

    public ApplyBackgroundSolverAction(Node obj, BackgroundSMTExtension extension) {
        this.extension = extension;
        this.treeNode = obj;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        extension.applyRunner(treeNode);
    }

}
