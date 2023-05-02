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

package cn.taketoday.framework.context.event;

import java.io.Serial;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationArguments;

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
