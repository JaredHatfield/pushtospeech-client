package com.unitvectory.pushtospeech.client;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

public class PasswordGenerator {

    private static final Random random = new SecureRandom();

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String newSecret() {
        return new BigInteger(130, random).toString(32);
    }
}
