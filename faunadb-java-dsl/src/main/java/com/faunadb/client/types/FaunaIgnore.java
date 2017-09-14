package com.faunadb.client.types;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>Instructs the encoder/decoder to not encode/decode the annotated member.</p>
 *
 * <pre><code>
 * class User {
 *     private String userName;
 *     private String password;
 *
 *     &#64;FaunaField("user_name")
 *     public String getUserName() { return userName; }
 *
 *     &#64;FaunaIgnore
 *     public String getPassword() { return password; }
 * }
 * </code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FaunaIgnore {
}
