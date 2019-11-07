/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.context.listener;

import java.text.SimpleDateFormat;

import cn.taketoday.context.AbstractApplicationContext;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;

/**
 * @author TODAY <br>
 *         2018-09-09 23:20
 */
@Order(Ordered.LOWEST_PRECEDENCE - Ordered.HIGHEST_PRECEDENCE)
public class ContextCloseListener implements ApplicationListener<ContextCloseEvent> {

    @Override
    public void onApplicationEvent(ContextCloseEvent event) {

        final ApplicationContext applicationContext = event.getApplicationContext();

        final Logger log = LoggerFactory.getLogger(getClass());

        log.info("Closing: [{}] at [{}]", applicationContext,
                 new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(event.getTimestamp()));

        if (applicationContext instanceof AbstractApplicationContext) {
            AbstractBeanFactory beanFactory = ((AbstractApplicationContext) applicationContext).getBeanFactory();
            beanFactory.getDependencies().clear();
            beanFactory.getPostProcessors().clear();
        }

        try {

            for (final String name : applicationContext.getBeanDefinitions().keySet()) {
                applicationContext.destroyBean(name);
            }

            for (final Object bean : applicationContext.getSingletons().values()) {
                ContextUtils.destroyBean(bean, bean.getClass().getDeclaredMethods());
            }
        }
        catch (Throwable e) {
            log.error("An Exception Occurred When Destroy Beans");
            throw ExceptionUtils.newContextException(e);
        }
        finally {
            ClassUtils.clearCache();
        }
    }

}
