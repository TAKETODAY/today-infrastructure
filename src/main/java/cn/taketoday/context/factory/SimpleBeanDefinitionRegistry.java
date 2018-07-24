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
package cn.taketoday.context.factory;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.BeanNameAware;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;

/**
 * @author Today
 * @date 2018年7月8日 下午7:57:22
 */
public class SimpleBeanDefinitionRegistry implements BeanDefinitionRegistry {

	/** Map of bean definition objects, keyed by bean name */
	private final Map<String, BeanDefinition>	beanDefinitionMap	= new ConcurrentHashMap<>(32);

	private Properties							properties			= new Properties();
	
	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {
		
		if(		beanName.equals(BeanPostProcessor.class.getName())			//
				|| beanName.equals(DisposableBean.class.getName())			//
				|| beanName.equals(InitializingBean.class.getName())		//
				|| beanName.equals(BeanNameAware.class.getName())			//
				|| beanName.equals(ApplicationContextAware.class.getName()) //
				|| beanName.equals(Serializable.class.getName())			//
				|| beanName.equals(Cloneable.class.getName())) 				//
		{
			return;
		}
		
		this.beanDefinitionMap.put(beanName, beanDefinition);
	}

	@Override
	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		if (this.beanDefinitionMap.remove(beanName) == null) {
			throw new NoSuchBeanDefinitionException(beanName);
		}
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		BeanDefinition bd = this.beanDefinitionMap.get(beanName);
		if (bd == null) {
			Class<?>[] interfaces;
			try {
				interfaces = Class.forName(beanName).getInterfaces();
			} catch (ClassNotFoundException e) {
				throw new NoSuchBeanDefinitionException("no such bean definition named -> " + beanName);
			}
			for (Class<?> clazz : interfaces) {
				return getBeanDefinition(clazz.getName());
			}
			throw new NoSuchBeanDefinitionException("no such bean definition named -> " + beanName);
		}
		return bd;
	}

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return this.beanDefinitionMap.containsKey(beanName);
	}

	@Override
	public Set<String> getBeanDefinitionNames() {
		return this.beanDefinitionMap.keySet();
	}

	@Override
	public int getBeanDefinitionCount() {
		return this.beanDefinitionMap.size();
	}

	@Override
	public boolean isBeanNameInUse(String beanName) {
		return containsBeanDefinition(beanName);
	}

	@Override
	public Set<BeanDefinition> getBeanDefinitions() {
		Set<BeanDefinition> beanDefinitions = new HashSet<>();
		beanDefinitionMap.forEach((key, value) -> {
			beanDefinitions.add(value);
		});
		return beanDefinitions;
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public Map<String, BeanDefinition> getBeanDefinitionsMap() {
		return beanDefinitionMap;
	}

}
