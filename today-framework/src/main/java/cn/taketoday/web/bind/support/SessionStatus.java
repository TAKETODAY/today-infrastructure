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

package cn.taketoday.web.bind.support;

import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.SessionAttributes;

/**
 * Simple interface that can be injected into handler methods, allowing them to
 * signal that their session processing is complete. The handler invoker may
 * then follow up with appropriate cleanup, e.g. of session attributes which
 * have been implicitly created during this handler's processing (according to
 * the {@link SessionAttributes @SessionAttributes} annotation).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestMapping
 * @see SessionAttributes
 * @since 4.0 2022/4/8 23:24
 */
public interface SessionStatus {

  /**
   * Mark the current handler's session processing as complete, allowing for
   * cleanup of session attributes.
   */
  void setComplete();

  /**
   * Return whether the current handler's session processing has been marked
   * as complete.
   */
  boolean isComplete();

}
