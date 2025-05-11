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
 * An interface responsible for dynamically instantiating classes based on a given implementation
 * type. This interface provides a flexible mechanism to create instances of classes at runtime,
 * typically used in scenarios where implementations are determined dynamically, such as through
 * configuration or dependency injection frameworks.
 *
 * <p>The {@link #instantiate(Class)} method is the core functionality of this interface. It allows
 * the creation of an instance of a specified class using a strategy defined by the implementing
 * class. The strategy may involve resolving constructor arguments, handling access restrictions,
 * or other custom logic.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Define an interface and its implementation
 * public interface MyInterface {
 *   void someMethod();
 * }
 *
 * public class MyImplementation implements MyInterface {
 *   public void someMethod() {
 *     System.out.println("Method executed");
 *   }
 * }
 *
 * // Use ClassInstantiator to create an instance
 * ClassInstantiator instantiator = new DefaultInstantiator(null);
 * MyInterface instance = instantiator.instantiate(MyImplementation.class);
 * instance.someMethod(); // Output: Method executed
 * }</pre>
 *
 * <p>In the example above, a custom implementation of {@code ClassInstantiator} is used to
 * instantiate a class dynamically. The {@code DefaultInstantiator} is an internal implementation
 * that resolves constructor arguments if necessary.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/5 15:43
 */
public interface ClassInstantiator {

  /**
   * Instantiates a given implementation class using the strategy defined by this instantiator.
   *
   * <p>This method allows dynamic instantiation of a class that implements the specified type.
   * It is typically used in scenarios where implementations need to be created at runtime
   * based on configuration or other dynamic factors.</p>
   *
   * <p>Example usage:</p>
   * <pre>{@code
   * ClassInstantiator instantiator = new SomeClassInstantiator();
   * MyInterface instance = instantiator.instantiate(MyImplementation.class);
   * instance.someMethod();
   * }</pre>
   *
   * @param <T> the type of the implementation to instantiate
   * @param implementation the class object representing the implementation to instantiate;
   * must not be null and should have a visible no-argument constructor
   * @return an instance of the specified implementation class
   * @throws Exception if instantiation fails, such as due to missing constructors,
   * inaccessible constructors, or other reflective errors
   */
  <T> T instantiate(Class<T> implementation) throws Exception;

}
