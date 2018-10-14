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

import java.util.Set;

import lombok.NonNull;

/**
 * 
 * @author Today <br>
 *         2018-07-03 22:05:21
 */
public class DefaultApplicationContext extends AbstractApplicationContext {

	/**
	 * start with given class set
	 * 
	 * @param actions
	 */
	public DefaultApplicationContext(Set<Class<?>> actions) {
		this();
		loadContext(actions);
	}

	/**
	 * start with given package
	 * 
	 * @param package_
	 */
	public DefaultApplicationContext(String package_) {
		this();
		loadContext();
	}

	/**
	 * auto load and clear cache?
	 * 
	 * @param clear
	 */
	public DefaultApplicationContext(boolean clear) {
		this();
		loadContext();
		if (clear) {
			loadSuccess();
		}
	}

	/**
	 * default constructor
	 */
	public DefaultApplicationContext() {
		super();
	}

	@Override
	public void loadContext(@NonNull String path, @NonNull String package_) {
		
		try {
			
			// load bean definition
			this.loadBeanDefinition(path, package_);
			// add bean post processor
			this.addBeanPostProcessor();
			// handle dependency
			super.handleDependency(beanDefinitionRegistry.getBeanDefinitionsMap().entrySet());
			
			onRefresh();
		} //
		catch (Exception e) {
			log.error("ERROR -> [{}] caused by {}", e.getMessage(), e.getCause(), e);
		}
	}


}
