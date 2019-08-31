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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.aware.Aware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.aware.BeanNameAware;
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
 * @author TODAY <br>
 *         2018-06-23 11:20:58
 */
public abstract class AbstractBeanFactory implements ConfigurableBeanFactory {

    private static final Logger log = LoggerFactory.getLogger(AbstractBeanFactory.class);

    private BeanNameCreator beanNameCreator;
    /** dependencies */
    private final Set<PropertyValue> dependencies = new HashSet<>(64);
    /** Bean Post Processors */
    private final List<BeanPostProcessor> postProcessors = new ArrayList<>();
    /** Map of bean instance, keyed by bean name */
    private final Map<String, Object> singletons = new ConcurrentHashMap<>(64);
    /** Map of bean definition objects, keyed by bean name */
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

    // @since 2.1.6
    private boolean fullPrototype = false;
    // @since 2.1.6
    private boolean fullLifecycle = false;

    @Override
    public Object getBean(final String name) throws ContextException {

        final BeanDefinition def = getBeanDefinition(name);
        if (def != null) {

            if (def.isInitialized()) { // fix #7
                return getSingleton(name);
            }

            try {
                if (def.isSingleton()) {
                    return doCreateSingleton(def, name);
                }
                // prototype
                return doCreatePrototype(def, name);
            }
            catch (Throwable ex) {
                ex = ExceptionUtils.unwrapThrowable(ex);
                log.error("An Exception Occurred When Getting A Bean Named: [{}], With Msg: [{}]", //
                        name, ex.toString(), ex);
                throw ExceptionUtils.newContextException(ex);
            }
        }
        return getSingleton(name); // if not exits a bean definition return a bean may exits in singletons cache
    }

    /**
     * Create prototype bean instance.
     *
     * @param def
     *            Bean definition
     * @param name
     *            Bean name
     * @return A initialized Prototype bean instance
     * @throws Throwable
     *             If any {@link Exception} occurred when create prototype
     */
    protected Object doCreatePrototype(final BeanDefinition def, final String name) throws Throwable {

        if (def.isFactoryBean()) {
            final FactoryBean<?> $factoryBean = (FactoryBean<?>) initializingBean(//
                    getSingleton(FACTORY_BEAN_PREFIX + name), name, def//
            );
            return $factoryBean.getBean();
        }

        // initialize
        return initializingBean(createBeanInstance(def), name, def);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) {
        return getBean(getBeanNameCreator().create(requiredType), requiredType);
    }

    /**
     * Get bean for required type
     * 
     * @param requiredType
     *            Bean type
     * @since 2.1.2
     */
    protected <T> Object doGetBeanforType(final Class<T> requiredType) {
        Object bean = null;
        for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
            if (requiredType.isAssignableFrom(entry.getValue().getBeanClass())) {
                bean = getBean(entry.getKey());
                if (bean != null) {
                    return bean;
                }
            }
        }
        // fix
        for (final Object entry : getSingletons().values()) {
            if (requiredType.isAssignableFrom(entry.getClass())) {
                return entry;
            }
        }
        return bean;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> requiredType) {

        final Object bean = getBean(name);
        if (bean != null && requiredType.isInstance(bean)) {
            return (T) bean;
        }
        // @since 2.1.2
        return requiredType.cast(doGetBeanforType(requiredType));
    }

    @Override
    public <T> List<T> getBeans(Class<T> requiredType) {
        final Set<T> beans = new LinkedHashSet<>();

        for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
            if (requiredType.isAssignableFrom(entry.getValue().getBeanClass())) {
                @SuppressWarnings("unchecked") //
                T bean = (T) getBean(entry.getKey());
                if (bean != null) {
                    beans.add(bean);
                }
            }
        }
        if (beans.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(beans);
    }

    @Override
    @SuppressWarnings("unchecked") //
    public <A extends Annotation, T> List<T> getAnnotatedBeans(Class<A> annotationType) {
        final Set<T> beans = new LinkedHashSet<>();

        for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
            final BeanDefinition beanDefinition = entry.getValue();

            if (ClassUtils.isAnnotationPresent(beanDefinition.getBeanClass(), annotationType)) {// extend
                final T bean = (T) getBean(entry.getKey());
                if (bean != null) {
                    beans.add(bean);
                }
            }
            else if (beanDefinition instanceof StandardBeanDefinition) {
                // fix #3: when get annotated beans that StandardBeanDefinition missed
                // @since v2.1.6
                final Method factoryMethod = ((StandardBeanDefinition) beanDefinition).getFactoryMethod();
                if (ClassUtils.isAnnotationPresent(factoryMethod, annotationType)) { // extend
                    final T bean = (T) getBean(entry.getKey());
                    if (bean != null) {
                        beans.add(bean);
                    }
                }
            }
        }
        if (beans.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(beans);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> requiredType) {
        final Map<String, T> beans = new HashMap<>();

        for (Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
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

    @Override
    public Map<String, BeanDefinition> getBeanDefinitions() {
        return beanDefinitionMap;
    }

    @Override
    public Map<String, BeanDefinition> getBeanDefinitionsMap() {
        return beanDefinitionMap;
    }

    /**
     * Create bean instance
     *
     * @param def
     *            Bean definition
     * @return Target bean instance
     * @throws Throwable
     *             If any {@link Exception} occurred when create bean instance
     */
    protected Object createBeanInstance(final BeanDefinition def) throws Throwable {
        final Object bean = getSingleton(def.getName());
        if (bean == null) {
            return ClassUtils.newInstance(def, this);
        }
        return bean;
    }

    /**
     * Apply property values.
     *
     * @param bean
     *            Bean instance
     * @param propertyValues
     *            Property list
     * @throws IllegalAccessException
     *             If any {@link Exception} occurred when apply
     *             {@link PropertyValue}s
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
     * @return a {@link PropertyValue} bean
     */
    protected Object resolvePropertyValue(final BeanReference beanReference) {

        final Class<?> beanClass = beanReference.getReferenceClass();
        final String beanName = beanReference.getName();

        if (fullPrototype//
                && beanReference.isPrototype() //
                && beanClass.isInterface() // only support interface TODO cglib support
                && containsBeanDefinition(beanName)) //
        {
            final BeanDefinition beanDefinition = getBeanDefinition(beanName);
            // @off
			return Proxy.newProxyInstance(beanClass.getClassLoader(),  beanDefinition.getBeanClass().getInterfaces(), 
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
     * Invoke initialize methods
     * 
     * @param bean
     *            Bean instance
     * @param methods
     *            Initialize methods
     * @throws Exception
     *             If any {@link Exception} occurred when invoke init methods
     */
    protected void invokeInitMethods(final Object bean, final Method... methods) throws Exception {

        for (final Method method : methods) {
//			method.setAccessible(true); // fix: can not access a member
            method.invoke(bean, ContextUtils.resolveParameter(ClassUtils.makeAccessible(method), this));
        }

        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }
    }

    /**
     * Create {@link Singleton} bean
     *
     * @param def
     *            Bean definition
     * @param name
     *            Bean name
     * @return Bean instance
     * @throws Throwable
     *             If any {@link Exception} occurred when create singleton
     */
    protected Object doCreateSingleton(final BeanDefinition def, final String name) throws Throwable {

        if (def.isFactoryBean()) { // If bean is a FactoryBean not initialized
            final Object $factoryBean = initializingBean(getSingleton(FACTORY_BEAN_PREFIX + name), name, def);

            def.setInitialized(true); // $name bean initialized
            final Object ret = ((FactoryBean<?>) $factoryBean).getBean();// fix
            registerSingleton(name, ret);
            return ret;
        }

        return getImplementation(name, def);
    }

    /**
     * Create singleton bean.
     * 
     * @param beanDefinition
     *            Current {@link BeanDefinition}
     * @throws Throwable
     *             If any {@link Exception} occurred when initialize singleton
     */
    protected void initializeSingleton(final BeanDefinition beanDefinition) throws Throwable {

        if (!beanDefinition.isSingleton() || beanDefinition.isInitialized()) {
            return;// Prototype or bean has already initialized
        }

        final String name = beanDefinition.getName();

        if (beanDefinition.isFactoryBean()) {
            log.debug("[{}] is FactoryBean", name);
            final FactoryBean<?> $factoryBean = (FactoryBean<?>) initializingBean(//
                    getSingleton(FACTORY_BEAN_PREFIX + name), name, beanDefinition//
            );
            beanDefinition.setInitialized(true);
            singletons.put(name, $factoryBean.getBean());
            return;
        }

        if (beanDefinition.isAbstract() && findImplementation(name, beanDefinition)) {
            return;// has already initialized
        }

        // initializing singleton bean
        initializeSingleton(name, beanDefinition);
    }

    /**
     * Get current {@link BeanDefinition} implementation
     * 
     * @param currentBeanName
     *            Bean name
     * @param currentDef
     *            Bean definition
     * @return Current {@link BeanDefinition} implementation
     * @throws Throwable
     *             If any {@link Exception} occurred when get current
     *             {@link BeanDefinition} implementation
     */
    protected Object getImplementation(final String currentBeanName, final BeanDefinition currentDef) throws Throwable {

        if (currentDef.isAbstract()) {
            // current define
            final Class<? extends Object> currentBeanClass = currentDef.getBeanClass();

            for (final Entry<String, BeanDefinition> entry_ : getBeanDefinitions().entrySet()) {
                final BeanDefinition childDef = entry_.getValue();
                final String childName = childDef.getName();

                if (!currentBeanClass.isAssignableFrom(childDef.getBeanClass()) || childName.equals(currentBeanName)) {
                    continue; // Not beanClass's Child Bean
                }
                // Is
                log.debug("Found The Implementation Of [{}] Bean: [{}].", currentBeanName, childName);
                Object childSingleton = getSingleton(childName);

                try {

                    if (childSingleton == null) {
                        // current bean is a singleton don't care child bean is singleton or not
                        childSingleton = createBeanInstance(childDef);
                    }
                    if (!childDef.isInitialized()) {
                        // initialize child bean definition
                        log.debug("Initialize The Implementation Of [{}] Bean: [{}]", currentBeanName, childName);
                        childSingleton = initializingBean(childSingleton, childName, childDef);
                        singletons.put(childName, childSingleton);
                        childDef.setInitialized(true);
                    }

                    singletons.put(currentBeanName, childSingleton);

                    currentDef.setInitialized(true); // fix not initialize

                    return childSingleton;
                }
                catch (Throwable e) {
                    e = ExceptionUtils.unwrapThrowable(e);
                    childDef.setInitialized(false);
                    throw new BeanDefinitionStoreException(//
                            "Can't store bean named: [" + currentDef.getName() + "] With Msg: [" + e + "]", e//
                    );
                }
            }
        }
        return initializeSingleton(currentBeanName, currentDef);
    }

    /**
     * Initialize a singleton bean with given name and it's definition.
     *
     * @param name
     *            Bean name
     * @param beanDefinition
     *            Bean definition
     * @return A initialized singleton bean
     * @throws Throwable
     *             If any {@link Exception} occurred when initialize singleton
     */
    protected Object initializeSingleton(String name, BeanDefinition beanDefinition) throws Throwable {

        if (beanDefinition.isInitialized()) { // fix #7
            return getSingleton(name);
        }

        Object bean = initializingBean(createBeanInstance(beanDefinition), name, beanDefinition);
        log.debug("Singleton bean is being stored in the name of [{}]", name);

        singletons.put(name, bean);
        beanDefinition.setInitialized(true);

        return bean;
    }

    /**
     * Find a abstract implementation bean
     *
     * @param currentBeanName
     *            The target abstract bean name
     * @param currentBeanDefinition
     *            The target abstract bean definition
     * @return if found a abstract implementation bean?
     */
    protected boolean findImplementation(final String currentBeanName, final BeanDefinition currentBeanDefinition) {

        // current define
        final Class<? extends Object> currentBeanClass = currentBeanDefinition.getBeanClass();
        for (final Entry<String, BeanDefinition> entry_ : getBeanDefinitions().entrySet()) {

            final BeanDefinition childBeanDefinition = entry_.getValue();
            final String childName = childBeanDefinition.getName();

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
                }
                if (!singletons.containsKey(currentBeanName)) {
                    log.debug("Singleton bean is being stored in the name of [{}].", currentBeanName);
                    currentBeanDefinition.setInitialized(true);// fix not initialize
                    singletons.put(currentBeanName, childSingleton);
                }
                return true;// has already find child bean instance
            }
            catch (Throwable e) {
                e = ExceptionUtils.unwrapThrowable(e);
                childBeanDefinition.setInitialized(false);
                throw new BeanDefinitionStoreException(//
                        "Can't store bean named: [" + currentBeanDefinition.getName() + "] With Msg: [" + e + "]", e//
                );
            }
        }
        return false;
    }

    /**
     * Register {@link BeanPostProcessor}s to register
     */
    public void registerBeanPostProcessors() {

        log.debug("Start loading BeanPostProcessor.");

        final List<BeanPostProcessor> postProcessors = getPostProcessors();

        postProcessors.addAll(getBeans(BeanPostProcessor.class));
        OrderUtils.reversedSort(postProcessors);
    }

    /**
     * Handle abstract dependencies
     */
    public void handleDependency() {

        final Set<Entry<String, BeanDefinition>> entrySet = getBeanDefinitions().entrySet();

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
     *            Bean instance
     * @param name
     *            Bean name
     * @param def
     *            Bean definition
     * @return A initialized object
     * @throws Throwable
     *             If any {@link Exception} occurred when initialize bean
     */
    protected Object initializingBean(final Object bean, final String name, final BeanDefinition def) throws Throwable {

        log.debug("Initializing bean named: [{}].", name);

        aware(bean, name);

        if (getPostProcessors().isEmpty()) {
            // apply properties
            applyPropertyValues(bean, def.getPropertyValues());
            // invoke initialize methods
            invokeInitMethods(bean, def.getInitMethods());
            return bean;
        }
        return initWithPostProcessors(bean, name, def, getPostProcessors());
    }

    /**
     * Initialize with {@link BeanPostProcessor}s
     * 
     * @param bean
     *            Bean instance
     * @param name
     *            Bean name
     * @param beanDefinition
     *            Current {@link BeanDefinition}
     * @param postProcessors
     *            {@link BeanPostProcessor}s
     * @return Initialized bean
     * @throws Exception
     *             If any {@link Exception} occurred when initialize with processors
     */
    private Object initWithPostProcessors(Object bean, final String name, final BeanDefinition beanDefinition, //
            final List<BeanPostProcessor> postProcessors) throws Exception //
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
     *            Bean instance
     * @param name
     *            Bean name
     */
    protected void aware(final Object bean, final String name) {

        if (bean instanceof Aware) {
            awareInternal(bean, name);
        }
    }

    protected void awareInternal(final Object bean, final String name) {

        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(name);
        }
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(this);
        }
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {

        final BeanDefinition def = getBeanDefinition(name);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(name);
        }
        return def.isSingleton();
    }

    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        return !isSingleton(name);
    }

    @Override
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {

        final BeanDefinition def = getBeanDefinition(name);

        if (def == null) {
            throw new NoSuchBeanDefinitionException(name);
        }
        return def.getBeanClass();
    }

    @Override
    public Set<String> getAliases(Class<?> type) {
        return getBeanDefinitions()//
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
    public final void registerSingleton(final String name, final Object bean) {

        if (name.charAt(0) != FACTORY_BEAN_PREFIX && bean instanceof FactoryBean) {// @since v2.1.1
            singletons.put(FACTORY_BEAN_PREFIX + name, bean);
        }
        else {
            singletons.put(name, bean);
        }
    }

    @Override
    public void registerSingleton(Object bean) {
        registerSingleton(getBeanNameCreator().create(bean.getClass()), bean);
    }

    @Override
    public Map<String, Object> getSingletons() {
        return singletons;
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
     *            Bean name
     * @param targetClass
     *            Target class
     * @return Target singleton
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

        final PropertyValue[] propertyValues = beanDefinition.getPropertyValues();
        if (propertyValues != null && propertyValues.length != 0) {
            for (final PropertyValue propertyValue : propertyValues) {
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
     *            Bean instance
     * @param def
     *            Bean definition
     */
    public void destroyBean(final Object beanInstance, final BeanDefinition def) {

        try {

            if (beanInstance == null || def == null) {
                return;
            }
            // use real class
            final Class<? extends Object> beanClass = beanInstance.getClass();
            for (final String destroyMethod : def.getDestroyMethods()) {
                beanClass.getMethod(destroyMethod).invoke(beanInstance);
            }

            ContextUtils.destroyBean(beanInstance, beanClass.getDeclaredMethods());
        }
        catch (Throwable e) {
            e = ExceptionUtils.unwrapThrowable(e);
            log.error("An Exception Occurred When Destroy a bean: [{}], With Msg: [{}]", //
                    def.getName(), e.toString(), e);
            throw ExceptionUtils.newContextException(e);
        }
    }

    @Override
    public void destroyBean(String name) {

        BeanDefinition beanDefinition = getBeanDefinition(name);

        if (beanDefinition == null && name.charAt(0) == FACTORY_BEAN_PREFIX) {
            // if it is a factory bean
            final String factoryBeanName = name.substring(1);
            beanDefinition = getBeanDefinition(factoryBeanName);
            destroyBean(getSingleton(factoryBeanName), beanDefinition);
            removeBean(factoryBeanName);
        }
        destroyBean(getSingleton(name), beanDefinition);
        removeBean(name);
    }

    @Override
    public String getBeanName(Class<?> targetClass) throws NoSuchBeanDefinitionException {

        for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
            if (entry.getValue().getBeanClass() == targetClass) {
                return entry.getKey();
            }
        }
        throw new NoSuchBeanDefinitionException(targetClass);
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
        for (BeanDefinition definition : getBeanDefinitions().values()) {
            if (beanClass.isAssignableFrom(definition.getBeanClass())) {
                return definition;
            }
        }
        return null;
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return getBeanDefinitions().containsKey(beanName);
    }

    @Override
    public boolean containsBeanDefinition(Class<?> type) {
        return containsBeanDefinition(type, false);
    }

    @Override
    public boolean containsBeanDefinition(final Class<?> type, final boolean equals) {

        if (getBeanDefinitions().containsKey(getBeanNameCreator().create(type))) {
            return true;
        }
        if (equals) {
            for (final BeanDefinition beanDefinition : getBeanDefinitions().values()) {
                if (type == beanDefinition.getBeanClass()) {
                    return true;
                }
            }
        }
        else {
            for (final BeanDefinition beanDefinition : getBeanDefinitions().values()) {
                if (type.isAssignableFrom(beanDefinition.getBeanClass())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Set<String> getBeanDefinitionNames() {
        return getBeanDefinitions().keySet();
    }

    @Override
    public int getBeanDefinitionCount() {
        return getBeanDefinitions().size();
    }

    public Set<PropertyValue> getDependencies() {
        return dependencies;
    }

    @Override
    public void initializeSingletons() throws Throwable {

        log.debug("Initialization of singleton objects.");

        for (final BeanDefinition beanDefinition : getBeanDefinitions().values()) {
            initializeSingleton(beanDefinition);
        }

        log.debug("The singleton objects are initialized.");
    }

    /**
     * Initialization singletons that has already in context
     */
    public void preInitialization() throws Throwable {

        for (Entry<String, Object> entry : getSingletons().entrySet()) {
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
            final Object initializingBean = //
                    initializingBean(createBeanInstance(beanDefinition), beanDefinition.getName(), beanDefinition);

            beanDefinition.setInitialized(true);
            return initializingBean;
        }
        catch (Throwable ex) {
            throw ExceptionUtils.newContextException(ex);
        }
    }

    // -----------------------------

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
