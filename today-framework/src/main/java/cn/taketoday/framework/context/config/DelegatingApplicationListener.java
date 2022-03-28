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

package cn.taketoday.framework.context.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.event.SimpleApplicationEventMulticaster;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link ApplicationListener} that delegates to other listeners that are specified under
 * a {@literal context.listener.classes} environment property.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 4.0
 */
public class DelegatingApplicationListener implements ApplicationListener<ApplicationEvent>, Ordered {

  // NOTE: Similar to cn.taketoday.web.context.ContextLoader

  private static final String PROPERTY_NAME = "context.listener.classes";

  private int order = 0;

  private SimpleApplicationEventMulticaster multicaster;

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof ApplicationEnvironmentPreparedEvent) {
      List<ApplicationListener<ApplicationEvent>> delegates = getListeners(
              ((ApplicationEnvironmentPreparedEvent) event).getEnvironment());
      if (delegates.isEmpty()) {
        return;
      }
      this.multicaster = new SimpleApplicationEventMulticaster();
      for (ApplicationListener<ApplicationEvent> listener : delegates) {
        this.multicaster.addApplicationListener(listener);
      }
    }
    if (this.multicaster != null) {
      this.multicaster.multicastEvent(event);
    }
  }

  @SuppressWarnings("unchecked")
  private List<ApplicationListener<ApplicationEvent>> getListeners(ConfigurableEnvironment environment) {
    if (environment == null) {
      return Collections.emptyList();
    }
    String classNames = environment.getProperty(PROPERTY_NAME);
    List<ApplicationListener<ApplicationEvent>> listeners = new ArrayList<>();
    if (StringUtils.isNotEmpty(classNames)) {
      for (String className : StringUtils.commaDelimitedListToSet(classNames)) {
        try {
          Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
          Assert.isAssignable(ApplicationListener.class, clazz,
                  () -> "class [" + className + "] must implement ApplicationListener");
          listeners.add((ApplicationListener<ApplicationEvent>) BeanUtils.newInstance(clazz));
        }
        catch (Exception ex) {
          throw new ApplicationContextException("Failed to load context listener class [" + className + "]",
                  ex);
        }
      }
    }
    AnnotationAwareOrderComparator.sort(listeners);
    return listeners;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

}
