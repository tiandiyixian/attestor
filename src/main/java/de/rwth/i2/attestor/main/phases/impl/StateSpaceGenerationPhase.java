package de.rwth.i2.attestor.main.phases.impl;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.phases.AbstractPhase;
import de.rwth.i2.attestor.main.phases.transformers.InputTransformer;
import de.rwth.i2.attestor.main.phases.transformers.ProgramTransformer;
import de.rwth.i2.attestor.main.phases.transformers.StateSpaceTransformer;
import de.rwth.i2.attestor.stateSpaceGeneration.Program;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerator;

import java.util.List;

public class StateSpaceGenerationPhase extends AbstractPhase implements StateSpaceTransformer {

    private StateSpace stateSpace;

    @Override
    public String getName() {

        return "State space generation";
    }

    @Override
    protected void executePhase() {

        settings.factory().resetTotalNumberOfStates();

        Program program = getPhase(ProgramTransformer.class).getProgram();
        List<HeapConfiguration> inputs = getPhase(InputTransformer.class).getInputs();

        StateSpaceGenerator stateSpaceGenerator = settings
                .factory()
                .createStateSpaceGenerator(
                        program,
                        inputs,
                        0
                );

        printAnalyzedMethod();

        stateSpace = stateSpaceGenerator.generate();
        logger.info("State space generation finished. #states: "
                + settings.factory().getTotalNumberOfStates());
    }

    private void printAnalyzedMethod() {

        logger.info("Analyzing '"
                + settings.input().getClasspath()
                + "/"
                + settings.input().getClassName()
                + "."
                + settings.input().getMethodName()
                + "'..."
        );
    }

    @Override
    public void logSummary() {

        logger.info("State space generation summmary:");
        logger.info("+----------------------------------+--------------------------------+");
        logger.info(String.format("| # states w/ procedure calls      | %30d |",
                settings.factory().getTotalNumberOfStates()));
        logger.info(String.format("| # states w/o procedure calls     | %30d |",
                stateSpace.getStates().size()));
        logger.info(String.format("| # final states                   | %30d |",
                stateSpace.getFinalStates().size()));
        logger.info("+-----------+----------------------+--------------------------------+");
    }

    @Override
    public StateSpace getStateSpace() {

        return stateSpace;
    }
}