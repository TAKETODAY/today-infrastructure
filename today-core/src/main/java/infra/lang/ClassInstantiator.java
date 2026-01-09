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
