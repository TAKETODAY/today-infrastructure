/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package infra.session;

import java.io.Serial;

import infra.context.ApplicationEvent;

/**
 * This is the class representing event notifications for
 * changes to sessions within a web application.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/9 09:53
 */
public class WebSessionEvent extends ApplicationEvent {

  @Serial
  private static final long serialVersionUID = 1L;

  final WebSession session;

  public WebSessionEvent(Object source, WebSession session) {
    super(source);
    this.session = session;
  }

  /**
   * Return the session that changed.
   *
   * @return the {@link WebSession} for this event.
   */
  public WebSession getSession() {
    return session;
  }

  /**
   * returns session's id
   */
  public String getSessionId() {
    return session.getId();
  }

}
