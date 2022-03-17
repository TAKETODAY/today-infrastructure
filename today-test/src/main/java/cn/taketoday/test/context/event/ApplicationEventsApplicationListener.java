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

package cn.taketoday.test.context.event;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;

/**
 * {@link ApplicationListener} that listens to all events and adds them to the
 * current {@link ApplicationEvents} instance if registered for the current thread.
 *
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @since 5.3.3
 */
class ApplicationEventsApplicationListener implements ApplicationListener<ApplicationEvent> {

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    DefaultApplicationEvents applicationEvents =
            (DefaultApplicationEvents) ApplicationEventsHolder.getApplicationEvents();
    if (applicationEvents != null) {
      applicationEvents.addEvent(event);
    }
  }

}
