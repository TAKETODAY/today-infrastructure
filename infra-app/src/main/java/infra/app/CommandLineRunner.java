/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app;

import infra.core.Ordered;
import infra.core.annotation.Order;

/**
 * Interface used to indicate that a bean should <em>run</em> when it is contained within
 * a {@link Application}. Multiple {@link CommandLineRunner} beans can be defined
 * within the same application context and can be ordered using the {@link Ordered}
 * interface or {@link Order @Order} annotation.
 * <p>
 * If you need access to {@link ApplicationArguments} instead of the raw String array
 * consider using {@link ApplicationRunner#run(ApplicationArguments)}.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationRunner
 * @since 4.0 2022/1/16 20:54
 */
@FunctionalInterface
public interface CommandLineRunner extends ApplicationRunner {

  /**
   * Executes the command-line runner logic with the provided application arguments.
   * This method serves as a bridge between {@link ApplicationArguments} and raw string arguments,
   * delegating the execution to the {@link #run(String...)} method by extracting the raw arguments
   * from the {@link ApplicationArguments} instance.
   *
   * <p>This default implementation is particularly useful when implementing the {@link CommandLineRunner}
   * interface, allowing developers to focus on processing raw arguments in the {@link #run(String...)}
   * method while maintaining compatibility with {@link ApplicationArguments}.
   *
   * <p><strong>Usage Example:</strong>
   * <pre>{@code
   * @Component
   * public class MyRunner implements CommandLineRunner {
   *   @Override
   *   public void run(String... args) throws Exception {
   *     System.out.println("Application started with arguments:");
   *     for (String arg : args) {
   *       System.out.println(arg);
   *     }
   *   }
   * }
   * }</pre>
   *
   * <p>In this example, the {@code MyRunner} class implements the {@link CommandLineRunner} interface.
   * The {@code run} method processes the raw arguments passed to the application. When invoked through
   * the Infra application context, the default implementation of this method ensures that the raw
   * arguments are correctly extracted and passed to the custom logic defined in the {@code run(String...)} method.
   *
   * @param args an {@link ApplicationArguments} object encapsulating the arguments passed to the application
   * @throws Exception if an error occurs during the execution of the runner logic
   * @see ApplicationArguments#getSourceArgs()
   * @see #run(String...)
   */
  @Override
  default void run(ApplicationArguments args) throws Exception {
    run(args.getSourceArgs());
  }

  /**
   * Executes the command-line runner logic with the provided raw arguments.
   * This method is invoked when the application starts, allowing custom logic
   * to be executed based on the arguments passed to the application.
   *
   * <p>This method is typically implemented by beans that need to perform
   * initialization tasks or process command-line arguments during application startup.
   *
   * <p><strong>Usage Example:</strong>
   * <pre>{@code
   * @Component
   * public class MyRunner implements CommandLineRunner {
   *   @Override
   *   public void run(String... args) throws Exception {
   *     System.out.println("Application started with arguments:");
   *     for (String arg : args) {
   *       System.out.println(arg);
   *     }
   *   }
   * }
   * }</pre>
   *
   * <p>In this example, the {@code MyRunner} class implements the {@link CommandLineRunner}
   * interface. The {@code run} method processes the raw arguments passed to the application.
   * When invoked through the Infra application context, the method ensures that the arguments
   * are correctly handled and processed.
   *
   * @param args an array of raw arguments passed to the application
   * @throws Exception if an error occurs during the execution of the runner logic
   * @see ApplicationArguments#getSourceArgs()
   * @see CommandLineRunner
   */
  void run(String... args) throws Exception;

}

