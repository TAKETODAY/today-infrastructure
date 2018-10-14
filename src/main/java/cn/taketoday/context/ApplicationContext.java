/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context;

import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.event.ObjectRefreshedEvent;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.ObjectFactory;
import cn.taketoday.context.listener.ApplicationEventPublisher;

import java.io.File;
import java.io.IOException;

/**
 * 
 * @author Today <br>
 *         2018-06-23 16:39:36
 */
public interface ApplicationContext extends BeanFactory, ApplicationEventPublisher {

	/**
	 * 
	 * @return
	 */
	ObjectFactory getObjectFactory();

	/**
	 * 
	 * @param objectFactory
	 */
	void setObjectFactory(ObjectFactory objectFactory);

	/**
	 * Refresh bean with given name, and publish {@link ObjectRefreshedEvent}.
	 * 
	 * @param name
	 *            bean name
	 * 
	 * @since 1.2.0
	 */
	void refresh(String name);

	/**
	 * Refresh bean definition, and publish {@link ObjectRefreshedEvent}.
	 * 
	 * @param beanDefinition
	 *            bean definition
	 * @since 2.0.0
	 * @return initialized object
	 */
	Object refresh(BeanDefinition beanDefinition);

	/**
	 * Load properties configuration file. No specific name required.
	 * 
	 * @param dir
	 *            directory
	 * @throws IOException
	 */
	void loadProperties(File dir) throws IOException;

	/**
	 * init success
	 */
	void loadSuccess();

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
	 * when all the bean definition stores in the {@link BeanDefinitionRegistry} .
	 * It will find all the bean post processor,and initialize it.
	 * </p>
	 * <p>
	 * Now it store all the bean and then resolve dependency, Last refresh context.
	 * </p>
	 * 
	 * @param path
	 *            the path to load all the properties files
	 * @param package_
	 *            package to scan
	 */
	void loadContext(String path, String package_);

	/**
	 * close context
	 */
	void close();

}
