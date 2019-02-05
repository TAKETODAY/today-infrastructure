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
package cn.taketoday.context;

import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.factory.ConfigurableBeanFactory;
import cn.taketoday.context.listener.ApplicationEventPublisher;

import java.io.Closeable;
import java.util.Collection;

/**
 * 
 * @author Today <br>
 *         2018-06-23 16:39:36
 */
public interface ApplicationContext extends ConfigurableBeanFactory, ApplicationEventPublisher, Closeable {

	/**
	 * 
	 * @return
	 */
	ConfigurableEnvironment getEnvironment();

	/**
	 * refresh factory, initialize singleton
	 * 
	 * @since 2.0.1
	 */
	void refresh();

	/**
	 * Load Application Context.
	 * 
	 * <p>
	 * First of all, it will load all the properties files in the given path. If you
	 * use <b>""</b> instead of a exact path like <b>/config</b> ,it will load all
	 * the properties files in the application.
	 * </p>
	 * <p>
	 * And then {@link package_} parameter decided where to load the beans.
	 * </p>
	 * <p>
	 * when all the bean definition stores in the {@link BeanDefinitionRegistry}.
	 * then resolve dependency
	 * </p>
	 * <p>
	 * Then It will find all the bean post processor,and initialize it. Last refresh
	 * context.
	 * </p>
	 * 
	 * @param locations
	 *            packages to scan
	 */
	void loadContext(String... locations);

	/**
	 * load context from given classes
	 * 
	 * @param classes
	 * @since 2.1.2
	 */
	void loadContext(Collection<Class<?>> classes);

	/**
	 * close context
	 */
	@Override
	void close();

	/**
	 * started ?
	 * 
	 * @return
	 */
	boolean hasStarted();

	/**
	 * Get the context startup time stamp
	 * 
	 * @return
	 */
	long getStartupDate();

}
