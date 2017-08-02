package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.boolExpr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rwth.i2.attestor.semantics.jimpleSemantics.JimpleExecutable;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.ConcreteValue;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.NullPointerDereferenceException;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.Value;
import de.rwth.i2.attestor.stateSpaceGeneration.ViolationPoints;
import de.rwth.i2.attestor.types.Type;
import de.rwth.i2.attestor.types.TypeFactory;
import de.rwth.i2.attestor.util.DebugMode;
import de.rwth.i2.attestor.util.NotSufficientlyMaterializedException;

/**
 * Represents expressions of the form x == y
 * @author Hannah Arndt
 */
public class EqualExpr implements Value {

	private static final Logger logger = LogManager.getLogger( "EqualExpr" );

	/**
	 * x in x == y
	 */
	private final Value leftExpr;
	/**
	 * y in x == y
	 */
	private final Value rightExpr;
	/**
	 * boolean
	 */
	private final Type type = TypeFactory.getInstance().getType( "int" );
	
	private final ViolationPoints potentialViolationPoints;

	public EqualExpr( Value leftExpr, Value rightExpr ){
		this.leftExpr = leftExpr;
		this.rightExpr = rightExpr;
		this.potentialViolationPoints = new ViolationPoints();
		this.potentialViolationPoints.addAll(leftExpr.getPotentialViolationPoints());
		this.potentialViolationPoints.addAll(rightExpr.getPotentialViolationPoints());
	}

	/**
	 * evaluates both expressions on the executable and returns the element representing
	 * true if they result in the same element (otherwise false). undefined if one of the expressions 
	 * evaluates to undefined. in this case a warning is issued.
	 * @return the heap element representing true/false or undefined.
	 */
	@Override
	public ConcreteValue evaluateOn( JimpleExecutable executable ) throws NotSufficientlyMaterializedException{

		ConcreteValue leftRes;
		try {
			leftRes = leftExpr.evaluateOn( executable );
		} catch (NullPointerDereferenceException e) {
			logger.error(e.getErrorMessage(this));
			return executable.getUndefined();
		}
		
		if( leftRes.isUndefined() ){
			logger.debug( "leftExpr evaluated to undefined. Returning undefined." );
			return executable.getUndefined();
		}
		ConcreteValue rightRes;
		try {
			rightRes = rightExpr.evaluateOn( executable );
		} catch (NullPointerDereferenceException e) {
			logger.error("Null pointer dereference in " + this);
			return executable.getUndefined();
		}
		if( rightRes.isUndefined() ){
			logger.debug( "rightExpr evaluated to undefined. Returning undefined." );
			return executable.getUndefined();
		}
				
		ConcreteValue res;
		
		if(leftRes.equals( rightRes )) {
			res = executable.getConstant( "true" );
		} else {
			res = executable.getConstant( "false" );
		}

		if( DebugMode.ENABLED && !( res.type().equals( this.type ) ) ){
			String msg = "The type of the resulting ConcreteValue does not match.";
			msg += "\n expected: " + this.type + " got: " + res.type();
			logger.warn( msg );
		}

		return res;
	}

	@Override
	public boolean needsMaterialization( JimpleExecutable executable ) {
		
		return rightExpr.needsMaterialization( executable ) || leftExpr.needsMaterialization( executable );
	}


	@Override
	public Type getType(){
		return this.type;
	}

	/**
	 * "left == right"
	 */
	public String toString(){
		return leftExpr + " == " + rightExpr;
	}

	@Override
	public ViolationPoints getPotentialViolationPoints() {
		
		return potentialViolationPoints;
	}
}
