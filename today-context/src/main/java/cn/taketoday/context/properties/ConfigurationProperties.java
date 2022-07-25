/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.stereotype.Indexed;

/**
 * Annotation for externalized configuration. Add this to a class definition or a
 * {@code @Bean} method in a {@code @Configuration} class if you want to bind and validate
 * some external Properties (e.g. from a .properties file).
 * <p>
 * Binding is either performed by calling setters on the annotated class or, if
 * {@link ConstructorBinding @ConstructorBinding} is in use, by binding to the constructor
 * parameters.
 * <p>
 * Note that contrary to {@code @Value}, EL expressions are not evaluated since property
 * values are externalized.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurationPropertiesScan
 * @see ConstructorBinding
 * @see ConfigurationPropertiesBindingPostProcessor
 * @see EnableConfigurationProperties
 * @since 4.0
 */
@Indexed
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConfigurationProperties {

  /**
   * The prefix of the properties that are valid to bind to this object. Synonym for
   * {@link #prefix()}. A valid prefix is defined by one or more words separated with
   * dots (e.g. {@code "acme.system.feature"}).
   *
   * @return the prefix of the properties to bind
   */
  @AliasFor("prefix")
  String value() default "";

  /**
   * The prefix of the properties that are valid to bind to this object. Synonym for
   * {@link #value()}. A valid prefix is defined by one or more words separated with
   * dots (e.g. {@code "acme.system.feature"}).
   *
   * @return the prefix of the properties to bind
   */
  @AliasFor("value")
  String prefix() default "";

  /**
   * Flag to indicate that when binding to this object invalid fields should be ignored.
   * Invalid means invalid according to the binder that is used, and usually this means
   * fields of the wrong type (or that cannot be coerced into the correct type).
   *
   * @return the flag value (default false)
   */
  boolean ignoreInvalidFields() default false;

  /**
   * Flag to indicate that when binding to this object unknown fields should be ignored.
   * An unknown field could be a sign of a mistake in the Properties.
   *
   * @return the flag value (default true)
   */
  boolean ignoreUnknownFields() default true;

}
