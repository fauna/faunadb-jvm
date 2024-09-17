package com.faunadb.common;

/**
 * The HTTP Connection adapter for FaunaDB drivers.
 */
public class Connection {
    public Connection() {
        throw new UnsupportedOperationException(
                "Fauna is decommissioning FQL v4 on June 30, 2025. This driver is not compatible with FQL v10. " +
                        "Fauna accounts created after August 21, 2024 must use FQL v10. " +
                        "Ensure you migrate existing projects to the official v10 driver by the v4 EOL date: https://github.com/fauna/fauna-jvm."
        );
    }
}