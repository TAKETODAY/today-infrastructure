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
package cn.taketoday.context;

import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.factory.ConfigurableBeanFactory;
import cn.taketoday.context.listener.ApplicationListener;

/**
 * @author Today <br>
 * 
 *         2018-11-14 21:16
 */
public interface ConfigurableApplicationContext extends ApplicationContext {

    /**
     * Setting the {@link Environment}
     * 
     * @param environment
     *            {@link Environment} instance
     * @since 2.1.0
     */
    void setEnvironment(ConfigurableEnvironment environment);

    /**
     * Get configurable environment
     * 
     * @since 2.1.0
     */
    @Override
    ConfigurableEnvironment getEnvironment();

    /**
     * Get AbstractBeanFactory
     * 
     * @return A bean factory
     */
    ConfigurableBeanFactory getBeanFactory();

    /**
     * Add an {@link ApplicationListener} that will be notified on context events
     * such as context refresh and context shutdown.
     * <p>
     * 
     * @param listener
     *            the {@link ApplicationListener}
     * @since 2.1.6
     */
    void addApplicationListener(ApplicationListener<?> listener);

}
