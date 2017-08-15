package com.faunadb.client.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Maps a field, getter, or constructor parameter to an object field in FaunaDB when encoding or decoding an object.</p>
 * <p>
 * <pre>{@code
 * class Car {
 *     @FaunaField("model")
 *     private String model;
 *
 *     @FaunaField("manufacturer")
 *     private String manufacturer;
 * }
 *
 * class Product {
 *     private String description;
 *     private double price;
 *
 *     @FaunaConstructor
 *     public Product(@FaunaField("description") String description, @FaunaField("price") double price) {
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
