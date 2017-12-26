package de.rwth.i2.attestor.counterexampleGeneration;

import de.rwth.i2.attestor.ipa.FragmentedHeapConfiguration;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.executionMessages.NondeterminismMessage;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.AbstractMethod;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeCleanup;
import de.rwth.i2.attestor.stateSpaceGeneration.*;

import java.util.Collections;
import java.util.Set;

/**
 * A tailored observer that determines the required successor states of
 * procedure calls to guide the counterexample generation.
 *
 * @author Christoph
 */
final class CounterexampleSymbolicExecutionObserver implements SymbolicExecutionObserver {

    private final StateSpaceGenerator stateSpaceGenerator;
    private final CounterexampleStateSpaceSupplier stateSpaceSupplier;
    private final Trace trace;

    private ProgramState requiredFinalState = null;
    private int requiredNoOfFinalStates = 1;

    CounterexampleSymbolicExecutionObserver(StateSpaceGenerator stateSpaceGenerator,
                                            Trace trace) {

        this.stateSpaceGenerator = stateSpaceGenerator;
        this.stateSpaceSupplier = (CounterexampleStateSpaceSupplier) stateSpaceGenerator.getStateSpaceSupplier();
        this.trace = trace;

    }

    @Override
    public void update(Object handler, ProgramState input) {

        if (input.isFromTopLevelStateSpace() && handler instanceof InvokeCleanup) {
            updateInvoke((InvokeCleanup) handler, input);
        } else if (handler instanceof AbstractMethod) {
            updateMethod((AbstractMethod) handler, input);
        } else if (handler.getClass() == NondeterminismMessage.class) {
            /* Since nondeterminism due to overapproximation cannot occur during counterexample generation
               (we never perform abstraction), we know at this point that nondeterminism was caused by
               some kind of unsupported operation, such as arithmetical operations. In this case, we play
               safe and report that our counterexample generation failed.
             */
            throw new IllegalStateException("Counterexample might be spurious due to encountered nondeterminism" +
                    " caused by an unsupported program statement.");
        } else if (handler.getClass() == FragmentedHeapConfiguration.class) {
            updateFragmentedHc((FragmentedHeapConfiguration) handler);
        }
    }

    private void updateInvoke(InvokeCleanup invokeCleanup, ProgramState input) {

        requiredFinalState = trace.getSuccessor(input);
        stateSpaceSupplier.setInvokeCleanupOfPreviousProcedure(invokeCleanup, this);
    }

    private void updateMethod(AbstractMethod method, ProgramState input) {

        if (requiredFinalState != null) {
            requiredNoOfFinalStates = 1;
            stateSpaceSupplier.setFinalStatesOfPreviousProcedure(
                    Collections.singleton(requiredFinalState)
            );
        } else {
            method.setReuseResults(true);
            Set<ProgramState> finalStates = method.getFinalStates(input, this);

            stateSpaceSupplier.setFinalStatesOfPreviousProcedure(finalStates);
            requiredNoOfFinalStates = finalStates.size();
        }
        method.setReuseResults(false);
    }

    private void updateFragmentedHc(FragmentedHeapConfiguration fragmentedHc) {

        stateSpaceSupplier.setFragmentedHcOfPreviousProcedure(fragmentedHc);
    }

    @Override
    public StateSpace generateStateSpace(ProgramImpl program, ProgramState input)
            throws StateSpaceGenerationAbortedException {

        return StateSpaceGenerator
                .builder(stateSpaceGenerator)
                .setExplorationStrategy(new CounterexampleExplorationStrategy())
                .setProgram(program)
                .addInitialState(input)
                .build()
                .generate();
    }

    @Override
    public boolean isDeadVariableEliminationEnabled() {

        return stateSpaceGenerator.isDeadVariableEliminationEnabled();
    }

    private final class CounterexampleExplorationStrategy implements ExplorationStrategy {

        @Override
        public boolean check(ProgramState state, StateSpace stateSpace) {

            return stateSpace.getFinalStates().size() < requiredNoOfFinalStates;
        }
    }
}
