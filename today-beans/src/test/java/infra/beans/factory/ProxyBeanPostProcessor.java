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
package infra.beans.factory;

import infra.beans.BeansException;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * @author Today <br>
 *
 * 2018-09-09 20:14
 */
//@Singleton
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProxyBeanPostProcessor implements InitializationBeanPostProcessor {
  private static final Logger log = LoggerFactory.getLogger(ProxyBeanPostProcessor.class);

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) {
    log.debug("ProxyBeanPostProcessor Before named :[{}]", beanName);
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    log.debug("ProxyBeanPostProcessor After :[{}]", beanName);
    return bean;
  }

}
