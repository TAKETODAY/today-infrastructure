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

package infra.jdbc.type;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation that specify java types to map {@link TypeHandler}.
 *
 * <p>
 * <b>How to use:</b>
 * <pre>{@code
 * @MappedTypes(String.class)
 * public class StringTrimmingTypeHandler implements TypeHandler<String> {
 *   // ...
 * }
 * }</pre>
 *
 * @author Eduardo Macarron
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MappedTypes {

  /**
   * Returns java types to map {@link TypeHandler}.
   *
   * @return java types
   */
  Class<?>[] value();

}
