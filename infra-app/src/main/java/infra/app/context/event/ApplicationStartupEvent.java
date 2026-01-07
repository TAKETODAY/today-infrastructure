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

package infra.app.context.event;

import java.io.Serial;

import infra.app.Application;
import infra.app.ApplicationArguments;
import infra.context.ApplicationEvent;

/**
 * Base class for {@link ApplicationEvent} related to a {@link Application}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ApplicationStartupEvent extends ApplicationEvent {
  @Serial
  private static final long serialVersionUID = 1L;

  private final ApplicationArguments arguments;

  public ApplicationStartupEvent(Application application, ApplicationArguments arguments) {
    super(application);
    this.arguments = arguments;
  }

  public Application getApplication() {
    return (Application) getSource();
  }

  public ApplicationArguments getArguments() {
    return arguments;
  }

  public final String[] getSourceArgs() {
    return arguments.getSourceArgs();
  }

}
