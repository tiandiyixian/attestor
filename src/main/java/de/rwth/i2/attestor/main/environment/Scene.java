package de.rwth.i2.attestor.main.environment;

import de.rwth.i2.attestor.types.Type;

public interface Scene {

    Type getType(String name);
}
