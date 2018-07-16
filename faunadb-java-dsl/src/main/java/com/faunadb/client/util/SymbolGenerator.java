package com.faunadb.client.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The singleton SymbolGenerator is used to automatically generate
 * symbol names when a Java lambda function is provided where a
 * FaunaDB lambda expression is accepted. The symbol space is 2^64 and
 * thread-safe.
 */
final public class SymbolGenerator {
    private static final AtomicLong counter = new AtomicLong(0);
    
    private SymbolGenerator() {
    }

    /**
     * Generate a new symbol with the provided prefix.
     *
     * @param prefix the symbol prefix
     * @return a new symbol
     */
    public static String genSym(String prefix) {
        long i = counter.incrementAndGet();
        return String.format("%s%s", prefix, i);
    }
}
