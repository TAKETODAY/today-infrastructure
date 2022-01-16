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
package cn.taketoday.framework;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.event.ApplicationEvent;

/**
 * @author TODAY <br>
 * 2019-05-17 10:14
 */
@SuppressWarnings("serial")
public class ApplicationFailedEvent extends ApplicationEvent {

  private final ApplicationContext applicationContext;

  private final Throwable cause;

  public ApplicationFailedEvent(ApplicationContext applicationContext, Throwable cause) {
    super(applicationContext);
    this.cause = cause;
    this.applicationContext = applicationContext;
  }

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  public final Throwable getCause() {
    return cause;
  }

}
