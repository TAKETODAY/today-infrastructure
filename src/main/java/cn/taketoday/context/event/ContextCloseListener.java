/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.context.event;

import java.text.SimpleDateFormat;
import java.util.Map;

import cn.taketoday.beans.factory.AbstractBeanFactory;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.Constant;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ClassUtils;

import static cn.taketoday.context.ContextUtils.destroyBean;
import static cn.taketoday.util.ExceptionUtils.unwrapThrowable;

/**
 * @author TODAY 2018-09-09 23:20
 * @see ContextCloseEvent
 */
public class ContextCloseListener
        extends OrderedSupport implements ApplicationListener<ContextCloseEvent> {

  public ContextCloseListener() {
    this(Ordered.LOWEST_PRECEDENCE);
  }

  public ContextCloseListener(int order) {
    super(order);
  }

  @Override
  public void onApplicationEvent(ContextCloseEvent event) {
    final ApplicationContext context = event.getSource();
    final Logger log = LoggerFactory.getLogger(getClass());
    log.info("Closing: [{}] at [{}]", context,
             new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(event.getTimestamp()));

    ConfigurableBeanFactory beanFactory = event.getBeanFactory(ConfigurableBeanFactory.class);

    for (final String name : beanFactory.getBeanDefinitions().keySet()) {
      try {
        beanFactory.destroyBean(name);
        // remove bean in this context
        beanFactory.removeBean(name);
      }
      catch (final Throwable e) {
        log.error(e.getMessage(), e);
      }
    }
    final Map<String, Object> singletons = beanFactory.getSingletons();
    for (final Map.Entry<String, Object> entry : singletons.entrySet()) {
      try {
        destroyBean(entry.getValue());
      }
      catch (Throwable e) {
        e = unwrapThrowable(e);
        log.error(e.getMessage(), e);
      }
    }
    // remove bean in this context
    singletons.clear();

    if (beanFactory instanceof AbstractBeanFactory) {
      ((AbstractBeanFactory) beanFactory).getDependencies().clear();
      ((AbstractBeanFactory) beanFactory).getPostProcessors().clear();
    }
    ClassUtils.clearCache();
  }

}
