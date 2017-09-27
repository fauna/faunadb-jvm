package com.faunadb.client.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Instruct the encoder/decoder to rename the annotated enum field.</p>
 *
 * <p>If absent, the method {@link Enum#name()} will be used instead.</p>
 *
 * <pre><code>
 * enum CpuTypes {
 *     &#64;FaunaEnum("x86_32") X86,
 *     &#64;FaunaEnum("x86_64") X86_64,
 *     ARM,
 *     MIPS
 * }
 * </code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FaunaEnum {
  String value();
}
