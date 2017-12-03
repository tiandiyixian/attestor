package de.rwth.i2.attestor.semantics.jimpleSemantics.translation;

import java.util.*;

import de.rwth.i2.attestor.main.environment.SceneObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rwth.i2.attestor.ipa.IpaAbstractMethod;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.Skip;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.Statement;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.AbstractMethod;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.Value;
import de.rwth.i2.attestor.stateSpaceGeneration.Semantics;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;
import de.rwth.i2.attestor.types.Type;
import soot.*;
import soot.jimple.Stmt;
import soot.util.Chain;

/**
 * This class organizes the actual translation process by splitting a Jimple program
 * into methods and methods into units. The lower translation layers can ask
 * this element for the pc of a given unit and the method-translation for a
 * given method-signature.
 * 
 * The translation process is started by translate().
 * 
 * This class does not specify actual translation rules for statements/values/types. Those
 * can be found in the translation hierarchy starting in {@link #firstLevel}
 * 
 * @author Hannah Arndt, Christoph
 *
 */
public class TopLevelTranslation extends SceneObject implements JimpleToAbstractSemantics {

	/**
	 * The logger for this class.
	 */
	private static final Logger logger = LogManager.getLogger("TopLevelTranslation");

	/**
	 * Maps the Units from the Jimple control flow of the method which is
	 * currently translated to program counters.
	 */
	private Map<Unit, Integer> currentUnitToPC;
	
	/**
	 * necessary to fill the call graph during translation
	 */
	private IpaAbstractMethod currentMethod;

	/**
	 * The next level in the translation hierarchy. This level is the first one
	 * to actually translate statements/values/types.
	 */
	private final JimpleToAbstractSemantics firstLevel;
	
	/**
	 * Instance of Tarjan's Algorithm to find SCCs. Has to be filled with all call-edges
	 * and can then be used to mark all recursive methods as such. Necessary to later compute
	 * fixpoints for these methods instead of descending infinitely.
	 */
	private final TarjanAlgorithm recursiveMethodDetection = new TarjanAlgorithm();

	/**
	 * Default initialization for TopLevelTranslation.
	 * Sets the firstLevel of the translation hierarchy to
	 * {@link StandardAbstractSemantics}
	 */
	public TopLevelTranslation(SceneObject sceneObject) {
		super(sceneObject);
		firstLevel = new StandardAbstractSemantics(this);
	}

	/**
	 * Initializes the TopLevelTranslation with a custom first level.
	 * @param firstLevel The custom first level of the translation process.
	 */
	public TopLevelTranslation(SceneObject sceneObject, JimpleToAbstractSemantics firstLevel ){
		super(sceneObject);
		this.firstLevel = firstLevel;
		firstLevel.setTopLevel(this);
	}

	/**
	 * First fills the methodMapping with new abstractMethods for each method in
	 * the main class. Then fills these methods each with the corresponding
	 * translation of statements.<br>
	 * Assumes that soot.Scene already containsSubsumingState the Jimple code that should be
	 * translated.
	 * 
	 * @see #translateMethod(SootMethod)
	 */
	public void translate() {

		ArrayList<SootMethod> methods = new ArrayList<>();

		//SootClass mainClass = Scene.v().getMainClass();

		// Determine all necessary (non-library) classes and its methods
		Chain<SootClass> sootClasses = Scene.v().getApplicationClasses();
		for( SootClass sootClass : sootClasses ){
			methods.addAll( sootClass.getMethods() );
		}
	
		for (SootMethod method : methods) {
			logger.trace("Found soot method: " + method.getSignature());
			String shortName = shortMethodSignature(method);
			String signature = method.getSignature();

            final IpaAbstractMethod abstractMethod = scene().getMethod(signature);
            abstractMethod.setDisplayName(shortName);
            recursiveMethodDetection.addMethodAsVertex(abstractMethod);
		}
		for (SootMethod method : methods) {
			translateMethod(method);
		}
		
		recursiveMethodDetection.markRecursiveMethods();
	}

	/**
	 * Computes a shortened version of a method name that does not include all packages as a prefix.
	 *
	 * @param method The method whose shortened signature should be determined.
	 * @return A shortened method signature without package information.
	 */
	private String shortMethodSignature(SootMethod method) {
		StringBuilder params = new StringBuilder("(");
		boolean isFirst = true;
		for (soot.Type type : method.getParameterTypes()) {
			if (!isFirst) {
				params.append(", ");
			}
			isFirst = false;
			params.append(getShortName(type));
		}
		params.append(")");
		return getShortName(method.getReturnType()) + " " + method.getName() + params;
	}

	/**
	 * Computes a shortened version of a type name that does not include all packages as a prefix.
	 *
	 * @param type The type whose shortened name should be determined.
	 * @return The shortened name of the given type.
	 */
	private String getShortName(soot.Type type) {

		// escaped name was recently removed in soot.Type
		//String[] splitted = type.getEscapedName().split("\\.");

		String[] splitted = type.toString().split("\\.");
		return splitted[splitted.length - 1];
	}

	/**
	 * Sets {@link #currentUnitToPC} to a mapping with the units in this method.
	 * Then translates each statement and sets the resulting list as control
	 * flow in the corresponding abstractMethod.
	 * 
	 * @param method The method to translate.
	 */
	private void translateMethod( SootMethod method ) {

		currentMethod = scene().getMethod(method.getSignature());
		currentUnitToPC = new HashMap<>();

		Chain<Unit> units = method.getActiveBody().getUnits();
		Unit curr = units.getFirst();

		for (int i = 0; i < units.size(); i++) {
			currentUnitToPC.put(curr, i);
			curr = units.getSuccOf(curr);
		}

		List<Semantics> programStatements = new ArrayList<>();

		curr = units.getFirst();
		for (int i = 0; i < units.size(); i++) {
			programStatements.add(translateStatement((soot.jimple.Stmt) curr, i));
			curr = units.getSuccOf(curr);
		}

		logger.trace("method Name: " + method.getSignature());
		
		currentMethod.setControlFlow(programStatements);

	}

	@Override
	public Statement translateStatement(Stmt input, int pc) {
		return firstLevel.translateStatement(input, pc);
	}

	@Override
	public Value translateValue(soot.Value input) {
		return firstLevel.translateValue(input);
	}

	@Override
	public Type translateType(soot.Type input) {
		return firstLevel.translateType(input);
	}

	/**
	 * Gets the program counter associated with the requested unit in the method
	 * which is currently translated.
	 * 
	 * @param unit A unit in the jimple control flow.
	 * @return The associated program counter.
	 */
	int getPCforUnit(Unit unit) {
		return this.currentUnitToPC.get(unit);
	}

	/**
	 * Gets the abstract method for the given signature. If the method is not
	 * from the main class, it creates a default method with an empty body for
	 * this signature and adds it to the methodMapping.
	 * 
	 * @param signature The signature of the requested method.
	 * @return The corresponding abstract method.
	 */
	public AbstractMethod getMethod(String signature) throws StateSpaceGenerationAbortedException {
		IpaAbstractMethod res = scene().getMethod(signature);
		recursiveMethodDetection.addCallEdge(currentMethod, res);
		if (res.getControlFlow() == null) {
			
			String displayName = shortMethodSignature( Scene.v().getMethod(signature) );
			res.setDisplayName(displayName);
			
			List<Semantics> defaultControlFlow = new ArrayList<>();
			defaultControlFlow.add(new Skip(this,-1));
			res.setControlFlow(defaultControlFlow);

			logger.warn("Method " + signature + " replaced by empty default method.");

		}
		
		return res;
	}

	@Override
	public void setTopLevel(TopLevelTranslation topLevel) {
		// not necessary		
	}
}
