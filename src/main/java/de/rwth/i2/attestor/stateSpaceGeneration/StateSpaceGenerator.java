package de.rwth.i2.attestor.stateSpaceGeneration;

import de.rwth.i2.attestor.main.settings.Settings;
import de.rwth.i2.attestor.util.NotSufficientlyMaterializedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * A StateSpaceGenerator takes an analysis and generates a
 * state space from it. <br>
 * Initialization of a StateSpaceGenerator has to be performed
 * by calling the static method StateSpaceGenerator.builder().
 * This yields a builder object to configure the analysis used during
 * state space generation.
 * <br>
 *  The generation of a StateSpace itself is started by invoking generate().
 *  
 * @author christoph
 *
 */
public class StateSpaceGenerator {

	/**
	 * @return An SSGBuilder which is the only means to create a new
	 * StateSpaceGenerator object.
	 */
	public static SSGBuilder builder() {
		return new SSGBuilder();
	}	

	private final static Logger logger = LogManager.getLogger("StateSpaceGenerator");

	@FunctionalInterface
	public interface TotalStatesCounter {

		void addStates(int states);
	}

	/**
	 * Stores the state space generated upon instantiation of
	 * this generator.
	 */
	StateSpace stateSpace = new InternalStateSpace(Settings.getInstance().options().getMaxStateSpaceSize());

	/**
	 * Stores the program configurations that still have
	 * to be executed by the state space generation
	 */
	final Stack<ProgramState> unexploredConfigurations = new Stack<>();

	/**
	 * The control flow of the program together with the
	 * abstract semantics of each statement
	 */
	Program program = null;

	/**
	 * Strategy guiding the materialization of states.
	 * This strategy is invoked whenever an abstract transfer
	 * function cannot be executed.
	 */
	MaterializationStrategy materializationStrategy = null;

	/**
	 * Strategy guiding the canonicalization of states.
	 * This strategy is invoked after execution of abstract transfer
	 * functions in order to generalize.
	 */
	CanonicalizationStrategy canonicalizationStrategy = null;

	/**
	 * Strategy determining when to give up on further state space
	 * exploration.
	 */
	AbortStrategy abortStrategy = null;

	/**
	 * Strategy determining the labels of states in the state space
	 */
	StateLabelingStrategy stateLabelingStrategy = null;

	/**
	 * Strategy determining how states are refined prior to canonicalization
	 */
	StateRefinementStrategy stateRefinementStrategy = null;

	/**
	 * Counter for the total number of states generated so far.
	 */
	TotalStatesCounter totalStatesCounter = null;

	/**
	 * @return The strategy determining when state space generation is aborted.
	 */
	public AbortStrategy getAbortStrategy() {
		return abortStrategy;
	}

	/**
	 * @return The strategy determining how materialization is performed.
	 */
	public MaterializationStrategy getMaterializationStrategy() {
		return materializationStrategy;
	}

	/**
	 * @return The strategy determining how canonicalization is performed.
	 */
	public CanonicalizationStrategy getCanonizationStrategy() {
		return canonicalizationStrategy;
	}

	/**
	 * @return The strategy determining how states are labeled with atomic propositions.
	 */
	public StateLabelingStrategy getStateLabelingStrategy() {
		return stateLabelingStrategy;
	}

	/**
	 * @return The strategy determining how states are refined prior to canonicalization.
	 */
	public StateRefinementStrategy getStateRefinementStrategy() {
		return stateRefinementStrategy;
	}

	/**
	 * Attempts to generate a StateSpace according to the
	 * underlying analysis.
	 * @return The generated StateSpace.
	 */
	public StateSpace generate() {

		while( hasUnexploredStates() ){
			ProgramState state = unexploredConfigurations.pop();
			boolean isSufficientlyMaterialized = materializationPhase(state);

			if(isSufficientlyMaterialized) {
				Set<ProgramState> successorStates = executionPhase(state);
				if(successorStates.isEmpty()) {
					stateSpace.setFinal(state);
				} else {
					successorStates.forEach(nextState -> {
						nextState = stateRefinementStrategy.refine(nextState);
						nextState = canonicalizationPhase(nextState);
						stateLabelingStrategy.computeAtomicPropositions(nextState);
						addingPhase(state, nextState);
					});
				}
			}
		}

		totalStatesCounter.addStates(stateSpace.getStates().size());
		return stateSpace;
	}

	/**
	 * @return true iff further states can and should be generated.
	 */
	private boolean hasUnexploredStates(){

		return !unexploredConfigurations.isEmpty() && abortStrategy.isAllowedToContinue( stateSpace );
	}

	/**
	 * In the materialization phase violation points of the given state are removed until the current statement
	 * can be executed. The materialized states are immediately added to the state space as successors of the
	 * given state.
	 * @param state The program state that should be materialized.
	 * @return True if and only if no materialization is needed.
	 */
	private boolean materializationPhase(ProgramState state) {

		Semantics semantics = program.getStatement(state.getProgramCounter());
		List<ProgramState> materialized = materializationStrategy.materialize(
				state,
				semantics.getPotentialViolationPoints()
				);

		for(ProgramState m : materialized) {

			// performance optimization that prevents isomorphism checks against states in the state space.
			// This is achieved by making the hash code of the materialized state different from existing states.
			stateSpace.addState(m);
			unexploredConfigurations.add(m);
			stateSpace.addMaterializationTransition(state, m);
		}
		return materialized.isEmpty();
	}

	/**
	 * Computes canonical successors of the given program state.
	 *
	 * @param state The program state whose successor states shall be computed.
	 */
	private Set<ProgramState> executionPhase(ProgramState state ){

		Semantics semantics = program.getStatement( state.getProgramCounter() );
		try {
			return semantics.computeSuccessors(state);
		} catch (NotSufficientlyMaterializedException e) {
			logger.error("A state could not be sufficiently materialized.");
			return new HashSet<>();
		}
	}

	private ProgramState canonicalizationPhase(ProgramState state) {

		Semantics semantics = program.getStatement(state.getProgramCounter());
		if(semantics.permitsCanonicalization()) {
			state = canonicalizationStrategy.canonicalize(semantics, state);
		}
		return state;
	}

	/**
	 * Adds a state as a successor of the given previous state to the state space
	 * provided to no subsuming state already exists.
	 * @param previousState The predecessor of the given state.
	 * @param state The state that should be added.
	 */
	private void addingPhase(ProgramState previousState, ProgramState state) {

		// this ensures that we never ever add further states if the abort strategy has triggered somewhere
		// before hitting the top of the state space generation loop again.
		if(!abortStrategy.isAllowedToContinue(stateSpace)) {
			return;
		}

		// performance optimization that prevents isomorphism checks against states in the state space.
		// This is achieved by making the hash code of the materialized state different from existing states.
		Semantics semantics = program.getStatement(state.getProgramCounter());
		if(semantics.hasUniqueSuccessor() || !semantics.permitsCanonicalization()) {
			stateSpace.addState(state);
			unexploredConfigurations.add(state);
		} else if(stateSpace.addStateIfAbsent(state)) {
			unexploredConfigurations.add(state);
		}

		stateSpace.addControlFlowTransition(previousState, state);
	}

	/**
	 * @return The initial state of the generated state space.
	 */
	public Set<ProgramState> getInitialStates() {
		return stateSpace.getInitialStates();
	}
}
