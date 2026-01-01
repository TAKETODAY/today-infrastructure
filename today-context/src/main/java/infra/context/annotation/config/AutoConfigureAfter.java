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

package infra.context.annotation.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Configuration;
import infra.context.annotation.DependsOn;

/**
 * Hint for that an {@link EnableAutoConfiguration auto-configuration} should be applied
 * after other specified auto-configuration classes.
 * <p>
 * As with standard {@link Configuration @Configuration} classes, the order in which
 * auto-configuration classes are applied only affects the order in which their beans are
 * defined. The order in which those beans are subsequently created is unaffected and is
 * determined by each bean's dependencies and any {@link DependsOn @DependsOn}
 * relationships.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 11:55
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoConfigureAfter {

  /**
   * The auto-configuration classes that should have already been applied.
   * <p>
   * Since this annotation is parsed by loading class bytecode, it is safe to specify
   * classes here that may ultimately not be on the classpath, but only if this
   * annotation is directly on the affected component and <b>not</b> if this annotation
   * is used as a composed, meta-annotation. In order to use this annotation as a
   * meta-annotation, only use the {@link #name} attribute.
   *
   * @return the classes
   */
  Class<?>[] value() default {};

  /**
   * The names of the auto-configuration classes that should have already been applied.
   * In the unusual case that an auto-configuration class is not a top-level class, its
   * class name should use {@code $} to separate it from its containing class, for
   * example {@code com.example.Outer$NestedAutoConfiguration}.
   *
   * @return the class names
   */
  String[] name() default {};

}

