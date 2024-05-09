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
 * Indicates that the classes specified in the annotation attributes require some
 * reflection hints for binding or reflection-based serialization purposes. For each
 * class specified, hints on constructors, fields, properties, record components,
 * including types transitively used on properties and record components are registered.
 * At least one class must be specified in the {@code value} or {@code classes} annotation
 * attributes.
 *
 * <p>The annotated element can be a configuration class &mdash; for example:
 *
 * <pre>{@code
 *  @Configuration
 *  @RegisterReflectionForBinding({Foo.class,Bar.class})
 *  public class MyConfig {
 *     // ...
 *  }
 * }
 * </pre>
 *
 * <p>The annotated element can be any Infra bean class, constructor, field,
 * or method &mdash; for example:
 *
 * <pre>{@code
 * @Service
 * public class MyService {
 *
 *     @RegisterReflectionForBinding(Baz.class)
 *     public void process() {
 *         // ...
 *     }
 *
 * }
 * }</pre>
 *
 * <p>The annotated element can also be any test class that uses the <em>Infra
 * TestContext Framework</em> to load an {@code ApplicationContext}.
 *
 * @author Sebastien Deleuze
 * @see cn.taketoday.aot.hint.BindingReflectionHintsRegistrar
 * @see Reflective @Reflective
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Reflective(RegisterReflectionForBindingProcessor.class)
public @interface RegisterReflectionForBinding {

  /**
   * Alias for {@link #classes()}.
   */
  @AliasFor("classes")
  Class<?>[] value() default {};

  /**
   * Classes for which reflection hints should be registered.
   * <p>At least one class must be specified either via {@link #value} or
   * {@link #classes}.
   *
   * @see #value()
   */
  @AliasFor("value")
  Class<?>[] classes() default {};

}
