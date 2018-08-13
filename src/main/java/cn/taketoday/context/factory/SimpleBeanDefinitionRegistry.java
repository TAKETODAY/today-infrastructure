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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.BeanClassLoaderAware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.aware.BeanNameAware;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.core.Constant;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Today <br>
 * 
 * 		2018-07-08 19:57:22
 */
@NoArgsConstructor
public final class SimpleBeanDefinitionRegistry implements BeanDefinitionRegistry {

	protected final Logger log = LoggerFactory.getLogger(SimpleBeanDefinitionRegistry.class);
	
	private Properties properties = new Properties();
	
	/** Map of bean instance*/
	private final Map<String, Object> singletons = new ConcurrentHashMap<>(16);
	/** Map of bean definition objects, keyed by bean name */
	private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(16);
	/** dependency */
	private final Set<PropertyValue> dependency = new HashSet<>();
	
	private Set<String> discardedNames = new HashSet<String>() {
		
		private static final long serialVersionUID = -1534194758376819762L; {
			
			add(Constant.class.getName());
			add(Cloneable.class.getName());
			add(FactoryBean.class.getName());
			add(Serializable.class.getName());
			add(BeanNameAware.class.getName());
			add(DisposableBean.class.getName());
			add(BeanFactoryAware.class.getName());
			add(InitializingBean.class.getName());
			add(BeanPostProcessor.class.getName());
			add(BeanClassLoaderAware.class.getName());
//			add(PropertyValueResolver.class.getName());
			add(ApplicationContextAware.class.getName());
			
		}
	};
	
	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {
		if(discardedNames.contains(beanName)) {
			return ;
		}
		this.beanDefinitionMap.put(beanName, beanDefinition);
		
		PropertyValue[] propertyValues = beanDefinition.getPropertyValues();
		if(propertyValues == null) {
			return ;
		}
		this.dependency.addAll(Arrays.asList(propertyValues));
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
	public Properties getProperties() {
		return properties;
	}

	@Override
	public Map<String, BeanDefinition> getBeanDefinitionsMap() {
		return beanDefinitionMap;
	}

	@Override
	public void addExcludeName(String name) {
		discardedNames.add(name);
	}

	@Override
	public Object getInstance(String name){
		return singletons.get(name);
	}

	@Override
	public Object putInstance(String name, Object bean) {
		return singletons.put(name, bean);
	}

	@Override
	public boolean containsInstance(String name) {
		return singletons.containsKey(name);
	}

	@Override
	public Set<PropertyValue> getDependency() {
		return dependency;
	}
	
}
