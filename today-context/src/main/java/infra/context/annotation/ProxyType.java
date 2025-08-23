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

package infra.context.annotation;

/**
 * Common enum for indicating a desired proxy type.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Proxyable#value()
 * @since 5.0
 */
public enum ProxyType {

  /**
   * Default is a JDK dynamic proxy, or potentially a class-based CGLIB proxy
   * when globally configured.
   */
  DEFAULT,

  /**
   * Suggest a JDK dynamic proxy implementing <i>all</i> interfaces exposed by
   * the class of the target object. Overrides a globally configured default.
   */
  INTERFACES,

  /**
   * Suggest a class-based CGLIB proxy. Overrides a globally configured default.
   */
  TARGET_CLASS

}
