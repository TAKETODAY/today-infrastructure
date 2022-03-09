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

package cn.taketoday.context.testfixture.beans;

import java.util.Map;

import cn.taketoday.context.event.ApplicationEvent;
import cn.taketoday.context.event.ApplicationListener;

/**
 * A stub {@link ApplicationListener}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class BeanThatListens implements ApplicationListener<ApplicationEvent> {

  private BeanThatBroadcasts beanThatBroadcasts;

  private int eventCount;

  public BeanThatListens() {
  }

  public BeanThatListens(BeanThatBroadcasts beanThatBroadcasts) {
    this.beanThatBroadcasts = beanThatBroadcasts;
    Map<?, BeanThatListens> beans = beanThatBroadcasts.applicationContext.getBeansOfType(BeanThatListens.class);
    if (!beans.isEmpty()) {
      throw new IllegalStateException("Shouldn't have found any BeanThatListens instances");
    }
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    eventCount++;
    if (beanThatBroadcasts != null) {
      beanThatBroadcasts.receivedCount++;
    }
  }

  public int getEventCount() {
    return eventCount;
  }

  public void zero() {
    eventCount = 0;
  }

}
