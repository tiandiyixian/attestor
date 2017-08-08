package de.rwth.i2.attestor.grammar.canoncalization;

import static org.junit.Assert.*;

import org.junit.Test;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.internal.InternalHeapConfiguration;
import de.rwth.i2.attestor.graph.heap.matching.AbstractMatchingChecker;
import de.rwth.i2.attestor.graph.heap.matching.EmbeddingChecker;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.ReturnVoidStmt;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.Skip;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.Statement;
import de.rwth.i2.attestor.types.Type;
import de.rwth.i2.attestor.types.TypeFactory;
import gnu.trove.list.array.TIntArrayList;

public class DefaultEmbeddingCheckerProviderTest {

	/**
	 * aggressiveAbstractionThreshold > graphSize.
	 * aggressiveReturnAbstraction = true, statement != return
	 * expect DepthEmbeddingChecker
	 */
	@Test
	public void test() {
		int aggressiveAbstractionThreshold = 10;
		boolean aggressiveReturnAbstraction = true;
		HeapConfiguration graph = getGraphSmallerThan( aggressiveAbstractionThreshold );
		HeapConfiguration pattern = getPattern();
		Statement statement = new Skip(0);
		
		AbstractMatchingChecker expected = graph.getEmbeddingsOf( pattern );
		
		performTest( aggressiveAbstractionThreshold, aggressiveReturnAbstraction, 
					 graph, pattern, statement, expected );
	}
	
	/**
	 * aggressiveAbstractionThreshold < graphSize.
	 * aggressiveReturnAbstraction = false, statement != return
	 * expect  aggressive EmbeddingChecker
	 */
	@Test
	public void test2() {
		int aggressiveAbstractionThreshold = 2;
		boolean aggressiveReturnAbstraction = false;
		HeapConfiguration graph = getGraphBiggerThan( aggressiveAbstractionThreshold );
		HeapConfiguration pattern = getPattern();
		Statement statement = new Skip(0);
		
		AbstractMatchingChecker expected = new EmbeddingChecker(pattern, graph );
		
		performTest( aggressiveAbstractionThreshold, aggressiveReturnAbstraction, 
					 graph, pattern, statement, expected );
	}

	
	/**
	 * aggressiveAbstractionThreshold > graphSize.
	 * aggressiveReturnAbstraction = true, statement == return
	 * expect aggressive EmbeddingChecker
	 */
	@Test
	public void test3() {
		int aggressiveAbstractionThreshold = 10;
		boolean aggressiveReturnAbstraction = true;
		HeapConfiguration graph = getGraphSmallerThan( aggressiveAbstractionThreshold );
		HeapConfiguration pattern = getPattern();
		Statement statement = new ReturnVoidStmt();
		
		AbstractMatchingChecker expected = new EmbeddingChecker(pattern, graph );
		
		performTest( aggressiveAbstractionThreshold, aggressiveReturnAbstraction, 
					 graph, pattern, statement, expected );
	}

	/**
	 * aggressiveAbstractionThreshold > graphSize.
	 * aggressiveReturnAbstraction = false, statement == return
	 * expect DepthEmbeddingChecker
	 */
	@Test
	public void test4() {
		int aggressiveAbstractionThreshold = 10;
		boolean aggressiveReturnAbstraction = false;
		HeapConfiguration graph = getGraphSmallerThan( aggressiveAbstractionThreshold );
		HeapConfiguration pattern = getPattern();
		Statement statement = new ReturnVoidStmt();
		
		AbstractMatchingChecker expected = graph.getEmbeddingsOf( pattern );
		
		performTest( aggressiveAbstractionThreshold, aggressiveReturnAbstraction, 
					 graph, pattern, statement, expected );
	}


	private void performTest(int aggressiveAbstractionThreshold, boolean aggressiveReturnAbstraction,
			HeapConfiguration graph, HeapConfiguration pattern, Statement statement, AbstractMatchingChecker expected) {
		EmbeddingCheckerProvider checkerProvider = 
				new EmbeddingCheckerProvider( aggressiveAbstractionThreshold,
											  aggressiveReturnAbstraction 	);
		
	
		AbstractMatchingChecker checker = checkerProvider.getEmbeddingChecker( graph, pattern, statement );
		
		assertEquals( expected.getClass(), checker.getClass() );
		assertEquals( expected.getPattern(), checker.getPattern());
		assertEquals( expected.getTarget(), checker.getTarget() );
	}

	private HeapConfiguration getPattern() {
	HeapConfiguration hc =  new InternalHeapConfiguration();
		
		Type type = TypeFactory.getInstance().getType("someType");
		
		TIntArrayList nodes = new TIntArrayList();
		return hc.builder().addNodes(type, 1, nodes).build();
	}

	private HeapConfiguration getGraphSmallerThan( int aggressiveAbstractionThreshold ) {
		HeapConfiguration hc =  new InternalHeapConfiguration();
		
		Type type = TypeFactory.getInstance().getType("someType");
		
		TIntArrayList nodes = new TIntArrayList();
		return hc.builder().addNodes(type, aggressiveAbstractionThreshold - 1, nodes).build();
	}

	private HeapConfiguration getGraphBiggerThan(int aggressiveAbstractionThreshold) {
		HeapConfiguration hc =  new InternalHeapConfiguration();
		
		Type type = TypeFactory.getInstance().getType("someType");
		
		TIntArrayList nodes = new TIntArrayList();
		return hc.builder().addNodes(type, aggressiveAbstractionThreshold + 2, nodes).build();
	}
	
	

}
