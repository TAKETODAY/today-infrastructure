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
package cn.taketoday.framework.aware;

import java.util.Objects;

import javax.servlet.ServletContext;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.aware.Aware;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.factory.BeanPostProcessor;
import cn.taketoday.framework.WebServerApplicationContext;
import cn.taketoday.web.ServletContextAware;

/**
 * @author Today <br>
 * 
 *         2019-01-19 18:50
 */
@Order(Ordered.HIGHEST_PRECEDENCE * 2)
public class WebApplicationContextAwareProcessor implements BeanPostProcessor {

    private final ServletContext servletContext;

    private final WebServerApplicationContext applicationContext;

    public WebApplicationContextAwareProcessor(ServletContext servletContext, WebServerApplicationContext applicationContext) {
        this.servletContext = servletContext;
        this.applicationContext = applicationContext;
        Objects.requireNonNull(applicationContext, "applicationContext can't be null");
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, BeanDefinition beanDefinition) throws Exception {

        if (bean instanceof Aware) {
            if (bean instanceof ServletContextAware) {
                ((ServletContextAware) bean).setServletContext(servletContext);
            }
            if (bean instanceof WebServerApplicationContextAware) {
                ((WebServerApplicationContextAware) bean).setWebServerApplicationContext(applicationContext);
            }
        }
        return bean;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof WebApplicationContextAwareProcessor) {
            WebApplicationContextAwareProcessor processor = ((WebApplicationContextAwareProcessor) obj);
            if (applicationContext == processor.applicationContext) {
                return true;
            }
        }
        return false;
    }

}
