/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.annotation;

/**
 * Enumerates the various scoped-proxy options.
 *
 * <p>For a more complete discussion of exactly what a scoped proxy is, see the
 * section of the Framework reference documentation entitled '<em>Scoped beans as
 * dependencies</em>'.
 *
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ScopeMetadata
 * @since 4.0 2022/3/7 21:31
 */
public enum ScopedProxyMode {

  /**
   * Default typically equals {@link #NO}, unless a different default
   * has been configured at the component-scan instruction level.
   */
  DEFAULT,

  /**
   * Do not create a scoped proxy.
   * <p>This proxy-mode is not typically useful when used with a
   * non-singleton scoped instance, which should favor the use of the
   * {@link #INTERFACES} or {@link #TARGET_CLASS} proxy-modes instead if it
   * is to be used as a dependency.
   */
  NO,

  /**
   * Create a JDK dynamic proxy implementing <i>all</i> interfaces exposed by
   * the class of the target object.
   */
  INTERFACES,

  /**
   * Create a class-based proxy (uses CGLIB).
   */
  TARGET_CLASS

}
