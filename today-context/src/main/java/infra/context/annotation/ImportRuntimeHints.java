/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.RuntimeHintsRegistrar;

/**
 * Indicates that one or more {@link RuntimeHintsRegistrar} implementations
 * should be processed.
 *
 * <p>Unlike declaring {@link RuntimeHintsRegistrar} using
 * {@code META-INF/config/aot.factories}, this annotation allows for more flexible
 * registration where it is only processed if the annotated component or bean
 * method is actually registered in the bean factory. To illustrate this
 * behavior, consider the following example:
 *
 * <pre>{@code
 * @Configuration
 * public class MyConfiguration {
 *
 *     @Bean
 *     @ImportRuntimeHints(MyHints.class)
 *     @Conditional(MyCondition.class)
 *     public MyService myService() {
 *         return new MyService();
 *     }
 *
 * }}</pre>
 *
 * <p>If the configuration class above is processed, {@code MyHints} will be
 * contributed only if {@code MyCondition} matches. If the condition does not
 * match, {@code MyService} will not be defined as a bean and the hints will
 * not be processed either.
 *
 * <p>{@code @ImportRuntimeHints} can also be applied to any test class that uses
 * the <em>Infra TestContext Framework</em> to load an {@code ApplicationContext}.
 *
 * <p>If several components or test classes refer to the same {@link RuntimeHintsRegistrar}
 * implementation, the registrar will only be invoked once for the given bean factory
 * processing or test suite.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.aot.hint.RuntimeHints
 * @see ReflectiveScan @ReflectiveScan
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ImportRuntimeHints {

  /**
   * {@link RuntimeHintsRegistrar} implementations to process.
   */
  Class<? extends RuntimeHintsRegistrar>[] value();

}
