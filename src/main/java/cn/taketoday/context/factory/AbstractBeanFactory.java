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

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.OrderUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	@Override
	public Object getBean(String name) throws NoSuchBeanDefinitionException {

		Object bean = singletons.get(name);

		if (bean == null) {
			BeanDefinition beanDefinition = getBeanDefinition(name);
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
	public <T> T getBean(Class<T> requiredType) throws NoSuchBeanDefinitionException {
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
	<T> Object doGetBeanforType(Class<T> requiredType) {
		Object bean = null;
		for (Entry<String, BeanDefinition> entry : getBeanDefinitionsMap().entrySet()) {
			if (requiredType.isAssignableFrom(entry.getValue().getBeanClass())) {
				bean = getBean(entry.getKey());
				if (bean != null) {
					break;
				}
			}
		}
		return bean;
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws NoSuchBeanDefinitionException {

		Object bean = getBean(name);
		if (bean != null) {
			if (requiredType.isInstance(bean)) {
				return requiredType.cast(bean);
			}
		}
		// @since 2.1.2
		return requiredType.cast(doGetBeanforType(requiredType));
	}

	@Override
	public <T> List<T> getBeans(Class<T> requiredType) {

		List<T> beans = new ArrayList<>();

		for (Entry<String, BeanDefinition> entry : getBeanDefinitionsMap().entrySet()) {
			if (requiredType.isAssignableFrom(entry.getValue().getBeanClass())) {
				@SuppressWarnings("unchecked") //
				T bean = (T) getBean(entry.getKey());
				if (bean != null) {
					beans.add(bean);
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
			return ClassUtils.newInstance(beanDefinition.getBeanClass());
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

		for (PropertyValue propertyValue : propertyValues) {
			Object value = propertyValue.getValue();
			// reference bean
			if (value instanceof BeanReference) {
				BeanReference beanReference = (BeanReference) value;
				// fix: same name of bean
				value = this.getBean(beanReference.getName(), beanReference.getReferenceClass());
				if (value == null) {
					if (beanReference.isRequired()) {
						log.error("[{}] is required.", propertyValue.getField());
						throw new NoSuchBeanDefinitionException(beanReference.getName());
					}
					continue; // if reference bean is null and it is not required ,do nothing
				}
			}
			// set property
			propertyValue.getField().set(bean, value);
		}
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
			throw new ContextException(ex);
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
		if (!name.startsWith(FACTORY_BEAN_PREFIX) && bean instanceof FactoryBean) {// since v2.1.1
			name = FACTORY_BEAN_PREFIX + name;
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
		if (propertyValues == null) {
			return;
		}
		for (PropertyValue propertyValue : propertyValues) {
			if (propertyValue.getValue() instanceof BeanReference) {
				this.dependencies.add(propertyValue);
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
			throw new ContextException(ExceptionUtils.unwrapThrowable(ex));
		}
	}

	@Override
	public Object refresh(BeanDefinition beanDefinition) {

		try {

			return initializingBean(createBeanInstance(beanDefinition), beanDefinition.getName(), beanDefinition);
		}
		catch (Throwable ex) {
			throw new ContextException(ExceptionUtils.unwrapThrowable(ex));
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
}
