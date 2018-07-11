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

import java.io.File;
import java.io.IOException;
import java.util.Set;

import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.loader.BeanDefinitionLoader;

/**
 * @author Today
 * @date 2018年6月23日 下午4:39:36
 */
public interface ApplicationContext extends BeanFactory {

	public void loadPropertysConfig(File dir) throws IOException;

	public void loadSuccess();

	public void loadContext();

	public void loadContext(String path);

	/**
	 * 
	 * @param beanDefinitionRegistry
	 */
	public void setBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry);

	public BeanDefinitionRegistry getBeanDefinitionRegistry();

	public Set<Class<?>> getActions();
	
	public BeanDefinitionLoader getBeanDefinitionLoader();

}
