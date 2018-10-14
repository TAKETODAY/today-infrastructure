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
package test.context.aware;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.aware.BeanNameAware;
import cn.taketoday.context.factory.BeanFactory;

import lombok.Getter;

/**
 * @author Today <br>
 * 
 *         2018-08-08 16:32
 */
@Singleton
@Getter
public class AwareBean implements ApplicationContextAware, BeanFactoryAware, BeanNameAware {

	private String				beanName;

	private ClassLoader			classLoader;

	private BeanFactory			beanFactory;

	private ApplicationContext	applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	@Override
	public String toString() {
		return new StringBuilder()//
				.append("{\n\t\"beanName\":\"")//
				.append(beanName)//
				.append("\",\n\t\"classLoader\":\"")//
				.append(classLoader)//
				.append("\",\n\t\"beanFactory\":\"")//
				.append(beanFactory)//
				.append("\",\n\t\"applicationContext\":\"")//
				.append(applicationContext).append("\"\n}")//
				.toString();
	}

}
