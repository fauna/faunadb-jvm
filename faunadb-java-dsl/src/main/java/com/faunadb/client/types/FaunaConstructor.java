package com.faunadb.client.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Specifies which constructor to use when decoding an object with a {@link Decoder}.
 * You can also annotate a public static method, which will then be used instead of a constructor.</p>
 *
 * <p>This annotation can only be used once per class.</p>
 *
 * <pre>{@code
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
 *
 * class Order {
 *     private String number;
 *     private List<Product> products;
 *
 *     @FaunaConstructor
 *     public static Order createOrder(@FaunaField("number") String number, @FaunaField("products") List<Product> products) {
 *         Order order = new Order;
 *         order.number = number;
 *         order.products = products;
 *         return order;
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface FaunaConstructor {
}
