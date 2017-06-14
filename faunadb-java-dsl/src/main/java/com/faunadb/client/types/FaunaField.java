package com.faunadb.client.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Maps a field/getter or constructor parameter to FaunaDB object property while encoding/decoding an object.</p>
 * <p>
 * <pre>{@code
 * class Car {
 *     @FaunaField("model")
 *     private string model;
 *
 *     @FaunaField("manufacturer")
 *     private string manufacturer;
 * }
 *
 * class Product {
 *     private string description;
 *     private double price;
 *
 *     @FaunaConstructor
 *     public Product(@FaunaField("Description") string description, @FaunaField("Price") double price) {
 *         this.description = description;
 *         this.price = price;
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface FaunaField {
  String value() default "";
}
