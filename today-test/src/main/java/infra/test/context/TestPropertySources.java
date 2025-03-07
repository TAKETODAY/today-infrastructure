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

package infra.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @TestPropertySources} is a container for one or more
 * {@link TestPropertySource @TestPropertySource} declarations.
 *
 * <p>Note, however, that use of the {@code @TestPropertySources} container is
 * completely optional since {@code @TestPropertySource} is a
 * {@linkplain java.lang.annotation.Repeatable repeatable} annotation.
 *
 * <p> this annotation will be inherited from an
 * enclosing test class by default. See
 * {@link NestedTestConfiguration @NestedTestConfiguration} for details.
 *
 * @author Anatoliy Korovin
 * @author Sam Brannen
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TestPropertySources {

  /**
   * An array of one or more {@link TestPropertySource @TestPropertySource}
   * declarations.
   */
  TestPropertySource[] value();

}
