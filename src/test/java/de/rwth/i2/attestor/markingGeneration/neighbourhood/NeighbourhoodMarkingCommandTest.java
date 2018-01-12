package de.rwth.i2.attestor.markingGeneration.neighbourhood;

import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.grammar.materialization.ViolationPoints;
import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;
import de.rwth.i2.attestor.types.Type;
import de.rwth.i2.attestor.util.NotSufficientlyMaterializedException;
import gnu.trove.list.array.TIntArrayList;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NeighbourhoodMarkingCommandTest {

    private static final int NEXT_PC = 23;

    private SceneObject sceneObject;
    private Type type;
    private SelectorLabel selA;
    private SelectorLabel selB;

    private String markingName;
    private String markingSeparator;
    private Collection<String> availableSelectorNames;

    private NeighbourhoodMarkingCommand command;

    @Before
    public void setUp() {

        sceneObject = new MockupSceneObject();
        type = sceneObject.scene().getType("type");
        selA = sceneObject.scene().getSelectorLabel("selA");
        selB = sceneObject.scene().getSelectorLabel("selB");
        markingName = "%marking";
        markingSeparator = "-";

        availableSelectorNames = new LinkedHashSet<>();
        availableSelectorNames.add("selA");
        availableSelectorNames.add("selB");

        command = new NeighbourhoodMarkingCommand(NEXT_PC, markingName,
                markingSeparator, availableSelectorNames);
    }

    @Test
    public void testPotentialViolationPoints() {

        ViolationPoints violationPoints = command.getPotentialViolationPoints();
        Set<String> variables = violationPoints.getVariables();

        Set<String> expectedVariables = new LinkedHashSet<>();
        expectedVariables.add(markingName);
        expectedVariables.add(markingName + markingSeparator + selA);
        expectedVariables.add(markingName + markingSeparator + selB);

        assertEquals(expectedVariables, variables);
    }

    @Test
    public void testCanonicalization() {

        assertTrue(command.needsCanonicalization());
    }

    @Test
    public void testSuccessorPCs() {

        assertEquals(Collections.singleton(NEXT_PC), command.getSuccessorPCs());
    }

    @Test
    public void testSimpleComputeSuccessors() {

        ProgramState baseState = sceneObject.scene().createProgramState();
        TIntArrayList nodes = new TIntArrayList();
        baseState.getHeap()
                .builder()
                .addNodes(type, 3, nodes)
                .addSelector(nodes.get(0), selA, nodes.get(1))
                .addSelector(nodes.get(0), selB, nodes.get(2))
                .build();

        ProgramState inputState = baseState.clone();
        inputState.getHeap()
                .builder()
                .addVariableEdge(markingName, nodes.get(0))
                .addVariableEdge(markingName+markingSeparator+selA.getLabel(), nodes.get(1))
                .addVariableEdge(markingName+markingSeparator+selB.getLabel(), nodes.get(2))
                .build();

        Set<ProgramState> expected = new LinkedHashSet<>();

        ProgramState firstExpected = baseState.clone();
        firstExpected.getHeap()
                .builder()
                .addVariableEdge(markingName, nodes.get(1))
                .build();
        expected.add(firstExpected);

        ProgramState secondExpected = baseState.clone();
        secondExpected.getHeap()
                .builder()
                .addVariableEdge(markingName, nodes.get(2))
                .build();
        expected.add(secondExpected);

        Collection<ProgramState> resultStates = null;
        try {
            resultStates = command.computeSuccessors(inputState);
        } catch (NotSufficientlyMaterializedException | StateSpaceGenerationAbortedException e) {
            fail();
        }

        assertEquals(expected, resultStates);



    }
}
