package de.uka.ilkd.key.util;

import de.uka.ilkd.key.gui.ApplyStrategy;
import de.uka.ilkd.key.gui.ApplyStrategy.ApplyStrategyInfo;
import de.uka.ilkd.key.gui.configuration.ProofSettings;
import de.uka.ilkd.key.logic.Semisequent;
import de.uka.ilkd.key.logic.Sequent;
import de.uka.ilkd.key.logic.SequentFormula;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.proof.DepthFirstGoalChooserBuilder;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.ProofAggregate;
import de.uka.ilkd.key.proof.init.InitConfig;
import de.uka.ilkd.key.proof.init.Profile;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.proof.init.ProofOblInput;
import de.uka.ilkd.key.proof.io.ProofSaver;
import de.uka.ilkd.key.proof.mgt.ProofEnvironment;
import de.uka.ilkd.key.strategy.StrategyProperties;

/**
 * This class encapsulates the registration of a proof for a given problem.
 * It then starts a proof attempt.
 * 
 * After the proof attempt stops (successfully or not) the side proof is by default 
 * unregistered, but can be accessed via this class.
 * 
 * @author Richard Bubel
 */
public class ProofStarter {
    
    /** 
     * Proof obligation for a given formula or sequent 
     */
    public static class UserProvidedInput implements ProofOblInput {

        private static final String EMPTY_PROOF_HEADER = "";        
        private final ProofEnvironment env;
        private final Sequent seq;
        
        public UserProvidedInput(Sequent seq, ProofEnvironment env) {
            this.seq     = seq;
            this.env     = env;
        }

        public UserProvidedInput(Term formula, ProofEnvironment env) {
            this(Sequent.createSuccSequent(Semisequent.EMPTY_SEMISEQUENT.insertFirst(            
                    new SequentFormula(formula)).semisequent()), env);
        }
        
        @Override
        public String name() {
            return "ProofObligation for " + ProofSaver.printAnything(seq, null);
        }        
        
        @Override
        public void readProblem() throws ProofInputException {
        }

        
        private Proof createProof(String proofName) {
            
            final InitConfig initConfig = env.getInitConfig();
            
            return new Proof(proofName,
                    seq,
                    EMPTY_PROOF_HEADER,
                    initConfig.createTacletIndex(),
                    initConfig.createBuiltInRuleIndex(),
                    initConfig.getServices(),
                    initConfig.getSettings() != null
                    ? initConfig.getSettings()
                            : new ProofSettings(ProofSettings.DEFAULT_SETTINGS));
        }

        
        @Override
        public ProofAggregate getPO() throws ProofInputException {
            final Proof proof = createProof("Proof object for "+
                    ProofSaver.printAnything(seq, null));             
            
            return ProofAggregate.createProofAggregate(proof, "ProofAggregate for claim: "+proof.name());
        }

        @Override
        public boolean implies(ProofOblInput po) {
            return this == po;
        }

    }


    private Proof proof;
    
    private int maxSteps = 2000;
    
    private long timeout = -1L;

    private StrategyProperties strategyProperties = new StrategyProperties();

    /**
     * creates an instance of the ProofStarter
     * @param the ProofEnvironment in which the proof shall be performed
     */
    public ProofStarter() {}
    
    
    /**
     * creates a new proof object for formulaToProve and registers it in the given environment
     * 
     * @throws ProofInputException 
     */
    public void init(Term formulaToProve, ProofEnvironment env) throws ProofInputException {
        final ProofOblInput input = new UserProvidedInput(formulaToProve, env);
        proof = input.getPO().getFirstProof();
        proof.setProofEnv(env);
    }

    /**
     * creates a new proof object for sequentToProve and registers it in the given environment
     * 
     * @throws ProofInputException 
     */
    public void init(Sequent sequentToProve, ProofEnvironment env) throws ProofInputException {
       final ProofOblInput input = new UserProvidedInput(sequentToProve, env);
       proof = input.getPO().getFirstProof();
       proof.setProofEnv(env);
    }
    
    /** 
     * set timeout
     * @param timeout
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    /**
     * set maximal steps to be performed
     * @param maxSteps
     */
    public void setMaxRuleApplications(int maxSteps) {
        this.maxSteps = maxSteps;
    }
    
    
    public void setStrategy(StrategyProperties sp) {
        this.strategyProperties = (StrategyProperties) sp.clone();
    }
    
   /**
    * starts proof attempt
    * @return the proof after the attempt terminated  
    */
    public ApplyStrategyInfo start() {
        
        final Profile profile = proof.env().getInitConfig().getProfile();
        proof.setActiveStrategy(profile.getDefaultStrategyFactory().create(proof, strategyProperties));
        
        profile.setSelectedGoalChooserBuilder(DepthFirstGoalChooserBuilder.NAME);        
        
        ApplyStrategy prover = 
                new ApplyStrategy(proof.env().getInitConfig().getProfile().getSelectedGoalChooserBuilder().create());
        
        ApplyStrategy.ApplyStrategyInfo result = 
                prover.start(proof, proof.openGoals(), maxSteps, timeout, false);
    
        if (result.isError()) {
            throw new RuntimeException("Proof attempt failed due to exception:"+result.getException(),
                    result.getException());
        }
        
        return result;
    }    
}