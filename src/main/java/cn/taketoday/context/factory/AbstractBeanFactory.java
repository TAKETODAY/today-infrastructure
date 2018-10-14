/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.aware.BeanNameAware;
import cn.taketoday.context.aware.ObjectFactoryAware;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.loader.BeanDefinitionLoader;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.NonNull;

/**
 * 
 * @author Today <br>
 * 
 *         2018-06-23 11:20:58
 */
public abstract class AbstractBeanFactory implements BeanFactory {

	private final Logger log = LoggerFactory.getLogger(AbstractBeanFactory.class);

	/** storage BeanDefinition */
	protected BeanDefinitionRegistry beanDefinitionRegistry;
	/** resolve beanDefinition which It is marked annotation */
	protected BeanDefinitionLoader beanDefinitionLoader;
	/** Bean Post Processors */
	protected List<BeanPostProcessor> postProcessors;
	/** bean instance factory */
	protected ObjectFactory objectFactory;

	@Override
	public Object getBean(String name) throws NoSuchBeanDefinitionException {

		Object bean = beanDefinitionRegistry.getSingleton(name);

		if (bean != null) {
			return bean;
		}

		try {

			BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(name);

			if (beanDefinition == null) {
				log.warn("No such bean definition named : [{}].", name);
				return null;
			}
			if (beanDefinition.isSingleton()) {
				return doCreateBean(beanDefinition, name);
			}
			// prototype
			return doCreatePrototype(beanDefinition, name);
		} //
		catch (Exception ex) {
			log.error("ERROR -> [{}] caused by [{}]", ex.getMessage(), ex.getCause(), ex);
		}
		return bean;
	}

	/**
	 * Create prototype bean instance.
	 * 
	 * @param beanDefinition
	 *            bean definition
	 * @param name
	 *            bean name
	 * @return a initialized Prototype bean instance
	 * @throws Exception
	 */
	protected Object doCreatePrototype(BeanDefinition beanDefinition, String name) throws Exception {

		if (beanDefinition.isFactoryBean()) {
			FactoryBean<?> $factoryBean = (FactoryBean<?>) initializingBean(//
					beanDefinitionRegistry.getSingleton(FACTORY_BEAN_PREFIX + name), name, beanDefinition//
			);
			return $factoryBean.getBean();
		}
		// initialize
		return initializingBean(createBeanInstance(beanDefinition), name, beanDefinition);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws NoSuchBeanDefinitionException {
		return requiredType.cast(getBean(requiredType.getSimpleName()));
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws NoSuchBeanDefinitionException {
		return requiredType.cast(getBean(name));
	}

	/**
	 * create bean use default constructor
	 * 
	 * @param beanDefinition
	 *            bean definition
	 * @return bean instance
	 * @throws Exception
	 */
	protected Object createBeanInstance(BeanDefinition beanDefinition) {

		return objectFactory.create(beanDefinition.getBeanClass());
	}

	/**
	 * apply property values.
	 * 
	 * @param bean
	 *            bean instance
	 * @param propertyValues
	 *            property list
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws ContextException
	 */
	protected void applyPropertyValues(Object bean, PropertyValue... propertyValues) throws IllegalAccessException {

		for (PropertyValue propertyValue : propertyValues) {
			Object value = propertyValue.getValue();
			// reference bean
			if (value instanceof BeanReference) {
				BeanReference beanReference = ((BeanReference) value);
				value = this.getBean(beanReference.getName());
				if (value == null) {
					if (beanReference.isRequired()) {
						throw new NoSuchBeanDefinitionException(
								"Bean definition : [" + beanReference.getName() + "] is required.");
					}
					continue;
				}
			}
			// set property
			propertyValue.getField().set(bean, value);
		}
	}

	/**
	 * create bean use default constructor
	 * 
	 * @param beanDefinition
	 *            bean definition
	 * @return
	 * @throws Exception
	 */
	protected Object doCreateBean(BeanDefinition beanDefinition, String name) throws Exception {

		if (beanDefinition.isFactoryBean()) {

			FactoryBean<?> $factoryBean = (FactoryBean<?>) beanDefinitionRegistry
					.getSingleton(FACTORY_BEAN_PREFIX + name);

			if (!beanDefinition.isInitialized()) {
				$factoryBean = (FactoryBean<?>) initializingBean($factoryBean, name, beanDefinition);
			}
			if (beanDefinition.isSingleton()) {
				beanDefinition.setInitialized(true);
				beanDefinitionRegistry.putSingleton(name, $factoryBean.getBean());
			}
			return $factoryBean.getBean();
		}

		return doCreate(name, beanDefinition);
	}

	/**
	 * Create singleton bean.
	 * 
	 * @param entry
	 *            bean definition entry
	 * @param entrySet
	 *            the set of bean definition
	 * @throws Exception
	 */
	protected void doCreateSingleton(Entry<String, BeanDefinition> entry, Set<Entry<String, BeanDefinition>> entrySet)
			throws Exception {

		String name = entry.getKey();
		BeanDefinition beanDefinition = entry.getValue();

		if (!beanDefinition.isSingleton()) {
			return;// Prototype
		}

		if (beanDefinition.isInitialized()) {
			return;// has already initialized
		}

		if (beanDefinition.isFactoryBean()) {
			log.debug("[{}] is FactoryBean", name);
			FactoryBean<?> $factoryBean = (FactoryBean<?>) initializingBean(//
					beanDefinitionRegistry.getSingleton(FACTORY_BEAN_PREFIX + name), name, beanDefinition//
			);

			beanDefinition.setInitialized(true);
			beanDefinitionRegistry.putSingleton(name, $factoryBean.getBean());
			return;
		}

		if (createAbstractInstance(entrySet, name, beanDefinition)) {
			return;// has already initialized
		}

		// initializing singleton bean
		initializeSingleton(name, beanDefinition);
	}

	/**
	 * 
	 * 
	 * @param currentBeanName
	 * @param currentBeanDefinition
	 * @return
	 * @throws Exception
	 */
	protected Object doCreate(String currentBeanName, BeanDefinition currentBeanDefinition) throws Exception {

		if (!currentBeanDefinition.isAbstract()) {
			// init
			return initializeSingleton(currentBeanName, currentBeanDefinition);
		}

		Set<Entry<String, BeanDefinition>> entrySet = beanDefinitionRegistry.getBeanDefinitionsMap().entrySet();

		// current define
		Class<? extends Object> currentBeanClass = currentBeanDefinition.getBeanClass();

		for (Entry<String, BeanDefinition> entry_ : entrySet) {
			BeanDefinition childBeanDefinition = entry_.getValue();
			String childName = childBeanDefinition.getName();

			if (!currentBeanClass.isAssignableFrom(childBeanDefinition.getBeanClass())
					|| childName.equals(currentBeanName)) {
				continue; // Not beanClass's Child Bean
			}
			// Is
			log.debug("Found The Implementation Of [{}] Bean [{}].", currentBeanName, childName);
			Object childSingleton = beanDefinitionRegistry.getSingleton(childName);

			try {

				if (childSingleton == null) {
					// current bean is a singleton don't care child bean is singleton or not
					childSingleton = createBeanInstance(childBeanDefinition);
				}
				if (!childBeanDefinition.isInitialized()) {
					// initialize child bean definition
					log.debug("Initialize The Implementation Of [{}] Bean : [{}] .", currentBeanName, childName);
					childSingleton = initializingBean(childSingleton, childName, childBeanDefinition);
					beanDefinitionRegistry.putSingleton(childName, childSingleton);
					childBeanDefinition.setInitialized(true);
				}

				beanDefinitionRegistry.putSingleton(currentBeanName, childSingleton);
				return childSingleton;
			} catch (Exception e) {
				childBeanDefinition.setInitialized(false);
				throw new BeanDefinitionStoreException("Can't store bean : [" + currentBeanDefinition.getName() + "].");
			}
		}
		return initializeSingleton(currentBeanName, currentBeanDefinition);
	}

	/**
	 * Initialize a singleton bean with given name and it's definition.
	 * 
	 * @param name
	 *            bean name
	 * @param beanDefinition
	 *            bean definition
	 * @return A initialized singleton bean
	 * @throws Exception
	 */
	protected Object initializeSingleton(String name, BeanDefinition beanDefinition) throws Exception {
		Object bean = initializingBean(createBeanInstance(beanDefinition), name, beanDefinition);
		log.debug("Singleton bean is being stored in the name of [{}].", name);
		beanDefinition.setInitialized(true);
		beanDefinitionRegistry.putSingleton(name, bean);
		return bean;
	}

	/**
	 * Create a abstract implementation bean
	 * 
	 * @param entrySet
	 *            all bean definition
	 * @param currentBeanName
	 *            the target abstract bean name
	 * @param currentBeanDefinition
	 *            the target abstract bean definition
	 * 
	 */
	protected boolean createAbstractInstance(Set<Entry<String, BeanDefinition>> entrySet, String currentBeanName,
			BeanDefinition currentBeanDefinition) {

		if (!currentBeanDefinition.isAbstract()) {
			return false;
		}
		// current define
		Class<? extends Object> currentBeanClass = currentBeanDefinition.getBeanClass();
		for (Entry<String, BeanDefinition> entry_ : entrySet) {
			BeanDefinition childBeanDefinition = entry_.getValue();
			String childName = childBeanDefinition.getName();

			if (!currentBeanClass.isAssignableFrom(childBeanDefinition.getBeanClass())
					|| childName.equals(currentBeanName)) {
				continue; // Not beanClass's Child Bean
			}

			// Is
			log.debug("Found The Implementation Of [{}] Bean [{}].", currentBeanName, childName);
			Object childSingleton = beanDefinitionRegistry.getSingleton(childName);

			try {

				if (childSingleton == null) {
					// current bean is a singleton don't care child bean is singleton or not
					childSingleton = createBeanInstance(childBeanDefinition);
				}
				if (!childBeanDefinition.isInitialized()) {
					// initialize child bean definition
					log.debug("Initialize The Implementation Of [{}] Bean : [{}] .", currentBeanName, childName);
					childSingleton = initializingBean(childSingleton, childName, childBeanDefinition);
					beanDefinitionRegistry.putSingleton(childName, childSingleton);
					childBeanDefinition.setInitialized(true);
				}
				if (!beanDefinitionRegistry.containsSingleton(currentBeanName)) {
					log.debug("Singleton bean is being stored in the name of [{}].", currentBeanName);
					beanDefinitionRegistry.putSingleton(currentBeanName, childSingleton);
				}
				return true;// has already find child bean instance
			} catch (Exception e) {
				childBeanDefinition.setInitialized(false);
				throw new BeanDefinitionStoreException("Can't store bean : [" + currentBeanDefinition.getName() + "].");
			}
		}
		return false;
	}

	/**
	 * add BeanPostProcessor to pool
	 * 
	 */
	public abstract void addBeanPostProcessor();

	/**
	 * 
	 * Handle interface from dependency
	 * 
	 * @param entrySet
	 * @throws BeanDefinitionStoreException
	 */
	protected void handleDependency(Set<Entry<String, BeanDefinition>> entrySet) throws BeanDefinitionStoreException {

		Iterator<PropertyValue> iterator = beanDefinitionRegistry.getDependency().iterator();// all dependency

		while (iterator.hasNext()) {

			Class<?> propertyType = iterator.next().getField().getType();

			// interface
			String beanName = propertyType.getSimpleName();
			if (beanDefinitionRegistry.containsBeanDefinition(beanName) || !propertyType.isInterface()) {
				continue;
			}

			// handle dependency which is interface and parent object
			for (Entry<String, BeanDefinition> entry : entrySet) {
				BeanDefinition beanDefinition = entry.getValue();

				if (Modifier.isAbstract(propertyType.getModifiers())
						&& propertyType.isAssignableFrom(beanDefinition.getBeanClass())) {
					// register bean definition
					beanDefinitionRegistry.registerBeanDefinition(//
							beanName, //
							new BeanDefinition()//
									.setAbstract(true)//
									.setName(beanName)//
									.setScope(beanDefinition.getScope())//
									.setBeanClass(beanDefinition.getBeanClass())//
									.setPropertyValues(beanDefinition.getPropertyValues())//
					);
				}
			}
		}
	}

	/**
	 * initializing bean.
	 * 
	 * @param bean
	 *            bean instance
	 * @param name
	 *            bean name
	 * @param propertyValues
	 *            property value
	 * @throws Exception
	 */
	protected Object initializingBean(Object bean, String name, BeanDefinition beanDefinition) throws Exception {

		log.debug("Initializing bean named -> [{}].", name);

		aware(bean, name);

		// before properties
		for (BeanPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeforeInitialization(bean, beanDefinition);
		}

		// apply properties
		applyPropertyValues(bean, beanDefinition.getPropertyValues());

		if (bean instanceof InitializingBean) {
			((InitializingBean) bean).afterPropertiesSet();
		}

		// after properties
		for (BeanPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessAfterInitialization(bean, name);
		}
		return bean;
	}

	/**
	 * 
	 * 
	 * @param bean
	 *            bean instance
	 * @param name
	 *            bean name
	 */
	protected void aware(Object bean, String name) {
		// aware
		if (bean instanceof BeanNameAware) {
			((BeanNameAware) bean).setBeanName(name);
		}
		if (bean instanceof ApplicationContextAware) {
			((ApplicationContextAware) bean).setApplicationContext((ApplicationContext) this);
		}
		if (bean instanceof BeanFactoryAware) {
			((BeanFactoryAware) bean).setBeanFactory(this);
		}
		if (bean instanceof ObjectFactoryAware) {
			((ObjectFactoryAware) bean).setObjectFactory(objectFactory);
		}
	}

	@Override
	public void removeBeanDefinition(String name) throws NoSuchBeanDefinitionException {
		beanDefinitionRegistry.removeBeanDefinition(name);
	}

	@Override
	public boolean containsBeanDefinition(String name) {
		return beanDefinitionRegistry.containsBeanDefinition(name);
	}

	@Override
	public boolean containsBeanDefinition(Class<?> type) {
		return containsBeanDefinition(type.getSimpleName());
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return !isPrototype(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(name);

		if (beanDefinition == null) {

			log.error("no such bean named -> [{}]", name);
			throw new NoSuchBeanDefinitionException("no such bean exception");
		}
		return !beanDefinition.isSingleton();
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {

		BeanDefinition type = beanDefinitionRegistry.getBeanDefinition(name);

		if (type == null) {
			log.error("no such bean exception");
			throw new NoSuchBeanDefinitionException("no such bean exception");
		}
		return type.getBeanClass();
	}

	@Override
	public Set<String> getAliases(Class<?> type) {

		Set<String> names = new HashSet<>();

		Iterator<Entry<String, BeanDefinition>> iterator = beanDefinitionRegistry.getBeanDefinitionsMap().entrySet()
				.iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, BeanDefinition> entry = (Map.Entry<String, BeanDefinition>) iterator.next();
			if (entry.getValue().getBeanClass() == type) {
				names.add(entry.getKey());
			}
		}
		return names;
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
	public void registerBeanDefinition(Class<?> clazz) throws BeanDefinitionStoreException, ConfigurationException {
		beanDefinitionLoader.loadBeanDefinition(clazz);
	}

	@Override
	public void registerBeanDefinition(Set<Class<?>> clazz)
			throws BeanDefinitionStoreException, ConfigurationException {
		beanDefinitionLoader.loadBeanDefinitions(clazz);
	}

	@Override
	public BeanDefinitionLoader getBeanDefinitionLoader() {
		return beanDefinitionLoader;
	}

	@Override
	public void registerBeanDefinition(String name, Class<?> clazz) throws BeanDefinitionStoreException {
		beanDefinitionLoader.loadBeanDefinition(name, clazz);
	}

	@Override
	public void registerBeanDefinition(@NonNull String name, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException, ConfigurationException {
		beanDefinitionLoader.register(name, beanDefinition);
	}

	@Override
	public void registerSingleton(String name, @NonNull Object bean) {
		beanDefinitionRegistry.putSingleton(name, bean);
	}

}
