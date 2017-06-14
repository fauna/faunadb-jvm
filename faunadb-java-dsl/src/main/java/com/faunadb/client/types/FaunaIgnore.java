package com.faunadb.client.types;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>Instruct the encoder/decoder to not encode/decode the specified member.</p>
 *
 * <pre>{@code
 * class User {
 *     private string userName;
 *     private string password;
 *
 *     @FaunaField("user_name")
 *     public String getUserName() { return userName; }
 *
 *     @FaunaIgnore
 *     public String getPassword() { return password; }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FaunaIgnore {
}
