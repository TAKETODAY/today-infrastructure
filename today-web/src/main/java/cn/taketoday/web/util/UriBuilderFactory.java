/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.util;

/**
 * Factory to create {@link UriBuilder} instances with shared configuration
 * such as a base URI, an encoding mode strategy, and others across all URI
 * builder instances created through a factory.
 *
 * @author Rossen Stoyanchev
 * @see DefaultUriBuilderFactory
 * @since 4.0
 */
public interface UriBuilderFactory extends UriTemplateHandler {

  /**
   * Initialize a builder with the given URI template.
   *
   * @param uriTemplate the URI template to use
   * @return the builder instance
   */
  UriBuilder uriString(String uriTemplate);

  /**
   * Create a URI builder with default settings.
   *
   * @return the builder instance
   */
  UriBuilder builder();

}
