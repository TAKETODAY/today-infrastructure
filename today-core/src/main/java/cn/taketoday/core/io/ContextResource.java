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

package cn.taketoday.core.io;

/**
 * Extended interface for a resource that is loaded from an enclosing
 * 'context', e.g. from a plain classpath paths or relative file system
 * paths (specified without an explicit prefix, hence applying relative
 * to the local {@link ResourceLoader}'s context).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 16:38
 */
public interface ContextResource extends Resource {

  /**
   * Return the path within the enclosing 'context'.
   * <p>This is typically path relative to a context-specific root directory,
   * e.g. a ServletContext root or a PortletContext root.
   */
  String getPathWithinContext();

}

