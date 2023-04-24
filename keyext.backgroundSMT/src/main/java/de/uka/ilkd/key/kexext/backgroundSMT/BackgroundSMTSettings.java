package de.uka.ilkd.key.kexext.backgroundSMT;

import de.uka.ilkd.key.rule.Taclet;
import de.uka.ilkd.key.settings.DefaultSMTSettings;
import de.uka.ilkd.key.settings.NewSMTTranslationSettings;
import de.uka.ilkd.key.smt.SMTSettings;
import de.uka.ilkd.key.smt.solvertypes.SolverType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BackgroundSMTSettings implements SMTSettings {

    private final SMTSettings underlyingSettings;

    private long timeout;
    private final List<SolverType> solverTypes = new ArrayList<>();

    public BackgroundSMTSettings(DefaultSMTSettings underlyingSettings) {
        this.underlyingSettings = underlyingSettings;
        this.timeout = underlyingSettings.getTimeout();
    }

    public void setSolverTypes(Collection<SolverType> newSolverTypes) {
        solverTypes.clear();
        solverTypes.addAll(newSolverTypes);
    }

    public void setTimeout(long newTimeout) {
        timeout = newTimeout;
    }

    public Collection<SolverType> getTypes() {
        return new ArrayList<>(solverTypes);
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    @Override
    public long getTimeout(SolverType type) {
        return getTimeout();
    }

    @Override
    public String getSMTTemporaryFolder() {
        return underlyingSettings.getSMTTemporaryFolder();
    }

    @Override
    public int getMaxConcurrentProcesses() {
        return underlyingSettings.getMaxConcurrentProcesses();
    }

    @Override
    public boolean useExplicitTypeHierarchy() {
        return underlyingSettings.useExplicitTypeHierarchy();
    }

    @Override
    public boolean instantiateNullAssumption() {
        return underlyingSettings.instantiateNullAssumption();
    }

    @Override
    public boolean makesUseOfTaclets() {
        return underlyingSettings.makesUseOfTaclets();
    }

    @Override
    public int getMaxNumberOfGenerics() {
        return underlyingSettings.getMaxNumberOfGenerics();
    }

    @Override
    public Collection<Taclet> getTaclets() {
        return underlyingSettings.getTaclets();
    }

    @Override
    public boolean useBuiltInUniqueness() {
        return underlyingSettings.useBuiltInUniqueness();
    }

    @Override
    public boolean useUninterpretedMultiplicationIfNecessary() {
        return underlyingSettings.useUninterpretedMultiplicationIfNecessary();
    }

    @Override
    public boolean useAssumptionsForBigSmallIntegers() {
        return underlyingSettings.useAssumptionsForBigSmallIntegers();
    }

    @Override
    public String getLogic() {
        return underlyingSettings.getLogic();
    }

    @Override
    public long getMaximumInteger() {
        return underlyingSettings.getMaximumInteger();
    }

    @Override
    public long getMinimumInteger() {
        return underlyingSettings.getMinimumInteger();
    }

    @Override
    public long getIntBound() {
        return underlyingSettings.getIntBound();
    }

    @Override
    public long getHeapBound() {
        return underlyingSettings.getHeapBound();
    }

    @Override
    public long getSeqBound() {
        return underlyingSettings.getSeqBound();
    }

    @Override
    public long getObjectBound() {
        return underlyingSettings.getObjectBound();
    }

    @Override
    public long getLocSetBound() {
        return underlyingSettings.getLocSetBound();
    }

    @Override
    public boolean checkForSupport() {
        return underlyingSettings.checkForSupport();
    }

    @Override
    public boolean invarianForall() {
        return underlyingSettings.invarianForall();
    }

    @Override
    public NewSMTTranslationSettings getNewSettings() {
        return underlyingSettings.getNewSettings();
    }

}
