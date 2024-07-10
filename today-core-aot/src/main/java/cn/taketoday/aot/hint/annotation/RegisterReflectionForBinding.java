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

package cn.taketoday.aot.hint.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;

/**
 * Register reflection hints for data binding or reflection-based serialization
 * against an arbitrary number of target classes.
 *
 * <p>For each class hints are registered for constructors, fields, properties,
 * and record components. Hints are also registered for types transitively used
 * on properties and record components.
 *
 * <p>The annotated element can be a configuration class &mdash; for example:
 * <pre>{@code
 * @Configuration
 * @RegisterReflectionForBinding({Foo.class, Bar.class})
 * public class MyConfig {
 *     // ...
 * }
 * }</pre>
 *
 * <p>When the annotated element is a type, the type itself is registered if no
 * candidates are provided:<pre>{@code
 * @Component
 * @RegisterReflectionForBinding
 * public class MyBean {
 *     // ...
 * }}</pre>
 *
 * The annotation can also be specified on a method. In that case, at least one
 * target class must be specified:<pre>{@code
 * @Component
 * public class MyService {
 *
 *     @RegisterReflectionForBinding(Baz.class)
 *     public Baz process() {
 *         // ...
 *     }
 *
 * }
 * }</pre>
 *
 * <p>The annotated element can also be any test class that uses the <em>Spring
 * TestContext Framework</em> to load an {@code ApplicationContext}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.aot.hint.BindingReflectionHintsRegistrar
 * @see RegisterReflection @RegisterReflection
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RegisterReflection
@Reflective(RegisterReflectionForBindingProcessor.class)
public @interface RegisterReflectionForBinding {

  /**
   * Alias for {@link #classes()}.
   */
  @AliasFor(annotation = RegisterReflection.class, attribute = "classes")
  Class<?>[] value() default {};

  /**
   * Classes for which reflection hints should be registered.
   * <p>At least one class must be specified either via {@link #value} or {@code classes}.
   *
   * @see #value()
   */
  @AliasFor(annotation = RegisterReflection.class, attribute = "classes")
  Class<?>[] classes() default {};

  /**
   * Alternative to {@link #classes()} to specify the classes as class names.
   *
   * @see #classes()
   */
  @AliasFor(annotation = RegisterReflection.class, attribute = "classNames")
  String[] classNames() default {};

}
