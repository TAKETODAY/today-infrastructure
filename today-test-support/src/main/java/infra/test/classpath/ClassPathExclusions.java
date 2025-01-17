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

package infra.test.classpath;

import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;

/**
 * Annotation used to exclude entries from the classpath.
 *
 * @author Andy Wilkinson
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ExtendWith(ModifiedClassPathExtension.class)
public @interface ClassPathExclusions {

  /**
   * Alias for {@code files}.
   * <p>
   * One or more Ant-style patterns that identify entries to be excluded from the class
   * path. Matching is performed against an entry's {@link File#getName() file name}.
   * For example, to exclude Hibernate Validator from the classpath,
   * {@code "hibernate-validator-*.jar"} can be used.
   *
   * @return the exclusion patterns
   */
  @AliasFor("files")
  String[] value() default {};

  /**
   * One or more Ant-style patterns that identify entries to be excluded from the class
   * path. Matching is performed against an entry's {@link File#getName() file name}.
   * For example, to exclude Hibernate Validator from the classpath,
   * {@code "hibernate-validator-*.jar"} can be used.
   *
   * @return the exclusion patterns
   */
  @AliasFor("value")
  String[] files() default {};

  /**
   * One or more packages that should be excluded from the classpath.
   *
   * @return the excluded packages
   */
  String[] packages() default {};

}
