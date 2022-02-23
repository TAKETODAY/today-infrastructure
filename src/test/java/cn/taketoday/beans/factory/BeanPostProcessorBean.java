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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.beans.factory;

import cn.taketoday.beans.BeansException;
import cn.taketoday.core.Order;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author Today <br>
 *
 * 2018-08-08 18:20
 */
@Order(1)
//@Singleton
public class BeanPostProcessorBean implements InitializationBeanPostProcessor {
  private static final Logger log = LoggerFactory.getLogger(BeanPostProcessorBean.class);

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanDefinition) {
    log.debug("BeanPostProcessorBean Before named :[{}]", beanDefinition);
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    log.debug("BeanPostProcessorBean After named :[{}]", beanName);
    return bean;
  }

}
