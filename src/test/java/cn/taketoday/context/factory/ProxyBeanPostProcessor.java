/**
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.factory;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;

/**
 * @author Today <br>
 * 
 *         2018-09-09 20:14
 */
//@Singleton
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProxyBeanPostProcessor implements BeanPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(ProxyBeanPostProcessor.class);

    @Override
    public Object postProcessBeforeInitialization(Object bean, BeanDefinition beanDefinition) {
        log.debug("ProxyBeanPostProcessor Before named :[{}]", beanDefinition.getName());
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, BeanDefinition def) throws Exception {
        log.debug("ProxyBeanPostProcessor After :[{}]", def.getName());
        return bean;
    }

}
