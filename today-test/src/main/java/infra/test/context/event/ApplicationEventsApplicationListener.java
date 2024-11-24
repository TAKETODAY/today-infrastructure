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

package infra.test.context.event;

import infra.context.ApplicationEvent;
import infra.context.ApplicationListener;

/**
 * {@link ApplicationListener} that listens to all events and adds them to the
 * current {@link ApplicationEvents} instance if registered for the current thread.
 *
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @since 4.0
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
