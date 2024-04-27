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

package cn.taketoday.web.mock.descriptor;

/**
 * This interface provides access to the <code>&lt;taglib&gt;</code> related configuration of a web application.
 *
 * <p>
 * The configuration is aggregated from the <code>web.xml</code> and <code>web-fragment.xml</code> descriptor files of
 * the web application.
 *
 * @since Servlet 3.0
 */
public interface TaglibDescriptor {

  /**
   * Gets the unique identifier of the tag library represented by this TaglibDescriptor.
   *
   * @return the unique identifier of the tag library represented by this TaglibDescriptor
   */
  public String getTaglibURI();

  /**
   * Gets the location of the tag library represented by this TaglibDescriptor.
   *
   * @return the location of the tag library represented by this TaglibDescriptor
   */
  public String getTaglibLocation();
}
