/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.client.config;

/**
 * Redirect strategies support by HTTP clients.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public enum HttpRedirects {

  /**
   * Follow redirects (if the underlying library has support).
   */
  FOLLOW_WHEN_POSSIBLE,

  /**
   * Follow redirects (fail if the underlying library has no support).
   */
  FOLLOW,

  /**
   * Don't follow redirects (fail if the underlying library has no support).
   */
  DONT_FOLLOW

}
