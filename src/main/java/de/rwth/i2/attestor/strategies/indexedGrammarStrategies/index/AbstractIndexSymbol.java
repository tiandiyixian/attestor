package de.rwth.i2.attestor.strategies.indexedGrammarStrategies.index;

import java.util.HashMap;
import java.util.Map;

public class AbstractIndexSymbol implements IndexSymbol {

	private static final Map<String, AbstractIndexSymbol> existingSymbols = new HashMap<>();

	public static synchronized AbstractIndexSymbol get(String label ){
		if( ! existingSymbols.containsKey(label) ){
			existingSymbols.put(label, new AbstractIndexSymbol(label));
		}
		return existingSymbols.get(label);
	}

	private final String label;

	private AbstractIndexSymbol(String label) {
		super();
		this.label = label;
	}

	@Override
	public boolean isBottom() {
		return false;
	}

	public boolean equals( Object other ){

		return this == other;

	}

	public int hashCode(){
		return label.hashCode();
	}

	public String toString(){
		return this.label;
	}
}