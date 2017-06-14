package com.faunadb.client.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Instruct the decoder which constructor to use when decoding a object.
 * It can also be used in a public static method instead in a constructor.</p>
 *
 * <p>That attribute can only be used once per class.</p>
 *
 * <pre>{@code
 * class Product {
 *     private string description;
 *     private double price;
 *
 *     @FaunaConstructor
 *     public Product(@FaunaField("description") string description, @FaunaField("price") double price) {
 *         this.description = description;
 *         this.price = price;
 *     }
 * }
 *
 * class Order {
 *     private string number;
 *     private List<Product> products;
 *
 *     @FaunaConstructor
 *     public static Order createOrder(@FaunaField("number") string number, @FaunaField("products") List<Product> products) {
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
