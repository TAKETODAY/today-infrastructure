/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aot.hint.RuntimeHintsRegistrar;

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
 * <pre class="code">
 * &#064;Configuration
 * public class MyConfiguration {
 *
 *     &#064;Bean
 *     &#064;ImportRuntimeHints(MyHints.class)
 *     &#064;Conditional(MyCondition.class)
 *     public MyService myService() {
 *         return new MyService();
 *     }
 *
 * }</pre>
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
 * @see cn.taketoday.aot.hint.RuntimeHints
 * @see cn.taketoday.aot.hint.annotation.Reflective
 * @see cn.taketoday.aot.hint.annotation.RegisterReflectionForBinding
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
