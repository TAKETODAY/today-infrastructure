/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
