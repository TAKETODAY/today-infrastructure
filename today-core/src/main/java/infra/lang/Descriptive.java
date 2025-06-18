/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.lang;

/**
 * An interface representing an entity that can provide a description.
 * Implementations of this interface are expected to return a meaningful
 * textual representation via the {@link #getDescription()} method.
 *
 * <p>Example usage:
 * <pre>{@code
 * public enum Color implements Descriptive {
 *   RED,
 *   GREEN,
 *   BLUE;
 *
 *   @Override
 *   public String getDescription() {
 *     return "Color: " + name().toLowerCase();
 *   }
 * }
 *
 * public class Example {
 *   public static void main(String[] args) {
 *     Color color = Color.RED;
 *     System.out.println(color.getDescription());
 *     // Output: Color: red
 *   }
 * }
 * }</pre>
 *
 * <p>This interface can also be extended to provide additional functionality,
 * such as debugging support or value-based enumeration handling.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/1 17:18
 */
public interface Descriptive {

  /**
   * Returns a textual description of the implementing entity.
   *
   * <p>This method is intended to provide a meaningful representation
   * of the object, which can be used for display, logging, or debugging
   * purposes. The format and content of the description are determined
   * by the implementation.
   *
   * <p>Example usage:
   * <pre>{@code
   * public class Product implements Descriptive {
   *   private String name;
   *   private double price;
   *
   *   public Product(String name, double price) {
   *     this.name = name;
   *     this.price = price;
   *   }
   *
   *   @Override
   *   public String getDescription() {
   *     return "Product{name='" + name + "', price=" + price + "}";
   *   }
   * }
   *
   * public class Example {
   *   public static void main(String[] args) {
   *     Product product = new Product("Laptop", 999.99);
   *     System.out.println(product.getDescription());
   *     // Output: Product{name='Laptop', price=999.99}
   *   }
   * }
   * }</pre>
   *
   * @return a {@code String} containing the description of the object
   */
  String getDescription();

}
