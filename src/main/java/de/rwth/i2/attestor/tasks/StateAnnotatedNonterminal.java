package de.rwth.i2.attestor.tasks;

import de.rwth.i2.attestor.automata.HeapAutomatonState;
import de.rwth.i2.attestor.automata.StateAnnotatedSymbol;
import de.rwth.i2.attestor.graph.Nonterminal;

/**
 * A nonterminal symbol that is additionally annotated with a state of a heap automaton.
 *
 * @author Christoph
 */
public class StateAnnotatedNonterminal implements Nonterminal, StateAnnotatedSymbol {

    /**
     * The actual nonterminal symbol.
     */
    private Nonterminal nonterminal;

    /**
     * The state the nonterminal is annotated with.
     */
    private HeapAutomatonState state;

    public StateAnnotatedNonterminal(Nonterminal nonterminal, HeapAutomatonState state) {
       this.nonterminal = nonterminal;
       this.state = state;
    }

    @Override
    public HeapAutomatonState getState() {

        return state;
    }

    @Override
    public StateAnnotatedSymbol withState(HeapAutomatonState state) {

        return new StateAnnotatedNonterminal(nonterminal, state);
    }

    @Override
    public int getRank() {

        return nonterminal.getRank();
    }

    @Override
    public boolean isReductionTentacle(int tentacle) {

        return nonterminal.isReductionTentacle(tentacle);
    }

    @Override
    public void setReductionTentacle(int tentacle) {

        nonterminal.setReductionTentacle(tentacle);
    }

    @Override
    public void unsetReductionTentacle(int tentacle) {

        nonterminal.unsetReductionTentacle(tentacle);
    }

    @Override
    public boolean labelMatches(Nonterminal nonterminal) {

        return nonterminal.labelMatches(nonterminal);
    }

    @Override
    public int compareTo(Nonterminal nonterminal) {

        if(nonterminal instanceof StateAnnotatedNonterminal) {
            StateAnnotatedNonterminal sn = (StateAnnotatedNonterminal) nonterminal;
            if(sn.getState().equals(state) && sn.nonterminal.equals(nonterminal)) {
                return 0;
            }
        }
        return 1;
    }
}
