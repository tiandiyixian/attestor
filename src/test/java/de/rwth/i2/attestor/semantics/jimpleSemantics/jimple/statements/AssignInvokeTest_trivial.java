package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;

import de.rwth.i2.attestor.UnitTestGlobalSettings;
import de.rwth.i2.attestor.grammar.Grammar;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.internal.ExampleHcImplFactory;
import de.rwth.i2.attestor.main.settings.Settings;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.AbstractMethod;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeHelper;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.StaticInvokeHelper;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.Local;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.Semantics;
import de.rwth.i2.attestor.tasks.defaultTask.DefaultState;
import de.rwth.i2.attestor.types.Type;
import de.rwth.i2.attestor.types.TypeFactory;
import de.rwth.i2.attestor.util.NotSufficientlyMaterializedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class AssignInvokeTest_trivial {
	//private static final Logger logger = LogManager.getLogger( "AssignInvokeTest.java" );

	private AssignInvoke stmt;
	private HeapConfiguration inputGraph;
	private DefaultState inputState;


	@BeforeClass
	public static void init() {

		UnitTestGlobalSettings.reset();
	}

	@Before
	public void setUp() throws Exception{
		Settings.getInstance().grammar().setGrammar( Grammar.builder().build() );
		
		Type type = TypeFactory.getInstance().getType( "node" );
		Local var 
			= new Local( type, "x" );


		AbstractMethod method = new AbstractMethod( "method", StateSpaceFactoryHelper.get());
		List<Semantics> defaultControlFlow = new ArrayList<>();
		defaultControlFlow.add( new Skip( -1 ) );
		method.setControlFlow( defaultControlFlow );
		InvokeHelper invokePrepare = new StaticInvokeHelper( new ArrayList<>(), new ArrayList<>() );
		
		stmt = new AssignInvoke( var, method, invokePrepare, 1 );
		
		inputGraph = ExampleHcImplFactory.getListAndConstants();
		inputState = new DefaultState( inputGraph );
	}

	@Test
	public void testComputeSuccessors(){
		try{
			Set<ProgramState> resStates = stmt.computeSuccessors( inputState );
			assertEquals( 1, resStates.size() );
			DefaultState resState = (DefaultState) resStates.iterator().next();
			assertNotSame( resState, inputState );
			assertNotSame( inputGraph, resState.getHeap() );
			assertSame( inputGraph, inputState.getHeap() );
			assertEquals( ExampleHcImplFactory.getListAndConstants(), inputGraph );
			assertEquals( inputGraph, resState.getHeap());
		}catch( NotSufficientlyMaterializedException e ){
			fail("unexpected exception: " + e.getMessage());
		}
	}
	


	@Test
	public void testNeedsMaterialization(){
		assertFalse( stmt.needsMaterialization( inputState ) );
	}

	@Test
	public void testToString(){
		assertEquals( "x = method();", stmt.toString() );
	}
}
