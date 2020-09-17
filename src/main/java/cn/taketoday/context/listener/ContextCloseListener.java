/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import static cn.taketoday.context.utils.ContextUtils.destroyBean;
import static cn.taketoday.context.utils.ExceptionUtils.unwrapThrowable;

import java.text.SimpleDateFormat;

import cn.taketoday.context.AbstractApplicationContext;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;

/**
 * @author TODAY <br>
 *         2018-09-09 23:20
 */
public class ContextCloseListener extends OrderedSupport implements ApplicationListener<ContextCloseEvent> {

    public ContextCloseListener() {
        this(Ordered.LOWEST_PRECEDENCE - Ordered.HIGHEST_PRECEDENCE);
    }

    public ContextCloseListener(int order) {
        super(order);
    }

    @Override
    public void onApplicationEvent(ContextCloseEvent event) {
        final ApplicationContext context = event.getApplicationContext();
        final Logger log = LoggerFactory.getLogger(getClass());
        log.info("Closing: [{}] at [{}]", context,
                 new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(event.getTimestamp()));

        for (final String name : context.getBeanDefinitions().keySet()) {
            try {
                context.destroyBean(name);
            }
            catch (final Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
        for (final Object bean : context.getSingletons().values()) {
            try {
                destroyBean(bean);
            }
            catch (Throwable e) {
                e = unwrapThrowable(e);
                log.error(e.getMessage(), e);
            }
        }

        if (context instanceof AbstractApplicationContext) {
            AbstractBeanFactory beanFactory = ((AbstractApplicationContext) context).getBeanFactory();
            beanFactory.getDependencies().clear();
            beanFactory.getPostProcessors().clear();
        }
        ClassUtils.clearCache();
    }

}
