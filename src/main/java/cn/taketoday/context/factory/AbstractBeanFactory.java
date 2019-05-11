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
package cn.taketoday.context.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.bean.StandardBeanDefinition;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.OrderUtils;

/**
 *
 * @author Today <br>
 *
 *         2018-06-23 11:20:58
 */
public abstract class AbstractBeanFactory implements ConfigurableBeanFactory {

	private static final Logger log = LoggerFactory.getLogger(AbstractBeanFactory.class);

	private BeanNameCreator beanNameCreator;
	/** dependencies */
	private final Set<PropertyValue> dependencies = new HashSet<>(32, 1F);
	/** Bean Post Processors */
	private final List<BeanPostProcessor> postProcessors = new LinkedList<>();
	/** Map of bean instance, keyed by bean name */
	private final Map<String, Object> singletons = new ConcurrentHashMap<>(64, 1f);
	/** Map of bean definition objects, keyed by bean name */
	private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64, 1f);

	// @since 2.1.6
	private boolean fullPrototype = false;
	// @since 2.1.6
	private boolean fullLifecycle = false;

	@Override
	public Object getBean(String name) throws ContextException {

		final Object bean = singletons.get(name);

		if (bean == null) {
			final BeanDefinition beanDefinition = getBeanDefinition(name);
			if (beanDefinition != null) {
				try {
					if (beanDefinition.isSingleton()) {
						return doCreateBean(beanDefinition, name);
					}
					// prototype
					return doCreatePrototype(beanDefinition, name);
				}
				catch (Throwable ex) {
					ex = ExceptionUtils.unwrapThrowable(ex);
					log.error("An Exception Occurred When Getting A Bean Named: [{}], With Msg: [{}]", //
							name, ex.getMessage(), ex);
					throw ExceptionUtils.newContextException(ex);
				}
			}
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
	 * @throws Throwable
	 */
	protected Object doCreatePrototype(BeanDefinition beanDefinition, String name) throws Throwable {

		if (beanDefinition.isFactoryBean()) {
			FactoryBean<?> $factoryBean = (FactoryBean<?>) initializingBean(//
					singletons.get(FACTORY_BEAN_PREFIX + name), name, beanDefinition//
			);
			return $factoryBean.getBean();
		}

		// initialize
		return initializingBean(createBeanInstance(beanDefinition), name, beanDefinition);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) {
		return getBean(getBeanNameCreator().create(requiredType), requiredType);
	}

	/**
	 * Get bean for required type
	 * 
	 * @param requiredType
	 *            bean type
	 * @since 2.1.2
	 * @return
	 */
	protected <T> Object doGetBeanforType(final Class<T> requiredType) {
		Object bean = null;
		for (Entry<String, BeanDefinition> entry : getBeanDefinitionsMap().entrySet()) {
			if (requiredType.isAssignableFrom(entry.getValue().getBeanClass())) {
				bean = getBean(entry.getKey());
				if (bean != null) {
					return bean;
				}
			}
		}
		// fix
		for (Object entry : getSingletonsMap().values()) {
			if (requiredType.isAssignableFrom(entry.getClass())) {
				return entry;
			}
		}
		return bean;
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) {

		final Object bean = getBean(name);
		if (bean != null && requiredType.isInstance(bean)) {
			return requiredType.cast(bean);
		}
		// @since 2.1.2
		return requiredType.cast(doGetBeanforType(requiredType));
	}

	@Override
	public <T> List<T> getBeans(Class<T> requiredType) {
		final Set<T> beans = new HashSet<>();

		for (Entry<String, BeanDefinition> entry : getBeanDefinitionsMap().entrySet()) {
			if (requiredType.isAssignableFrom(entry.getValue().getBeanClass())) {
				@SuppressWarnings("unchecked") //
				T bean = (T) getBean(entry.getKey());
				if (bean != null) {
					beans.add(bean);
				}
			}
		}
		return new ArrayList<>(beans);
	}

	@Override
	@SuppressWarnings("unchecked") //
	public <A extends Annotation, T> List<T> getAnnotatedBeans(Class<A> annotationType) {
		final Set<T> beans = new HashSet<>();

		for (Entry<String, BeanDefinition> entry : getBeanDefinitionsMap().entrySet()) {
			BeanDefinition beanDefinition = entry.getValue();
			if (beanDefinition.getBeanClass().isAnnotationPresent(annotationType)) {
				T bean = (T) getBean(entry.getKey());
				if (bean != null) {
					beans.add(bean);
				}
			}
			else if (beanDefinition instanceof StandardBeanDefinition) {
				// fix #3: when get annotated beans that StandardBeanDefinition missed
				// @since v2.1.6
				Method factoryMethod = ((StandardBeanDefinition) beanDefinition).getFactoryMethod();
				if (factoryMethod.isAnnotationPresent(annotationType)) {
					T bean = (T) getBean(entry.getKey());
					if (bean != null) {
						beans.add(bean);
					}
				}
			}
		}
		return new ArrayList<>(beans);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> requiredType) {
		final Map<String, T> beans = new HashMap<>();

		for (Entry<String, BeanDefinition> entry : getBeanDefinitionsMap().entrySet()) {
			if (requiredType.isAssignableFrom(entry.getValue().getBeanClass())) {
				@SuppressWarnings("unchecked") //
				T bean = (T) getBean(entry.getKey());
				if (bean != null) {
					beans.put(entry.getKey(), bean);
				}
			}
		}
		return beans;
	}

	/**
	 * create bean use default constructor
	 *
	 * @param beanDefinition
	 *            bean definition
	 * @return bean instance
	 * @throws Throwable
	 */
	protected Object createBeanInstance(BeanDefinition beanDefinition) throws Throwable {
		final Object bean = getSingleton(beanDefinition.getName());
		if (bean == null) {
			return ClassUtils.newInstance(beanDefinition, this);
		}
		return bean;
	}

	/**
	 * Apply property values.
	 *
	 * @param bean
	 *            bean instance
	 * @param propertyValues
	 *            property list
	 * @throws IllegalAccessException
	 */
	protected void applyPropertyValues(Object bean, PropertyValue... propertyValues) throws IllegalAccessException {

		for (final PropertyValue propertyValue : propertyValues) {
			Object value = propertyValue.getValue();
			// reference bean
			if (value instanceof BeanReference) {
				final BeanReference beanReference = (BeanReference) value;
				// fix: same name of bean
				value = resolvePropertyValue(beanReference);
				if (value == null) {
					if (beanReference.isRequired()) {
						log.error("[{}] is required.", propertyValue.getField());
						throw new NoSuchBeanDefinitionException(beanReference.getName());
					}
					continue; // if reference bean is null and it is not required ,do nothing,default value
				}
			}
			// set property
			propertyValue.getField().set(bean, value);
		}
	}

	/**
	 * Resolve reference {@link PropertyValue}
	 * 
	 * @param beanReference
	 *            {@link BeanReference} record a reference of bean
	 * 
	 * @return a bean
	 */
	protected Object resolvePropertyValue(BeanReference beanReference) {

		final Class<?> beanClass = beanReference.getReferenceClass();
		final String beanName = beanReference.getName();

		if (fullPrototype//
				&& beanReference.isPrototype() //
				&& beanClass.isInterface() // only support interface TODO cglib support
				&& containsBeanDefinition(beanName)) //
		{
			final BeanDefinition beanDefinition = getBeanDefinition(beanName);
			final Class<?>[] interfaces = beanDefinition.getBeanClass().getInterfaces();
			// @off
			return Proxy.newProxyInstance(beanClass.getClassLoader(),  interfaces, 
				(Object proxy, Method method, Object[] args) -> {
					final Object bean = getBean(beanName, beanClass);
					try {
						return method.invoke(bean, args);
					}
					catch (InvocationTargetException ex) {
						throw ex.getTargetException();
					} finally {
						if (fullLifecycle) {
							// destroyBean after every call
							destroyBean(bean, beanDefinition);
						}
					}
				}
			); //@on
		}
		return getBean(beanName, beanClass);
	}

	/**
	 * @param bean
	 *            bean instance
	 * @param methods
	 *            initialize methods
	 * @throws Exception
	 */
	protected void invokeInitMethods(Object bean, Method... methods) throws Exception {

		for (Method method : methods) {
			method.setAccessible(true); // fix: can not access a member
			method.invoke(bean, ContextUtils.resolveParameter(method, this));
		}

		if (bean instanceof InitializingBean) {
			((InitializingBean) bean).afterPropertiesSet();
		}
	}

	/**
	 * Create bean
	 *
	 * @param beanDefinition
	 *            bean definition
	 * @param name
	 * @return
	 * @throws Throwable
	 */
	protected Object doCreateBean(BeanDefinition beanDefinition, String name) throws Throwable {

		if (beanDefinition.isFactoryBean()) {
			FactoryBean<?> $factoryBean = (FactoryBean<?>) singletons.get(FACTORY_BEAN_PREFIX + name);

			if (!beanDefinition.isInitialized()) {
				$factoryBean = (FactoryBean<?>) initializingBean($factoryBean, name, beanDefinition);
			}
			final Object bean = $factoryBean.getBean();// fix
			if (beanDefinition.isSingleton()) {
				beanDefinition.setInitialized(true);
				singletons.put(name, bean);
			}
			return bean;
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
	 * @throws Throwable
	 */
	protected void doCreateSingleton(Entry<String, BeanDefinition> entry, //
			Set<Entry<String, BeanDefinition>> entrySet) throws Throwable //
	{
		String name = entry.getKey();
		BeanDefinition beanDefinition = entry.getValue();

		if (!beanDefinition.isSingleton() || beanDefinition.isInitialized()) {
			return;// Prototype// has already initialized
		}

		if (beanDefinition.isFactoryBean()) {
			log.debug("[{}] is FactoryBean", name);
			FactoryBean<?> $factoryBean = (FactoryBean<?>) initializingBean(//
					singletons.get(FACTORY_BEAN_PREFIX + name), name, beanDefinition//
			);
			beanDefinition.setInitialized(true);
			singletons.put(name, $factoryBean.getBean());
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
	 * @param currentBeanName
	 *            bean name
	 * @param currentBeanDefinition
	 *            bean definition
	 * @return
	 * @throws Throwable
	 */
	protected Object doCreate(String currentBeanName, BeanDefinition currentBeanDefinition) throws Throwable {

		if (!currentBeanDefinition.isAbstract()) {
			// init
			return initializeSingleton(currentBeanName, currentBeanDefinition);
		}

		// current define
		Class<? extends Object> currentBeanClass = currentBeanDefinition.getBeanClass();

		for (Entry<String, BeanDefinition> entry_ : getBeanDefinitionsMap().entrySet()) {
			BeanDefinition childBeanDefinition = entry_.getValue();
			String childName = childBeanDefinition.getName();

			if (!currentBeanClass.isAssignableFrom(childBeanDefinition.getBeanClass()) || childName.equals(currentBeanName)) {
				continue; // Not beanClass's Child Bean
			}
			// Is
			log.debug("Found The Implementation Of [{}] Bean: [{}].", currentBeanName, childName);
			Object childSingleton = singletons.get(childName);

			try {

				if (childSingleton == null) {
					// current bean is a singleton don't care child bean is singleton or not
					childSingleton = createBeanInstance(childBeanDefinition);
				}
				if (!childBeanDefinition.isInitialized()) {
					// initialize child bean definition
					log.debug("Initialize The Implementation Of [{}] Bean: [{}]", currentBeanName, childName);
					childSingleton = initializingBean(childSingleton, childName, childBeanDefinition);
					singletons.put(childName, childSingleton);
					childBeanDefinition.setInitialized(true);
					currentBeanDefinition.setInitialized(true); // fix not initialize
				}

				singletons.put(currentBeanName, childSingleton);
				return childSingleton;
			}
			catch (Throwable e) {
				e = ExceptionUtils.unwrapThrowable(e);
				childBeanDefinition.setInitialized(false);
				throw new BeanDefinitionStoreException(//
						"Can't store bean named: [" + currentBeanDefinition.getName() + "] With Msg: [" + e.getMessage() + "]", e//
				);
			}
		}
		//
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
	 * @throws Throwable
	 */
	protected Object initializeSingleton(String name, BeanDefinition beanDefinition) throws Throwable {
		Object bean = initializingBean(createBeanInstance(beanDefinition), name, beanDefinition);
		log.debug("Singleton bean is being stored in the name of [{}]", name);

		singletons.put(name, bean);
		beanDefinition.setInitialized(true);

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
	 * @return if initialized?
	 */
	protected boolean createAbstractInstance(Set<Entry<String, BeanDefinition>> entrySet, //
			String currentBeanName, BeanDefinition currentBeanDefinition) //
	{
		if (!currentBeanDefinition.isAbstract()) {
			return false;
		}
		// current define
		Class<? extends Object> currentBeanClass = currentBeanDefinition.getBeanClass();
		for (Entry<String, BeanDefinition> entry_ : entrySet) {

			BeanDefinition childBeanDefinition = entry_.getValue();
			String childName = childBeanDefinition.getName();

			if (!currentBeanClass.isAssignableFrom(childBeanDefinition.getBeanClass()) || childName.equals(currentBeanName)) {
				continue; // Not beanClass's Child Bean
			}

			// Is
			log.debug("Found The Implementation Of [{}] Bean [{}].", currentBeanName, childName);
			Object childSingleton = singletons.get(childName);

			try {

				if (childSingleton == null) {
					// current bean is a singleton don't care child bean is singleton or not
					childSingleton = createBeanInstance(childBeanDefinition);
				}
				if (!childBeanDefinition.isInitialized()) {
					// initialize child bean definition
					log.debug("Initialize The Implementation Of [{}] Bean : [{}] .", currentBeanName, childName);
					childSingleton = initializingBean(childSingleton, childName, childBeanDefinition);
					singletons.put(childName, childSingleton);
					childBeanDefinition.setInitialized(true);
					currentBeanDefinition.setInitialized(true);// fix not initialize
				}
				if (!singletons.containsKey(currentBeanName)) {
					log.debug("Singleton bean is being stored in the name of [{}].", currentBeanName);
					singletons.put(currentBeanName, childSingleton);
				}
				return true;// has already find child bean instance
			}
			catch (Throwable e) {
				e = ExceptionUtils.unwrapThrowable(e);
				childBeanDefinition.setInitialized(false);
				throw new BeanDefinitionStoreException(//
						"Can't store bean named: [" + currentBeanDefinition.getName() + "] With Msg: [" + e.getMessage() + "]", e//
				);
			}
		}
		return false;
	}

	/**
	 * register {@link BeanPostProcessor}s to pool
	 */
	public void registerBeanPostProcessors() {

		log.debug("Start loading BeanPostProcessor.");
		try {

			List<BeanPostProcessor> postProcessors = this.getPostProcessors();

			for (Entry<String, BeanDefinition> entry : getBeanDefinitionsMap().entrySet()) {
				BeanDefinition beanDefinition = entry.getValue();
				if (!BeanPostProcessor.class.isAssignableFrom(beanDefinition.getBeanClass())) {
					continue;
				}
				log.debug("Find a BeanPostProcessor: [{}]", beanDefinition.getBeanClass());
				postProcessors.add((BeanPostProcessor) initializeSingleton(entry.getKey(), beanDefinition));
			}
			OrderUtils.reversedSort(postProcessors);
		}
		catch (Throwable ex) {
			ex = ExceptionUtils.unwrapThrowable(ex);
			log.error("An Exception Occurred When Adding Post Processor To Context: [{}] With Msg: [{}]", //
					this, ex.getMessage(), ex);

			throw ExceptionUtils.newContextException(ex);
		}
	}

	/**
	 * Handle interface dependencies
	 */
	public void handleDependency() {

		final Set<Entry<String, BeanDefinition>> entrySet = getBeanDefinitionsMap().entrySet();

		for (final PropertyValue propertyValue : getDependencies()) {

			final Class<?> propertyType = propertyValue.getField().getType();
			// Abstract
			if (!Modifier.isAbstract(propertyType.getModifiers())) {
				continue;
			}

			final String beanName = ((BeanReference) propertyValue.getValue()).getName();

			// fix: #2 when handle dependency some bean definition has already exist
			BeanDefinition registedBeanDefinition = getBeanDefinition(beanName);
			if (registedBeanDefinition != null) {
				registedBeanDefinition.setAbstract(true);
				continue;
			}

			// handle dependency which is interface and parent object
			for (Entry<String, BeanDefinition> entry : entrySet) {
				BeanDefinition beanDefinition = entry.getValue();

				if (propertyType.isAssignableFrom(beanDefinition.getBeanClass())) {
					// register new bean definition
					registerBeanDefinition(//
							beanName, //
							new DefaultBeanDefinition()//
									.setAbstract(true)//
									.setName(beanName)//
									.setScope(beanDefinition.getScope())//
									.setBeanClass(beanDefinition.getBeanClass())//
									.setInitMethods(beanDefinition.getInitMethods())//
									.setDestroyMethods(beanDefinition.getDestroyMethods())//
									.setPropertyValues(beanDefinition.getPropertyValues())//
					);
					break;// find the first child bean
				}
			}
		}
	}

	/**
	 * Initializing bean.
	 *
	 * @param bean
	 *            bean instance
	 * @param name
	 *            bean name
	 * @param beanDefinition
	 *            bean definition
	 * @return a initialized object
	 * @throws Exception
	 */
	protected Object initializingBean(Object bean, String name, BeanDefinition beanDefinition) throws Exception {

		log.debug("Initializing bean named: [{}].", name);

		aware(bean, name);

		if (!getPostProcessors().isEmpty()) {
			return initWithPostProcessors(bean, name, beanDefinition, getPostProcessors());
		}
		// apply properties
		applyPropertyValues(bean, beanDefinition.getPropertyValues());
		// invoke initialize methods
		invokeInitMethods(bean, beanDefinition.getInitMethods());
		return bean;
	}

	/**
	 * 
	 * @param bean
	 * @param name
	 * @param beanDefinition
	 * @param postProcessors
	 * @return
	 * @throws Exception
	 */
	private Object initWithPostProcessors(Object bean, String name, BeanDefinition beanDefinition, //
			List<BeanPostProcessor> postProcessors) throws Exception //
	{
		// before properties
		for (final BeanPostProcessor postProcessor : postProcessors) {
			bean = postProcessor.postProcessBeforeInitialization(bean, beanDefinition);
		}
		// apply properties
		applyPropertyValues(bean, beanDefinition.getPropertyValues());
		// invoke initialize methods
		invokeInitMethods(bean, beanDefinition.getInitMethods());
		// after properties
		for (final BeanPostProcessor postProcessor : postProcessors) {
			bean = postProcessor.postProcessAfterInitialization(bean, name);
		}
		return bean;
	}

	/**
	 * Inject FrameWork {@link Component}s to application
	 *
	 * @param bean
	 *            bean instance
	 * @param name
	 *            bean name
	 */
	protected abstract void aware(Object bean, String name);

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return !isPrototype(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		BeanDefinition beanDefinition = getBeanDefinition(name);

		if (beanDefinition == null) {
			throw new NoSuchBeanDefinitionException(name);
		}
		return !beanDefinition.isSingleton();
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {

		BeanDefinition type = getBeanDefinition(name);

		if (type == null) {
			throw new NoSuchBeanDefinitionException(name);
		}
		return type.getBeanClass();
	}

	@Override
	public Set<String> getAliases(Class<?> type) {
		return getBeanDefinitionsMap()//
				.entrySet()//
				.stream()//
				.filter(entry -> type.isAssignableFrom(entry.getValue().getBeanClass()))//
				.map(entry -> entry.getKey())//
				.collect(Collectors.toSet());
	}

	@Override
	public void registerBean(Class<?> clazz) throws BeanDefinitionStoreException {
		getBeanDefinitionLoader().loadBeanDefinition(clazz);
	}

	@Override
	public void registerBean(Set<Class<?>> clazz) //
			throws BeanDefinitionStoreException, ConfigurationException //
	{
		getBeanDefinitionLoader().loadBeanDefinitions(clazz);
	}

	@Override
	public void registerBean(String name, Class<?> clazz) throws BeanDefinitionStoreException {
		getBeanDefinitionLoader().loadBeanDefinition(name, clazz);
	}

	@Override
	public void registerBean(String name, BeanDefinition beanDefinition) //
			throws BeanDefinitionStoreException, ConfigurationException //
	{
		getBeanDefinitionLoader().register(name, beanDefinition);
	}

	@Override
	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		getPostProcessors().remove(beanPostProcessor);
		getPostProcessors().add(beanPostProcessor);
	}

	@Override
	public void removeBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		getPostProcessors().remove(beanPostProcessor);
	}

	@Override
	public void registerSingleton(String name, Object bean) {
		if (!name.startsWith(FACTORY_BEAN_PREFIX) && bean instanceof FactoryBean) {// @since v2.1.1
			name = FACTORY_BEAN_PREFIX.concat(name);
		}
		singletons.put(name, bean);
	}

	@Override
	public void registerSingleton(Object bean) {
		registerSingleton(getBeanNameCreator().create(bean.getClass()), bean);
	}

	@Override
	public Map<String, Object> getSingletonsMap() {
		return singletons;
	}

	@Override
	public Object getSingleton(String name) {
		return singletons.get(name);
	}

	/**
	 * Get target singleton
	 * 
	 * @param name
	 *            bean name
	 * @param targetClass
	 * @return
	 */
	public <T> T getSingleton(String name, Class<T> targetClass) {
		return targetClass.cast(getSingleton(name));
	}

	@Override
	public void removeSingleton(String name) {
		singletons.remove(name);
	}

	@Override
	public void removeBean(String name) throws NoSuchBeanDefinitionException {
		removeBeanDefinition(name);
		removeSingleton(name);
	}

	@Override
	public boolean containsSingleton(String name) {
		return singletons.containsKey(name);
	}

	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {

		this.beanDefinitionMap.put(beanName, beanDefinition);

		PropertyValue[] propertyValues = beanDefinition.getPropertyValues();
		if (propertyValues != null && propertyValues.length != 0) {
			for (PropertyValue propertyValue : propertyValues) {
				if (propertyValue.getValue() instanceof BeanReference) {
					this.dependencies.add(propertyValue);
				}
			}
		}
	}

	/**
	 * Destroy a bean with bean instance and bean definition
	 * 
	 * @param beanInstance
	 *            bean instance
	 * @param beanDefinition
	 *            bean definition
	 */
	public void destroyBean(Object beanInstance, BeanDefinition beanDefinition) {

		try {

			if (beanInstance == null || beanDefinition == null) {
				return;
			}
			// use real class
			final Class<? extends Object> beanClass = beanInstance.getClass();
			for (String destroyMethod : beanDefinition.getDestroyMethods()) {
				beanClass.getMethod(destroyMethod).invoke(beanInstance);
			}

			ContextUtils.destroyBean(beanInstance, beanClass.getDeclaredMethods());
		}
		catch (Throwable e) {
			e = ExceptionUtils.unwrapThrowable(e);
			log.error("An Exception Occurred When Destroy a bean: [{}], With Msg: [{}]", //
					beanDefinition.getName(), e.getMessage(), e);
			throw ExceptionUtils.newContextException(e);
		}
	}

	@Override
	public void destroyBean(String name) {

		BeanDefinition beanDefinition = getBeanDefinition(name);

		if (beanDefinition == null && name.startsWith(FACTORY_BEAN_PREFIX)) {
			// if it is a factory bean
			final String factoryBeanName = name.substring(FACTORY_BEAN_PREFIX.length());
			beanDefinition = getBeanDefinition(factoryBeanName);
			destroyBean(getSingleton(factoryBeanName), beanDefinition);
			removeBean(factoryBeanName);
		}
		destroyBean(getSingleton(name), beanDefinition);
		removeBean(name);
	}

	@Override
	public String getBeanName(Class<?> targetClass) {

		for (Entry<String, BeanDefinition> entry : getBeanDefinitionsMap().entrySet()) {
			if (entry.getValue().getBeanClass() == targetClass) {
				return entry.getKey();
			}
		}
		return null;
	}

	@Override
	public void removeBeanDefinition(String beanName) {
		beanDefinitionMap.remove(beanName);
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName) {
		return beanDefinitionMap.get(beanName);
	}

	@Override
	public BeanDefinition getBeanDefinition(Class<?> beanClass) {

		BeanDefinition beanDefinition = getBeanDefinition(getBeanNameCreator().create(beanClass));
		if (beanDefinition != null && beanClass.isAssignableFrom(beanDefinition.getBeanClass())) {
			return beanDefinition;
		}
		for (BeanDefinition definition : getBeanDefinitionsMap().values()) {
			if (beanClass.isAssignableFrom(definition.getBeanClass())) {
				return definition;
			}
		}
		return null;
	}

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return getBeanDefinitionsMap().containsKey(beanName);
	}

	@Override
	public boolean containsBeanDefinition(Class<?> type) {
		return containsBeanDefinition(type, false);
	}

	@Override
	public boolean containsBeanDefinition(Class<?> type, boolean equals) {

		final Map<String, BeanDefinition> beanDefinitionsMap = getBeanDefinitionsMap();

		if (beanDefinitionsMap.containsKey(getBeanNameCreator().create(type))) {
			return true;
		}

		for (final BeanDefinition beanDefinition : beanDefinitionsMap.values()) {
			if (equals) {
				if (type == beanDefinition.getBeanClass()) {
					return true;
				}
			}
			else {
				if (type.isAssignableFrom(beanDefinition.getBeanClass())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Set<String> getBeanDefinitionNames() {
		return getBeanDefinitionsMap().keySet();
	}

	@Override
	public int getBeanDefinitionCount() {
		return getBeanDefinitionsMap().size();
	}

	@Override
	public Map<String, BeanDefinition> getBeanDefinitionsMap() {
		return beanDefinitionMap;
	}

	public Set<PropertyValue> getDependencies() {
		return dependencies;
	}

	@Override
	public void initializeSingletons() throws Throwable {

		log.debug("Initialization of singleton objects.");

		final Set<Entry<String, BeanDefinition>> entrySet = getBeanDefinitionsMap().entrySet();
		for (Entry<String, BeanDefinition> entry : entrySet) {
			doCreateSingleton(entry, entrySet);
		}
		log.debug("The singleton objects are initialized.");
	}

	/**
	 * Initialization singletons that has already in context
	 * 
	 * @throws Exception
	 */
	public void preInitialization() throws Exception {

		for (Entry<String, Object> entry : getSingletonsMap().entrySet()) {
			final String name = entry.getKey();
			final Object singleton = entry.getValue();
			final BeanDefinition beanDefinition = getBeanDefinition(name);
			if (beanDefinition == null || beanDefinition.isInitialized()) {
				continue;
			}
			registerSingleton(name, initializingBean(singleton, name, beanDefinition));
			log.debug("Singleton bean is being stored in the name of [{}].", name);

			beanDefinition.setInitialized(true);
		}
	}

	// -----------------------------------------------------
	@Override
	public void refresh(String name) {

		final BeanDefinition beanDefinition = getBeanDefinition(name);
		if (beanDefinition == null) {
			throw new NoSuchBeanDefinitionException(name);
		}

		try {

			if (beanDefinition.isInitialized()) {
				log.warn("A bean named: [{}] has already initialized", name);
				return;
			}

			final Object initializingBean = initializingBean(//
					createBeanInstance(beanDefinition), name, beanDefinition//
			);

			if (!containsSingleton(name)) {
				registerSingleton(name, initializingBean);
			}

			beanDefinition.setInitialized(true);
		}
		catch (Throwable ex) {
			throw ExceptionUtils.newContextException(ex);
		}
	}

	@Override
	public Object refresh(BeanDefinition beanDefinition) {

		try {
			return initializingBean(createBeanInstance(beanDefinition), beanDefinition.getName(), beanDefinition);
		}
		catch (Throwable ex) {
			throw ExceptionUtils.newContextException(ex);
		}
	}

	@Override
	public void refresh(Class<?> previousClass, Class<?> currentClass) {

//		if (previousClass == currentClass || previousClass.isInterface()) {
		if (previousClass == currentClass) {
			return;
		}

		BeanDefinition previousBeanDefinition = //
				Objects.requireNonNull(getBeanDefinition(previousClass), "No such bean definition : " + previousClass.getName());

		// remove previous bean
		String previousBeanName = previousBeanDefinition.getName();
		removeBean(previousBeanName);

		if (currentClass == null) {
			updateDependencies(previousBeanName, null);
			return;
		}

		// TODO remove all the property bean definition
		getBeanDefinitionLoader().loadBeanDefinition(currentClass);

		BeanDefinition currentBeanDefinition = getBeanDefinition(currentClass);

		if (currentBeanDefinition == null) {
			getBeanDefinitionLoader().loadBeanDefinition(beanNameCreator.create(currentClass), currentClass);
			currentBeanDefinition = getBeanDefinition(currentClass);
		}

		// refresh all property and remove all reference dependencies
		for (PropertyValue propertyValue : currentBeanDefinition.getPropertyValues()) {
			Object value = propertyValue.getValue();
			if (value instanceof BeanReference) {
				BeanReference beanReference = (BeanReference) value;

				Set<PropertyValue> dependencies = getDependencies();
				Object[] dependent = //
						dependencies.stream()//
								.filter(predicate -> beanReference.equals(predicate.getValue()))//
								.toArray();

				for (Object object : dependent) {
					if (beanReference != object) {
						dependencies.remove(object);
					}
				}

				PropertyValue previousPropertyValue = //
						previousBeanDefinition.getPropertyValue(propertyValue.getField().getName());
				// do refresh property
				refresh(previousPropertyValue.getField().getType(), beanReference.getReferenceClass());
			}
		}
		// refresh with new bean name
		String currentName = currentBeanDefinition.getName();

		refresh(currentName);

		Object refreshed = getBean(currentName);

		updateDependencies(currentName, refreshed);
	}

	/**
	 * Update dependencies
	 * 
	 * @param currentName
	 *            bean's new name
	 * @param refreshed
	 *            refreshed object
	 */
	protected void updateDependencies(final String currentName, final Object refreshed) {

		try {

			Class<? extends Object> refreshedClass = refreshed.getClass();
			// update all dependencies
			for (PropertyValue propertyValue : getDependencies()) {

				final Field field = propertyValue.getField();
				final BeanReference beanReference = (BeanReference) propertyValue.getValue();

				if (beanReference.getName().equals(currentName) && //
						field.getType().isAssignableFrom(refreshedClass)) {

					Object bean = getBean(field.getDeclaringClass());
					field.set(bean, refreshed);
				}
			}
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw ExceptionUtils.newContextException(e);
		}
	}

	// -----------------------------
	/**
	 * @return
	 */
	public abstract BeanDefinitionLoader getBeanDefinitionLoader();

	public abstract void setBeanDefinitionLoader(BeanDefinitionLoader beanDefinitionLoader);

	public BeanNameCreator getBeanNameCreator() {
		return beanNameCreator;
	}

	public void setBeanNameCreator(BeanNameCreator beanNameCreator) {
		this.beanNameCreator = beanNameCreator;
	}

	public List<BeanPostProcessor> getPostProcessors() {
		return postProcessors;
	}

	@Override
	public void enableFullPrototype() {
		fullPrototype = true;
	}

	public boolean isFullPrototype() {
		return fullPrototype;
	}

	@Override
	public void enableFullLifecycle() {
		fullLifecycle = true;
	}

}
