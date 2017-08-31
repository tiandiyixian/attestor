package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;

import de.rwth.i2.attestor.semantics.jimpleSemantics.JimpleProgramState;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.JimpleUtil;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.AbstractMethod;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeHelper;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.ViolationPoints;
import de.rwth.i2.attestor.util.NotSufficientlyMaterializedException;
import de.rwth.i2.attestor.util.SingleElementUtil;

import java.util.Set;

/**
 * InvokeStmt models statements like foo(); or bar(1,2);
 * 
 * @author Hannah Arndt
 *
 */
public class InvokeStmt extends Statement {

	/**
	 * the abstract representation of the called method
	 */
	private final AbstractMethod method;
	/**
	 * handles arguments, and if applicable the this-reference. Also manages the
	 * variable scope.
	 */
	private final InvokeHelper invokePrepare;
	/**
	 * the program location of the successor state
	 */
	private final int nextPC;

	public InvokeStmt( AbstractMethod method, InvokeHelper invokePrepare, int nextPC ){
		super();
		this.method = method;
		this.invokePrepare = invokePrepare;
		this.nextPC = nextPC;
	}

	/**
	 * gets the fixpoint from the
	 * {@link de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.SimpleAbstractMethod
	 * AbstractMethod} for the input heap and returns it for the successor
	 * location.<br>
	 * 
	 * If any variable appearing in the arguments is not live at this point,
	 * it will be removed from the heap to enable abstraction.
	 */
	@Override
	public Set<ProgramState> computeSuccessors( ProgramState programState )
			throws NotSufficientlyMaterializedException{
		
		JimpleProgramState jimpleProgramState = JimpleUtil.deepCopy( (JimpleProgramState) programState );

		invokePrepare.prepareHeap( jimpleProgramState );

		Set<ProgramState> methodResult = method.getResult( jimpleProgramState.getHeap(), jimpleProgramState.getScopeDepth() );
		methodResult.forEach( x -> invokePrepare.cleanHeap( (JimpleProgramState) x ) );
		methodResult.forEach( x -> ( (JimpleProgramState) x ).clone() );
		methodResult.forEach( x -> x.setProgramCounter(nextPC) );
		
		return methodResult;
	}

	@Override
	public boolean needsMaterialization( ProgramState programState ){
		return invokePrepare.needsMaterialization( (JimpleProgramState) programState );
	}


	public String toString(){
		return method.toString() + "(" + invokePrepare.argumentString() + ");";
	}

	@Override
	public boolean hasUniqueSuccessor() {

		return false;
	}
	
	@Override
	public ViolationPoints getPotentialViolationPoints() {
		
		return invokePrepare.getPotentialViolationPoints();
	}
	
	@Override
	public Set<Integer> getSuccessorPCs() {
		
		return SingleElementUtil.createSet(nextPC);
	}

}
