// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.EventListenerList;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.gui.configuration.ProofSettings;
import de.uka.ilkd.key.gui.notification.events.NotificationEvent;
import de.uka.ilkd.key.gui.notification.events.ProofClosedNotificationEvent;
import de.uka.ilkd.key.java.JavaInfo;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.*;
import de.uka.ilkd.key.pp.NotationInfo;
import de.uka.ilkd.key.pp.PosInSequent;
import de.uka.ilkd.key.proof.*;
import de.uka.ilkd.key.proof.init.JavaProfile;
import de.uka.ilkd.key.proof.init.Profile;
import de.uka.ilkd.key.rule.*;
import de.uka.ilkd.key.strategy.feature.AbstractBetaFeature;
import de.uka.ilkd.key.strategy.feature.IfThenElseMalusFeature;
import de.uka.ilkd.key.util.Debug;
import de.uka.ilkd.key.util.KeYExceptionHandler;
import de.uka.ilkd.key.util.KeYRecoderExcHandler;


public class KeYMediator {

    private IMain mainFrame;


    private InteractiveProver interactiveProver;

    /** the notation info used to print sequents */
    private final NotationInfo notationInfo;

    /** listenerList with to gui listeners */
    private EventListenerList listenerList = new EventListenerList();

    /** listens to the proof */
    private KeYMediatorProofListener proofListener;

    /** listens to the ProofTree */
    private KeYMediatorProofTreeListener proofTreeListener;

    /** current proof and node the user works with. All user
     * interaction is relative to this model
     */
    private KeYSelectionModel keySelectionModel;

    private KeYExceptionHandler defaultExceptionHandler;

    private boolean stupidMode; // minimize user interaction

    private boolean autoMode; // autoModeStarted has been fired
    
    private ArrayList<InterruptListener> interruptListener = new ArrayList<InterruptListener>();
    
    /** creates the KeYMediator with a reference to the application's
     * main frame and the current proof settings
    */
    public KeYMediator(IMain mainFrame) {
	this.mainFrame = mainFrame;
	notationInfo        = new NotationInfo();
	proofListener       = new KeYMediatorProofListener();
	proofTreeListener   = new KeYMediatorProofTreeListener();
	keySelectionModel   = new KeYSelectionModel();
	interactiveProver   = new InteractiveProver(this);
	
	addRuleAppListener(proofListener);
	addAutoModeListener(proofListener);
	defaultExceptionHandler = new KeYRecoderExcHandler();
    }

    public void addinterruptListener(InterruptListener il) {
	this.interruptListener.add(il);
    }
    
    public void removeInterruptListener(InterruptListener il) {
	this.interruptListener.remove(il);
    }
    
    public void interrupted(ActionEvent e) {
	for (InterruptListener il : interruptListener) {
	    il.interruptionPerformed(e);
	}
    }
    
    /** returns the used NotationInfo
     * @return the used NotationInfo
     */
    public NotationInfo getNotationInfo() {
	return notationInfo;
    }

    public KeYExceptionHandler getExceptionHandler(){
	if(getProof() != null){
	    return getServices().getExceptionHandler();
	}else{
	    return defaultExceptionHandler;
	}
    }

    /** returns the variable namespace 
     * @return the variable namespace 
     */
    public Namespace var_ns() {
	return getProof().getNamespaces().variables();
    }
    
    /** returns the program variable namespace 
     * @return the program variable namespace 
     */
    public Namespace progVar_ns() {
	return getProof().getNamespaces().programVariables();
    }

    /** returns the function namespace 
     * @return the function namespace 
     */
    public Namespace func_ns() {
	return getProof().getNamespaces().functions();
    }

    /** returns the sort namespace 
     * @return the sort namespace 
     */
    public Namespace sort_ns() {
	return getProof().getNamespaces().sorts();
    }

    /** returns the heuristics namespace 
     * @return the heuristics namespace 
     */
    public Namespace heur_ns() {
	return getProof().getNamespaces().ruleSets();
    }

    /** returns the choice namespace
     * @return the choice namespace 
     */
    public Namespace choice_ns() {
	return getProof().getNamespaces().choices();
    }

    /** returns the prog var namespace 
     * @return the prog var namespace 
     */
    public Namespace pv_ns() {
	return getProof().getNamespaces().programVariables();
    }

    /** returns the namespace set
     * @return the  namespace set
     */
    public NamespaceSet namespaces() {
	return getProof().getNamespaces();
    }

    /** returns the JavaInfo with the java type information */
    public JavaInfo getJavaInfo() {
       return getProof().getJavaInfo();
    }

    /** returns the Services with the java service classes */
    public Services getServices() {
       return getProof().getServices();
    }

    /** simplified user interface? */
    public boolean stupidMode() {
       return stupidMode;
    }

    public void setStupidMode(boolean b) {
       stupidMode = b;
    }

    public boolean ensureProofLoadedSilent() {
	return getProof() != null;
    }

    public boolean ensureProofLoaded() {
	final boolean loaded = ensureProofLoadedSilent();
	if (!loaded) {
	    popupWarning("No proof.", "Oops...");
	}
	return loaded;
    }
    
    /** Undo.
     * @author VK
     */
    public void setBack() {
	if (ensureProofLoaded()) {
	    setBack(getSelectedGoal());
	}
    }

    public void setBack(Node node) {
	if (ensureProofLoaded()) {
	    if (getProof().setBack(node)) {
                finishSetBack();
	    }else{
                popupWarning("Setting back at the chosen node is not possible.",
                "Oops...");            
	    }
	}
    }    
    
    public void setBack(Goal goal) {
	if (ensureProofLoaded()) {
	    if (getProof() != null && getProof().setBack(goal)){
                finishSetBack();
	    }else{
                popupWarning("Setting back the current goal is not possible.", 
                "Oops...");
	    }
	}
    }
    
    
    private void finishSetBack(){
        TermTacletAppIndexCacheSet.clearCache();
        AbstractBetaFeature.clearCache();
        IfThenElseMalusFeature.clearCache();
    }

    
    /** 
     * initializes proof (this is Swing thread-safe) 
     */
    public void setProof(Proof p) {
        final Proof pp = p;    
        if (SwingUtilities.isEventDispatchThread()) {
	    setProofHelper(pp);
        } else {
            Runnable swingProzac = new Runnable() {
               public void run() { setProofHelper(pp); }
            };
	    invokeAndWait(swingProzac);
        }        
    }


    private void setProofHelper(Proof p) {
	if (getProof() != null) {
	    getProof().removeProofTreeListener(proofTreeListener);
	}
	if (p!=null) notationInfo.setAbbrevMap(p.abbreviations());
	Proof proof = p;
	if (proof != null) {
	    proof.addProofTreeListener(proofTreeListener);
	    proof.mgt().setMediator(this);
	}
        keySelectionModel.setSelectedProof(proof);
    }
    

    /**
     * Get the interactive prover.
     */
    public InteractiveProver getInteractiveProver() {
	return interactiveProver;
    }


    /** the proof the mediator handles with */
    public Proof getProof() {
	return keySelectionModel.getSelectedProof();
    }
    

    /** sets the maximum number of rule applications allowed in
     * automatic mode
     * @param steps an int setting the limit
     */
    public void setMaxAutomaticSteps(int steps) {
       if (getProof() != null) {
           getProof().getSettings().getStrategySettings().setMaxSteps(steps);
       }
       ProofSettings.DEFAULT_SETTINGS.getStrategySettings().setMaxSteps(steps);
    }

    /** returns the maximum number of rule applications allowed in
     * automatic mode
     * @return the maximum number of rule applications allowed in
     * automatic mode
     */
    public int getMaxAutomaticSteps() {
        if (getProof() != null) {
            return getProof().getSettings().getStrategySettings().getMaxSteps();
        } else {
            return ProofSettings.DEFAULT_SETTINGS.getStrategySettings().getMaxSteps();
        }
    }

    public ImmutableSet<TacletApp> getTacletApplications(Goal g, String name,
                                                PosInOccurrence p) {
       return interactiveProver.getAppsForName(g, name, p);
    }


    public ImmutableSet<TacletApp> getTacletApplications(Goal            goal, 
						String          name,
                                                PosInOccurrence pos,
                                                TacletFilter    filter) {
       return interactiveProver.getAppsForName(goal, name, pos, 
					       filter);
    }
    
    /**
     * collects all applications of a rule given by its name at a give position in the sequent
     * @param name
     * 				the name of the BuiltInRule for which applications are collected.
     * @param pos
     * 				the position in the sequent where the BuiltInRule should be applied
     * @return
     * 				a SetOf<RuleApp> with all possible applications of the rule
     */
    public ImmutableSet<RuleApp> getBuiltInRuleApplications(String name, PosInOccurrence pos)
    {
    	return interactiveProver.getBuiltInRuleAppsForName(name, pos);
    }

    /**
     * selected rule to apply; opens a dialog 
     * @param tacletApp the TacletApp which has been selected  
     * @param pos the PosInSequent describes the position where to apply the 
     * rule 
     */
    public void selectedTaclet(TacletApp tacletApp, PosInSequent pos) {
	Goal goal = keySelectionModel.getSelectedGoal();
        Debug.assertTrue(goal != null);
        selectedTaclet(tacletApp.taclet(), goal, pos.getPosInOccurrence());
    }


    public boolean selectedTaclet(Taclet taclet, Goal goal, 
				  PosInOccurrence pos) {
	ImmutableSet<TacletApp> applics = 
           getTacletApplications(goal, taclet.name().toString(), pos);
        if (applics.size() == 0) {
	   JOptionPane.showMessageDialog(mainFrame(), "Taclet application failed." 
					 + "\n" + taclet.name(), "Bummer!",
					 JOptionPane.ERROR_MESSAGE);
           return false;
        }
	Iterator<TacletApp> it = applics.iterator();	
	if (applics.size() == 1) {
	    TacletApp firstApp = it.next();
            boolean ifSeqInteraction = 
               !firstApp.taclet().ifSequent().isEmpty() ;
            if (stupidMode && !firstApp.complete()) {                
                ImmutableList<TacletApp> ifSeqCandidates =
                    firstApp.findIfFormulaInstantiations(goal.sequent(),
		        getServices());
                
                if (ifSeqCandidates.size() == 1) {
                    ifSeqInteraction = false;
                    firstApp = ifSeqCandidates.head();
                }               
                TacletApp tmpApp = 
                    firstApp.tryToInstantiate(getServices());                
                if (tmpApp != null) firstApp = tmpApp;
                
               
            }            
	    if (ifSeqInteraction || !firstApp.complete()) {             
                TacletMatchCompletionDialog.completeAndApplyApp(
                    firstApp, goal, this);
	    } else {
		applyInteractive(firstApp, goal);
	    }
	} else if (applics.size() > 1) {
            java.util.List<TacletApp> appList = new java.util.LinkedList<TacletApp>();
            
	    for (int i = 0; i < applics.size(); i++) {
	        TacletApp rapp = it.next();
                appList.add(rapp);
            }
            
            if (appList.size()==0) {
                 assert false;
                 return false;
            }

            TacletMatchCompletionDialog.completeAndApplyApp(
                appList, goal, this);
            
        }
        return true;
    }
    

    /** selected rule to apply
     * @param rule the selected built-in rule
     * @param pos the PosInSequent describes the position where to apply the
     * rule 
     */
    public void selectedBuiltInRule(BuiltInRule rule, PosInOccurrence pos) {
	Goal goal = keySelectionModel.getSelectedGoal();
	assert goal != null;

	ImmutableSet<RuleApp> set = interactiveProver.
	    getBuiltInRuleApp(rule, pos);
	if (set.size() > 1) {
	    System.err.println("keymediator:: Expected a single app. If " +
		      "it is OK that there are more than one " +
		      "built-in rule apps. You have to add a " +
		      "selection dialog here");
	    System.err.println("keymediator:: Ambigous applications, " +
		      "taking the first in list.");
	}

	RuleApp app = set.iterator().next();
	if (app != null && app.rule() == rule) {
	    goal.apply(app);
	    return;
	}
    }
     
      
    /**
     * Apply a RuleApp and continue with update simplification or strategy
     * application according to current settings.
     * @param app
     * @param goal
     */
    public void applyInteractive(RuleApp app, Goal goal) {
        interactiveProver.applyInteractive(app, goal);
    }

    

    /** collects all applicable FindTaclets of the current goal
     * (called by the SequentViewer)
     * @return a list of Taclets with all applicable FindTaclets
     */

    public ImmutableList<TacletApp> getFindTaclet(PosInSequent pos) {
    	return interactiveProver.getFindTaclet(pos);
    }

    /** collects all applicable RewriteTaclets of the current goal
     * (called by the SequentViewer)
     * @return a list of Taclets with all applicable RewriteTaclets
     */
    public ImmutableList<TacletApp> getRewriteTaclet(PosInSequent pos) {
    	return interactiveProver.getRewriteTaclet(pos);    
    }

    /** collects all applicable NoFindTaclets of the current goal
     * (called by the SequentViewer)
     * @return a list of Taclets with all applicable NoFindTaclets
     */
    public ImmutableList<TacletApp> getNoFindTaclet() {	
    	return interactiveProver.getNoFindTaclet();
    }

    /** collects all built-in rules 
     * @return a list of all applicable built-in rules 
     */
    public ImmutableList<BuiltInRule> getBuiltInRule(PosInOccurrence pos) {
	return interactiveProver.getBuiltInRule
	    (pos);
    }

    /** adds a listener to the KeYSelectionModel, so that the listener
     * will be informed if the proof or node the user has selected
     * changed 
     * @param listener the KeYSelectionListener to add
     */
    public synchronized void addKeYSelectionListener(KeYSelectionListener listener) {
	keySelectionModel.addKeYSelectionListener(listener);
    }

    /** removes a listener from the KeYSelectionModel
     * @param listener the KeYSelectionListener to be removed
     */
    public synchronized void removeKeYSelectionListener(KeYSelectionListener listener) {
	keySelectionModel.removeKeYSelectionListener(listener);
    }

    /** adds a listener to GUI events 
     * @param listener the GUIListener to be added
     */
    public void addGUIListener(GUIListener listener) {
	listenerList.add(GUIListener.class, listener);
    }

    /** adds a listener to GUI events 
     * @param listener the GUIListener to be added
     */
    public void removeGUIListener(GUIListener listener) {
	listenerList.remove(GUIListener.class, listener);
    }

    public void addRuleAppListener(RuleAppListener listener) {
	Goal.addRuleAppListener(listener);
    }

    public void removeRuleAppListener(RuleAppListener listener) {  
	Goal.removeRuleAppListener(listener);
    }

    public void addAutoModeListener(AutoModeListener listener) {
	interactiveProver.addAutoModeListener(listener);
    }

    public void removeAutoModeListener(AutoModeListener listener) {  
	interactiveProver.addAutoModeListener(listener);
    }

    /** sets the current goal 
     * @param goal the Goal being displayed in the view of the sequent
     */
    public void goalChosen(Goal goal) {
	keySelectionModel.setSelectedGoal(goal);
    }

    /** returns the main frame
     * @return the main frame 
     */
    public JFrame mainFrame() {
	return mainFrame instanceof JFrame ? (JFrame) mainFrame : null;
    }

    /** notifies that a node that is not a goal has been chosen
     * @param node the node being displayed in the view of the sequent
     */
    public void nonGoalNodeChosen(Node node) {
	keySelectionModel.setSelectedNode(node);
    }

    /** called to ask for modal access 
     * @param src Object that is the asking component 
     */
    public synchronized void requestModalAccess(Object src) {
	fireModalDialogOpened(new GUIEvent(src));	
    }

    /** called if no more modal access is needed
    * @param src Object that is the asking component 
     */
    public synchronized void freeModalAccess(Object src) {
	fireModalDialogClosed(new GUIEvent(src));	
    }
    
    /** fires the request of a GUI component for modal access
     * this can be used to disable all views even if the GUI component
     * has no built in modal support 
     */
    public synchronized void fireModalDialogOpened(GUIEvent e) {
	Object[] listeners = listenerList.getListenerList();
	for (int i = listeners.length-2; i>=0; i-=2) {
	    if (listeners[i] == GUIListener.class) {
		((GUIListener)listeners[i+1]).modalDialogOpened(e);
	    }
	}
    }

    /** fires that a GUI component that has asked for modal access
     * has been closed, so views can be enabled again
     */
    public synchronized void fireModalDialogClosed(GUIEvent e) {
	Object[] listeners = listenerList.getListenerList();
	for (int i = listeners.length-2; i>=0; i-=2) {
	    if (listeners[i] == GUIListener.class) {
		((GUIListener)listeners[i+1]).modalDialogClosed(e);
	    }
	}
    }

    /** Fires the shut down event.
     */
    public synchronized void fireShutDown(GUIEvent e) {
	Object[] listeners = listenerList.getListenerList();
	for (int i = listeners.length-2; i>=0; i-=2) {
	    if (listeners[i] == GUIListener.class) {
		((GUIListener)listeners[i+1]).shutDown(e);
	    }
	}
    }
   
  
    /** returns the current selected proof 
     * @return the current selected proof 
     */
    public Proof getSelectedProof() {
 	return keySelectionModel.getSelectedProof();
    }

    /** returns the current selected goal 
     * @return the current selected goal 
     */
    public Goal getSelectedGoal() {
 	return keySelectionModel.getSelectedGoal();
    }

   /** returns the current selected goal 
     * @return the current selected goal 
     */
    public KeYSelectionModel  getSelectionModel() {
 	return keySelectionModel;
    }

    /** returns the current selected node
     * @return the current selected node 
     */
    public Node getSelectedNode() {
 	return keySelectionModel.getSelectedNode();
    }
        
    /**
     * Start automatic application of rules on open goals.
     */
    public void startAutoMode() {
	if (ensureProofLoaded()) {
	    startAutoMode(getProof().openEnabledGoals());
	}
    }

    /**
     * Start automatic application of rules on specified goals.
     * @param goals
     */
    public void startAutoMode(ImmutableList<Goal> goals) {
       interactiveProver.startAutoMode(goals);
    }

    /**
     * Stop automatic application of rules.
     */
    public void stopAutoMode() {
        interactiveProver.stopAutoMode();
    }
    
    public void setResumeAutoMode(boolean b) {
       interactiveProver.setResumeAutoMode(b);
    }
    
    /**
     * Switches interactive mode on or off.
     * @param b true iff interactive mode is to be turned on
     */
    public void setInteractive ( boolean b ) {
        interactiveProver.setInteractive ( b );
        if (getProof() != null) {
            if ( b  ) {
                getProof().setRuleAppIndexToInteractiveMode ();
            } else {
                getProof().setRuleAppIndexToAutoMode ();
            }
        }
    }

    public void popupInformationMessage(Object message, String title) {
        JOptionPane.showMessageDialog
	    (mainFrame(), message,
	     title, JOptionPane.INFORMATION_MESSAGE);
    }

    public void popupWarning(Object message, String title) {
        JOptionPane.showMessageDialog(mainFrame(), message, title, 
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Brings up a dialog displaying a message.
     * @param modal whether or not the message should be displayed in a modal dialog.
     */
    public void popupInformationMessage(Object message, String title, boolean modal) {
        if (modal) {
	    popupInformationMessage(message, title);
	} else {
	    if (!(message instanceof Component))
		throw new InternalError("only messages of type " + Component.class + " supported, yet");
	    // JFrame dlg = new JDialog(mainFrame(),title, modal);
	    JFrame dlg = new JFrame(title);
	    dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	    dlg.getContentPane().add((Component)message);
	    dlg.pack();
	    setCenter(dlg, mainFrame());
	    dlg.setVisible(true);
	}
    }

    /**
     * Center a component within a parental component.
     * @param comp the component to be centered.
     * @param parent center relative to what. <code>null</code> to center relative to screen.
     * @see #setCenter(Component)
     */
    public static void setCenter(Component comp, Component parent) {
	if (parent == null) {
	    setCenter(comp);
	    return;
	} 
	Dimension dlgSize = comp.getPreferredSize();
	Dimension frmSize = parent.getSize();
	Point	  loc = parent.getLocation();
	if (dlgSize.width < frmSize.width && dlgSize.height < frmSize.height)
	    comp.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
	else
	    setCenter(comp);
    } 

    /**
     * Center a component on the screen.
     * @param comp the component to be centered relative to the screen.
     *  It must already have its final size set.
     * @preconditions comp.getSize() as on screen.
     */
    public static void setCenter(Component comp) {
	Dimension screenSize = comp.getToolkit().getScreenSize();
	Dimension frameSize = comp.getSize();
	if (frameSize.height > screenSize.height)
	    frameSize.height = screenSize.height;
	if (frameSize.width > screenSize.width)
	    frameSize.width = screenSize.width;
	comp.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    } 

    private int goalsClosedByAutoMode=0;


    private Profile profile;

    public void closedAGoal() { 
	    goalsClosedByAutoMode++;
    }

    public int getNrGoalsClosedByAutoMode() {
	return goalsClosedByAutoMode;
    }

    public void resetNrGoalsClosedByHeuristics() {
	goalsClosedByAutoMode=0;
    }


   public void stopInterface(boolean fullStop) {
      final boolean b = fullStop;
      Runnable interfaceSignaller = new Runnable() {
         public void run() {
	     mainFrame().setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            if (b) {
               interactiveProver.fireAutoModeStarted(
                  new ProofEvent(getProof()));
            }
         }
      };
      invokeAndWait(interfaceSignaller);
   }

   public void startInterface(boolean fullStop) {
      final boolean b = fullStop;
      Runnable interfaceSignaller = new Runnable() {
         public void run() {
            if ( b )
               interactiveProver.fireAutoModeStopped (new ProofEvent(getProof()));
            mainFrame().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            if (getProof() != null)
                keySelectionModel.fireSelectedProofChanged();
         }
      };
      invokeAndWait(interfaceSignaller);
   }
   

   public static void invokeAndWait(Runnable runner) {
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else {
            try{
               SwingUtilities.invokeAndWait(runner);
            } catch(InterruptedException e) {
	        System.err.println(e);
	        e.printStackTrace();
            } catch(java.lang.reflect.InvocationTargetException ite) {
	       Throwable targetExc = ite.getTargetException();
               System.err.println(targetExc);
	       targetExc.printStackTrace();
               ite.printStackTrace();
            }
        }
    }
    
    
    public boolean autoMode() {
        return autoMode;
    }


    class KeYMediatorProofTreeListener extends ProofTreeAdapter {
	public void proofClosed(ProofTreeEvent e) {
	    KeYMediator.this.notify
	        (new ProofClosedNotificationEvent(e.getSource()));
	}

	public void proofPruned(ProofTreeEvent e) {
	    final ProofTreeRemovedNodeEvent ev = (ProofTreeRemovedNodeEvent) e;
	    if (ev.getRemovedNode() == getSelectedNode()) {
		SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
			keySelectionModel.setSelectedNode(ev.getNode());
		    }
		});
	    }
	}
    
	public void proofGoalsAdded(ProofTreeEvent e) {
	    ImmutableList<Goal> newGoals = e.getGoals();
	    // Check for a closed goal ...
	    if (newGoals.size() == 0){
		// No new goals have been generated ...
		closedAGoal();
	    }
	}

	public void proofStructureChanged(ProofTreeEvent e) {
	    if (autoMode()) return;
	    Proof p = e.getSource();
	    if (p == getSelectedProof()) {
		Node sel_node = getSelectedNode();
		if (!p.find(sel_node)) {
		    keySelectionModel.defaultSelection();
		} else {
		    // %%% hack does need to be done proper
		    // needed top update that the selected node nay have
		    // changed its status 
		    keySelectionModel.setSelectedNode(sel_node);
		}
	    }
	}
    }

    private final class KeYMediatorProofListener implements RuleAppListener, 
                                                            AutoModeListener {

	/** invoked when a rule has been applied */
	public void ruleApplied(ProofEvent e) {
	    if (autoMode()) return;
	    if (e.getSource() == getProof()) {
	        keySelectionModel.defaultSelection();
	    }
	}


	/** invoked if automatic execution has started
	 */
	public void autoModeStarted(ProofEvent e) {
	    autoMode = true;
	}
	
	/** invoked if automatic execution has stopped
	 */
	public void autoModeStopped(ProofEvent e) {
            autoMode = false;
	}
    }

    class KeYMediatorSelectionListener implements KeYSelectionListener {
	/** focused node has changed */
	public void selectedNodeChanged(KeYSelectionEvent e) {
	    // empty
	}

	/** the selected proof has changed (e.g. a new proof has been
	 * loaded) 
	 */ 
	public void selectedProofChanged(KeYSelectionEvent e) {
	    setProof(e.getSource().getSelectedProof());
	}	
    }
    
    public void enableWhenProof(final Action a) {
        a.setEnabled(getProof() != null);
        addKeYSelectionListener(new KeYSelectionListener() {
            public void selectedNodeChanged(KeYSelectionEvent e) {}
            public void selectedProofChanged(KeYSelectionEvent e) {
                a.setEnabled(
                    e.getSource().getSelectedProof() != null);
            }
        });
    }
    

    /**
     * takes a notification event and informs the notification
     * manager 
     * @param event the NotificationEvent
     */
    public void notify(NotificationEvent event) {        
        if (mainFrame != null) {
            mainFrame.notify(event);
        }
    }

    /** return the chosen profile */
    public Profile getProfile() {  
        if (profile == null) {               
            profile = ProofSettings.DEFAULT_SETTINGS.getProfile();   
            if (profile == null) {
                profile = new JavaProfile((IMain) this.mainFrame());
            }
        }
        return profile;
    }

    /** 
     * besides the number of rule applications it is possible to define a timeout after which rule application
     * shall be terminated
     * @return the time in ms after which automatic rule application stops
     */
    public long getAutomaticApplicationTimeout() {      
        if (getProof() != null) {
            return getProof().getSettings().getStrategySettings().getTimeout();
        } else {
            return ProofSettings.DEFAULT_SETTINGS.getStrategySettings().getTimeout();
        }
    }        
    
    /** 
     * sets the time out after which automatic rule application stops
     * @param timeout a long specifying the timeout time in ms
     */
    public void setAutomaticApplicationTimeout(long timeout) {
       if (getProof() != null) {
           getProof().getSettings().getStrategySettings().setTimeout(timeout);
       }
       ProofSettings.DEFAULT_SETTINGS.getStrategySettings().setTimeout(timeout);
    }

    
    
    /** 
     * returns the prover task listener of the main frame
     */
    public ProverTaskListener getProverTaskListener() {
        return mainFrame.getProverTaskListener();
    }
}