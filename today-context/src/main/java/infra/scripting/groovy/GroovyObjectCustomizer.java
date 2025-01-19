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

package infra.scripting.groovy;

import groovy.lang.GroovyObject;

/**
 * Strategy used by {@link GroovyScriptFactory} to allow the customization of
 * a created {@link GroovyObject}.
 *
 * <p>This is useful to allow the authoring of DSLs, the replacement of missing
 * methods, and so forth. For example, a custom {@link groovy.lang.MetaClass}
 * could be specified.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see GroovyScriptFactory
 * @since 4.0
 */
@FunctionalInterface
public interface GroovyObjectCustomizer {

  /**
   * Customize the supplied {@link GroovyObject}.
   * <p>For example, this can be used to set a custom metaclass to
   * handle missing methods.
   *
   * @param goo the {@code GroovyObject} to customize
   */
  void customize(GroovyObject goo);

}
