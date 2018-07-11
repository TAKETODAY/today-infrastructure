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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.loader.DefaultBeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;

/**
 * @author Today
 * @date 2018年7月3日 下午10:05:21
 */
public class ClassPathApplicationContext extends AbstractBeanFactory implements ApplicationContext {
	
	public ClassPathApplicationContext(Set<Class<?>> actions) {
		this.actions = actions;
		beanDefinitionReader = new DefaultBeanDefinitionLoader(beanDefinitionRegistry);
		loadContext();
	}

	public ClassPathApplicationContext() {
		beanDefinitionReader = new DefaultBeanDefinitionLoader(beanDefinitionRegistry);
	}

	private Set<Class<?>> getAllClazz() {
		actions = ClassUtils.scanPackage("");
		return actions;
	}

	public void loadContext() {
		try {

			String path = ClassUtils.getClassLoader().getResource("").getPath();

			this.loadPropertysConfig(new File(path));

			// load bean from class set
			beanDefinitionReader.loadBeanDefinitions(actions);
//			setBeanDefinitionRegistry(beanDefinitionReader.getRegistry());

			log.debug("init singleton bean start");
			// init singleton bean
			doCreateSingleton(beanDefinitionRegistry);
			log.debug("init singleton bean end");
		} catch (Exception e) {
			log.error("ERROR -> [{}] caused by {}", e.getMessage(), e.getCause(), e);
		}
	}

	@Override
	public void loadContext(String path) {
		try {
			
			this.loadPropertysConfig(new File(path));
			// load bean from class set
			beanDefinitionReader.loadBeanDefinitions(getAllClazz());
//			setBeanDefinitionRegistry(beanDefinitionReader.getRegistry());

			log.debug("init singleton bean start");
			// init singleton bean
			doCreateSingleton(beanDefinitionRegistry);
			log.debug("init singleton bean end");
		} catch (Exception e) {
			log.error("ERROR -> [{}] caused by {}", e.getMessage(), e.getCause(), e);
		}
	}

	/**
	 * @throws IOException
	 */
	public void loadPropertysConfig(File dir) throws IOException {

		File[] listFiles = dir.listFiles(file -> (file.isDirectory()) || (file.getName().endsWith(".properties")));

		for (File file : listFiles) {
			if (file.isDirectory()) { // recursive
				loadPropertysConfig(file);
				continue;
			}
			InputStream inputStream = null;
			try {
				inputStream = new FileInputStream(file);
				beanDefinitionRegistry.getProperties().load(inputStream);
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}
			}
		}
	}

	@Override
	public void loadSuccess() {
		actions.clear();
		actions = null;
	}

	@Override
	public void setBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) {
		this.beanDefinitionRegistry.getBeanDefinitionsMap().putAll(beanDefinitionRegistry.getBeanDefinitionsMap());
	}

	@Override
	public BeanDefinitionRegistry getBeanDefinitionRegistry() {
		return beanDefinitionRegistry;
	}

	@Override
	public void registerBean(Class<?> clazz) throws BeanDefinitionStoreException {
		beanDefinitionReader.loadBeanDefinition(clazz);
		setBeanDefinitionRegistry(beanDefinitionReader.getRegistry());
	}

	@Override
	public void registerBean(Set<Class<?>> clazz) throws BeanDefinitionStoreException {
		beanDefinitionReader.loadBeanDefinitions(clazz);
		setBeanDefinitionRegistry(beanDefinitionReader.getRegistry());
	}

	@Override
	public BeanDefinitionLoader getBeanDefinitionLoader() {
		return beanDefinitionReader;
	}

	@Override
	public Set<Class<?>> getActions() {
		return actions;
	}

	@Override
	public void registerBean(String name, Class<?> clazz) throws BeanDefinitionStoreException {
		beanDefinitionReader.loadBeanDefinition(name, clazz);
	}

	@Override
	public void onRefresh() {
		try {
			doCreateSingleton(beanDefinitionRegistry);
		} catch (Exception ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}
	}

}
