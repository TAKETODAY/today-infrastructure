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

package infra.context.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Conditional;
import infra.core.env.Environment;

/**
 * {@link Conditional @Conditional} that checks if the specified properties have a
 * specific boolean value. By default, the properties must be present in the
 * {@link infra.core.env.Environment} and equal to {@code true}. The {@link #havingValue()} and
 * {@link #matchIfMissing()} attributes allow further customizations.
 * <p>
 * If the property is not contained in the {@link Environment} at all, the
 * {@link #matchIfMissing()} attribute is consulted. By default, missing attributes do not
 * match.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ConditionalOnProperty
 * @since 5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnPropertyCondition.class)
@Repeatable(ConditionalOnBooleanProperties.class)
public @interface ConditionalOnBooleanProperty {

  /**
   * Alias for {@link #name()}.
   *
   * @return the names
   */
  String[] value() default {};

  /**
   * A prefix that should be applied to each property. The prefix automatically ends
   * with a dot if not specified. A valid prefix is defined by one or more words
   * separated with dots (e.g. {@code "acme.system.feature"}).
   *
   * @return the prefix
   */
  String prefix() default "";

  /**
   * The name of the properties to test. If a prefix has been defined, it is applied to
   * compute the full key of each property. For instance if the prefix is
   * {@code app.config} and one value is {@code my-value}, the full key would be
   * {@code app.config.my-value}
   * <p>
   * Use the dashed notation to specify each property, that is all lower case with a "-"
   * to separate words (e.g. {@code my-long-property}).
   * <p>
   * If multiple names are specified, all the properties have to pass the test for
   * the condition to match.
   *
   * @return the names
   */
  String[] name() default {};

  /**
   * The expected value for the properties. If not specified, the property must be equal
   * to {@code true}.
   *
   * @return the expected value
   */
  boolean havingValue() default true;

  /**
   * Specify if the condition should match if the property is not set. Defaults to
   * {@code false}.
   *
   * @return if the condition should match if the property is missing
   */
  boolean matchIfMissing() default false;

}
