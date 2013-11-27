package com.unitvectory.pushtospeech.client;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

/**
 * The Password Generator
 * 
 * @author Jared Hatfield
 * 
 */
public class PasswordGenerator {

    private static final Random random = new SecureRandom();

    /**
     * Generates a new identifier.
     * 
     * @return the identifier
     */
    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generates a new secret.
     * 
     * @return the secret
     */
    public static String newSecret() {
        return new BigInteger(130, random).toString(32);
    }
}
