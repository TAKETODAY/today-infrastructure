/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.session;

/**
 * For {@link SessionRepository} implementations that support it, this event is fired when
 * a {@link WebSession} is destroyed via deletion.
 *
 * @author Mark Anderson
 * @author Rob Winch
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/10 16:33
 */
@SuppressWarnings("serial")
public class SessionDeletedEvent extends SessionDestroyedEvent {

  /**
   * Create a new {@link SessionDeletedEvent}.
   *
   * @param source the source of the event
   * @param session the session that was created
   */
  public SessionDeletedEvent(Object source, WebSession session) {
    super(source, session);
  }

}
