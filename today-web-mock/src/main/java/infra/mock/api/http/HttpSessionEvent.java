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

package infra.mock.api.http;

/**
 * This is the class representing event notifications for changes to sessions within a web application.
 */
public class HttpSessionEvent extends java.util.EventObject {

  private static final long serialVersionUID = -7622791603672342895L;

  /**
   * Construct a session event from the given source.
   *
   * @param source the {@link HttpSession} corresponding to this event
   */
  public HttpSessionEvent(HttpSession source) {
    super(source);
  }

  /**
   * Return the session that changed.
   *
   * @return the {@link HttpSession} for this event.
   */
  public HttpSession getSession() {
    return (HttpSession) super.getSource();
  }
}
