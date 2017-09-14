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
 * <pre><code>
 * class Product {
 *     private String description;
 *     private double price;
 *
 *     &#64;FaunaConstructor
 *     public Product(&#64;FaunaField("description") String description, &#64;FaunaField("price") double price) {
 *         this.description = description;
 *         this.price = price;
 *     }
 * }
 *
 * class Order {
 *     private String number;
 *     private List&lt;Product&gt; products;
 *
 *     &#64;FaunaConstructor
 *     public static Order createOrder(&#64;FaunaField("number") String number, &#64;FaunaField("products") List&lt;Product&gt; products) {
 *         Order order = new Order;
 *         order.number = number;
 *         order.products = products;
 *         return order;
 *     }
 * }
 * </code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface FaunaConstructor {
}
