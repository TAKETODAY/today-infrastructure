/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.aware.Aware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.aware.BeanNameAware;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.bean.FactoryBeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.cglib.proxy.Enhancer;
import cn.taketoday.context.cglib.proxy.MethodInterceptor;
import cn.taketoday.context.env.DefaultBeanNameCreator;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;

/**
 * @author TODAY <br>
 *         2018-06-23 11:20:58
 */
public abstract class AbstractBeanFactory implements ConfigurableBeanFactory {

    private static final Logger log = LoggerFactory.getLogger(AbstractBeanFactory.class);

    /** bean name creator */
    private BeanNameCreator beanNameCreator;
    /** object factories */
    private Map<Class<?>, Object> objectFactories;
    /** dependencies */
    private final Set<PropertyValue> dependencies = new HashSet<>(64);
    /** Bean Post Processors */
    private final List<BeanPostProcessor> postProcessors = new ArrayList<>();
    /** Map of bean instance, keyed by bean name */
    private final Map<String, Object> singletons = new HashMap<>(64);
    /** Map of bean definition objects, keyed by bean name */
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

    // @since 2.1.6
    private boolean fullPrototype = false;
    // @since 2.1.6
    private boolean fullLifecycle = false;

    /**
     * @since 2.1.7 Preventing repeated initialization of beans(Prevent duplicate
     *        initialization) , Prevent Cycle Dependency
     */
    private final HashSet<String> currentInitializingBeanName = new HashSet<>();

    @Override
    public Object getBean(final String name) throws ContextException {

        final BeanDefinition def = getBeanDefinition(name);
        if (def != null) {
            return getBean(name, def);
        }
        return getSingleton(name); // if not exits a bean definition return a bean may exits in singletons cache
    }

    @Override
    public Object getBean(BeanDefinition def) {
        return getBean(def.getName(), def);
    }

    public final Object getBean(final String name, final BeanDefinition def) throws ContextException {

        if (def.isInitialized()) { // fix #7
            return getSingleton(name);
        }
        try {
            if (def.isSingleton()) {
                return doCreateSingleton(def, name);
            }
            return doCreatePrototype(def, name); // prototype
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            throw new ContextException("An Exception Occurred When Getting A Bean Named: [" + name + "], With Msg: [" + ex + "]", ex);
        }
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
            final FactoryBean<?> factoryBean = getFactoryBean(def, name);
            initializingBean(factoryBean, name, def);
            return factoryBean.getBean();
        }
        // initialize
        return initializingBean(createBeanInstance(name, def), name, def);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {

        Object bean = getBean(getBeanNameCreator().create(requiredType));
        if (bean != null && requiredType.isInstance(bean)) {
            return (T) bean;
        }
        return (T) doGetBeanforType(requiredType);
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
        if (requiredType.isInstance(bean)) {
            return (T) bean;
        }
        return null;
    }

    @Override
    public <T> List<T> getBeans(Class<T> requiredType) {
        final Set<T> beans = new HashSet<>();

        for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
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
    @SuppressWarnings("unchecked")
    public <A extends Annotation, T> List<T> getAnnotatedBeans(Class<A> annotationType) {
        final Set<T> beans = new HashSet<>();

        for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
            if (entry.getValue().isAnnotationPresent(annotationType)) {
                final T bean = (T) getBean(entry.getKey());
                if (bean != null) {
                    beans.add(bean);
                }
            }
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
     * <p>
     * If target bean is {@link Scope#SINGLETON} will be register is to the
     * singletons pool
     * 
     * @param def
     *            Bean definition
     * @return Target bean instance
     * @throws Throwable
     *             If any {@link Exception} occurred when create bean instance
     */
    protected Object createBeanInstance(final BeanDefinition def) throws Throwable {
        return createBeanInstance(def.getName(), def);
    }

    /**
     * Create bean instance with given name and {@link BeanDefinition}
     * 
     * <p>
     * If target bean is {@link Scope#SINGLETON} will be register is to the
     * singletons pool
     * 
     * @param name
     *            Bean name
     * @param def
     *            Bean definition
     * @return Target bean instance
     * @throws Throwable
     *             If any {@link Exception} occurred when create bean instance
     */
    protected Object createBeanInstance(final String name, final BeanDefinition def) throws Throwable {
        if (def.isSingleton()) {
            Object bean = getSingleton(name);
            if (bean == null) {
                registerSingleton(name, bean = ClassUtils.newInstance(def, this));
            }
            return bean;
        }
        else {
            return ClassUtils.newInstance(def, this);
        }
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
    protected void applyPropertyValues(final Object bean, final PropertyValue[] propertyValues)
            throws IllegalAccessException //
    {
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
                        throw new NoSuchBeanDefinitionException(beanReference.getName(), beanReference.getReferenceClass());
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
     * @return A {@link PropertyValue} bean or a proxy
     */
    protected Object resolvePropertyValue(final BeanReference beanReference) {

        final Class<?> type = beanReference.getReferenceClass();
        final String name = beanReference.getName();

        if (fullPrototype && beanReference.isPrototype() && containsBeanDefinition(name)) {
            return Prototypes.newProxyInstance(type, getBeanDefinition(name), this);
        }
        final Object bean = getBean(name, type);
        if (bean == null) {
            return doGetBeanforType(type);
        }
        return bean;
    }

    /**
     * The helper class achieve the effect of the prototype
     * 
     * @author TODAY <br>
     *         2019-09-03 21:20
     */
    public static final class Prototypes {

        private final String name;
        private final BeanDefinition def;
        private final AbstractBeanFactory f;

        private Prototypes(AbstractBeanFactory f, BeanDefinition def) {
            this.f = f;
            this.def = def;
            this.name = def.getName();
        }

        private final Object handle(final Method m, final Object[] a) throws Throwable {
            final Object b = f.getBean(name, def);
            try {
                return m.invoke(b, a);
            }
            catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            finally {
                if (f.fullLifecycle) {
                    f.destroyBean(b, def); // destroyBean after every call
                }
            }
        }

        public static Object newProxyInstance(Class<?> refType, BeanDefinition def, AbstractBeanFactory f) {

            final Prototypes handler = new Prototypes(f, def);

            if (refType.isInterface()) { // Use Jdk Proxy
                // @off
                return Proxy.newProxyInstance(refType.getClassLoader(),  def.getBeanClass().getInterfaces(), 
                    (final Object p, final Method m, final Object[] a) -> {
                        return handler.handle(m, a);
                    }
                ); //@on
            }

            return new Enhancer()
                    .setUseCache(true)
                    .setSuperclass(refType)
                    .setInterfaces(refType.getInterfaces())
                    .setClassLoader(refType.getClassLoader())
                    .setCallback((MethodInterceptor) (obj, m, a, proxy) -> handler.handle(m, a))
                    .create();
        }
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
            //method.setAccessible(true); // fix: can not access a member
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

            final FactoryBean<?> factoryBean = getFactoryBean(def, name);
            initializingBean(factoryBean, name, def);
            final Object ret = factoryBean.getBean();
            def.setInitialized(true); // $name bean initialized
            registerSingleton(name, ret);
            return ret;
        }

        return getImplementation(name, def);
    }

    /**
     * Create singleton bean.
     * 
     * @param def
     *            Current {@link BeanDefinition}
     * @throws Throwable
     *             If any {@link Exception} occurred when initialize singleton
     */
    protected void initializeSingleton(final BeanDefinition def) throws Throwable {

        if (def.isSingleton() && !def.isInitialized()) {
            final String name = def.getName();
            if (def.isFactoryBean()) {

                final FactoryBean<?> factoryBean = getFactoryBean(def, name);
                initializingBean(factoryBean, name, def);
                final Object bean = factoryBean.getBean();
                log.debug("Initialize FactoryBean: [{}]", name);
                registerSingleton(name, bean);
                def.setInitialized(true);
            }
            else {
                getImplementation(name, def);
            }
        }
    }

    protected FactoryBean<?> getFactoryBean(final BeanDefinition def, final String name) throws Throwable {
        return def instanceof FactoryBeanDefinition
                ? ((FactoryBeanDefinition<?>) def).getFactory()
                : (FactoryBean<?>) (def.isSingleton()
                        ? initializingBean(getSingleton(FACTORY_BEAN_PREFIX.concat(name)), name, def)
                        : ClassUtils.newInstance(def, this));
    }

    /**
     * Get current {@link BeanDefinition} implementation invoke this method requires
     * that input {@link BeanDefinition} is not initialized, Otherwise the bean will
     * be initialized multiple times
     * 
     * @param beanName
     *            Bean name
     * @param currentDef
     *            Bean definition
     * @return Current {@link BeanDefinition} implementation
     * @throws Throwable
     *             If any {@link Exception} occurred when get current
     *             {@link BeanDefinition} implementation
     */
    protected Object getImplementation(final String beanName, final BeanDefinition currentDef) throws Throwable {

        final String childName = currentDef.getChildBean();
        if (childName == null) {
            return initializeSingleton(beanName, currentDef);
        }

        // If contains its bean instance
        Object bean = getSingleton(beanName);

        if (bean == null) {
            bean = initializeSingleton(childName, getBeanDefinition(childName)); // abstract
            registerSingleton(beanName, bean);
            currentDef.setInitialized(true);
            return bean;
        }

        // contains its bean instance, and direct registration
        // ------------------------------------------------------

        // apply this bean definition's 'initialized' property
        currentDef.setInitialized(true);

        if (!containsSingleton(childName)) {
            registerSingleton(childName, bean); // direct register child bean
            getBeanDefinition(childName).setInitialized(true);
        }
        return bean;
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
     *             If any {@link Throwable} occurred when initialize singleton
     */
    protected Object initializeSingleton(final String name, final BeanDefinition beanDefinition) throws Throwable {

        if (beanDefinition.isInitialized()) { // fix #7
            return getSingleton(name);
        }

        Object bean = initializingBean(createBeanInstance(name, beanDefinition), name, beanDefinition);

//        registerSingleton(name, bean);
        beanDefinition.setInitialized(true);
        return bean;
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

    // handleDependency
    // ---------------------------------------

    /**
     * Handle abstract dependencies
     */
    public void handleDependency() {

        for (final PropertyValue propertyValue : getDependencies()) {

            final Class<?> propertyType = propertyValue.getField().getType();

            // Abstract
            if (!Modifier.isAbstract(propertyType.getModifiers())) {
                continue;
            }

            final BeanReference ref = (BeanReference) propertyValue.getValue();
            final String beanName = ref.getName();

            // fix: #2 when handle dependency some bean definition has already exist
            if (containsBeanDefinition(beanName)) {
                continue;
            }

            // handle dependency which is interface and parent object
            // --------------------------------------------------------

            // find child beans
            final List<BeanDefinition> childDefs = doGetChildDefinition(beanName, propertyType);

            if (!childDefs.isEmpty()) {

                BeanDefinition childDef = null;

                if (childDefs.size() > 1) {
                    // size > 1
                    OrderUtils.reversedSort(childDefs); // sort
                    for (final BeanDefinition def : childDefs) {
                        if (def.isAnnotationPresent(Primary.class)) {
                            childDef = def;
                            break;
                        }
                    }
                }

                if (childDef == null) {
                    childDef = childDefs.get(0); // first one
                }
                log.debug("Found The Implementation Of [{}] Bean: [{}].", beanName, childDef.getName());

                registerBeanDefinition(beanName, new DefaultBeanDefinition(beanName, childDef));
            }
            else if (ref.isRequired()) {
                throw new ConfigurationException("Context does not exist for this type:[" + propertyType + "] of bean");
            }
        }
    }

    /**
     * Get child {@link BeanDefinition}s
     * 
     * @param beanName
     *            Bean name
     * @param beanClass
     *            Bean class
     * @return A list of {@link BeanDefinition}s, Never be null
     */
    protected List<BeanDefinition> doGetChildDefinition(final String beanName, final Class<?> beanClass) {

        final Set<BeanDefinition> ret = new HashSet<>();

        for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
            final BeanDefinition childDef = entry.getValue();
            final Class<?> clazz = childDef.getBeanClass();

            if (beanClass != clazz
                && beanClass.isAssignableFrom(clazz)
                && !beanName.equals(childDef.getName())) {

                ret.add(childDef); // is beanClass's Child Bean
            }
        }

        if (ret.isEmpty()) { // If user registered BeanDefinition
            final BeanDefinition handleBeanDef = handleDependency(beanName, beanClass);
            if (handleBeanDef != null) {
                ret.add(handleBeanDef);
            }
        }
        return ret.isEmpty() ? Collections.emptyList() : new ArrayList<>(ret);
    }

    /**
     * Handle dependency {@link BeanDefinition}
     * 
     * @param beanName
     *            bean name
     * @param beanClass
     *            bean class
     * @return Dependency {@link BeanDefinition}
     */
    protected BeanDefinition handleDependency(final String beanName, final Class<?> beanClass) {

        final Object obj = createDependencyInstance(beanClass);
        if (obj != null) {
            registerSingleton(beanName, obj);
            return new DefaultBeanDefinition(beanName, beanClass);
        }
        return null;
    }

    /**
     * Create dependency object
     * 
     * @param type
     *            dependency type
     * @return Dependency object
     */
    protected Object createDependencyInstance(final Class<?> type) {

        final Map<Class<?>, Object> objectFactories = getObjectFactories();
        if (objectFactories != null) {
            return createDependencyInstance(type, objectFactories.get(type));
        }
        return null;
    }

    /**
     * Create dependency object
     * 
     * @param type
     *            dependency type
     * @param objectFactory
     *            Object factory
     * @return Dependency object
     */
    protected Object createDependencyInstance(final Class<?> type, final Object objectFactory) {
        if (objectFactory instanceof ObjectFactory) {
            return Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type },
                                          new ObjectFactoryDelegatingHandler((ObjectFactory<?>) objectFactory));
        }
        if (type.isInstance(objectFactory)) {
            return objectFactory;
        }
        return null;
    }

    /**
     * Get {@link ObjectFactory}s
     * 
     * @since 2.3.7
     * @return {@link ObjectFactory}s
     */
    public final Map<Class<?>, Object> getObjectFactories() {
        final Map<Class<?>, Object> objectFactories = this.objectFactories;
        if (objectFactories == null) {
            return this.objectFactories = createObjectFactories();
        }
        return objectFactories;
    }

    protected Map<Class<?>, Object> createObjectFactories() {
        return null;
    }

    public void setObjectFactories(Map<Class<?>, Object> objectFactories) {
        this.objectFactories = objectFactories;
    }

    /**
     * Reflective InvocationHandler for lazy access to the current target object.
     */
    @SuppressWarnings("serial")
    private static class ObjectFactoryDelegatingHandler implements InvocationHandler, Serializable {

        private final ObjectFactory<?> objectFactory;

        public ObjectFactoryDelegatingHandler(ObjectFactory<?> objectFactory) {
            this.objectFactory = objectFactory;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

            try {
                return method.invoke(objectFactory.getObject(), args);
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }

    // ---------------------------------------

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

        if (currentInitializingBeanName.contains(name)) {
            return bean;
        }
        currentInitializingBeanName.add(name);
        log.debug("Initializing bean named: [{}].", name);

        aware(bean, name);

        final List<BeanPostProcessor> postProcessors = getPostProcessors();
        if (postProcessors.isEmpty()) {
            // apply properties
            applyPropertyValues(bean, def.getPropertyValues());
            // invoke initialize methods
            invokeInitMethods(bean, def.getInitMethods());
            currentInitializingBeanName.remove(name);
            return bean;
        }
        final Object initWithPostProcessors = initWithPostProcessors(bean, name, def, postProcessors);
        currentInitializingBeanName.remove(name);
        return initWithPostProcessors;
    }

    /**
     * Initialize with {@link BeanPostProcessor}s
     * 
     * @param bean
     *            Bean instance
     * @param name
     *            Bean name
     * @param def
     *            Current {@link BeanDefinition}
     * @param processors
     *            {@link BeanPostProcessor}s
     * @return Initialized bean
     * @throws Exception
     *             If any {@link Exception} occurred when initialize with processors
     */
    @SuppressWarnings("deprecation")
    private Object initWithPostProcessors(Object bean, final String name, final BeanDefinition def, //
                                          final List<BeanPostProcessor> processors) throws Exception //
    {
        // before properties
        for (final BeanPostProcessor postProcessor : processors) {
            bean = postProcessor.postProcessBeforeInitialization(bean, def);
        }
        // apply properties
        applyPropertyValues(bean, def.getPropertyValues());
        // invoke initialize methods
        invokeInitMethods(bean, def.getInitMethods());
        // after properties
        for (final BeanPostProcessor processor : processors) {
            bean = processor.postProcessAfterInitialization(processor.postProcessAfterInitialization(bean, name), def);
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
    public final void aware(final Object bean, final String name) {
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
        return getBeanDefinitions()
                .entrySet()
                .stream()
                .filter(entry -> type.isAssignableFrom(entry.getValue().getBeanClass()))
                .map(entry -> entry.getKey())
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
        String nameToUse = name;
        if (bean instanceof FactoryBean && name.charAt(0) != FACTORY_BEAN_PREFIX_CHAR) {// @since v2.1.1
            nameToUse = FACTORY_BEAN_PREFIX.concat(name);
        }
        singletons.put(nameToUse, bean);
        if (log.isDebugEnabled()) {
            log.debug("Register Singleton: [{}] = [{}]", nameToUse, bean);
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
    public void registerBeanDefinition(final String beanName, final BeanDefinition beanDefinition) {

        this.beanDefinitionMap.put(beanName, beanDefinition);

        final PropertyValue[] propertyValues = beanDefinition.getPropertyValues();
        if (ObjectUtils.isNotEmpty(propertyValues)) {
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
            for (final BeanPostProcessor postProcessor : getPostProcessors()) {
                if (postProcessor instanceof DestructionBeanPostProcessor) {
                    final DestructionBeanPostProcessor destruction = (DestructionBeanPostProcessor) postProcessor;
                    if (destruction.requiresDestruction(beanInstance)) {
                        destruction.postProcessBeforeDestruction(beanInstance, def.getName());
                    }
                }
            }
            ContextUtils.destroyBean(beanInstance, beanClass.getDeclaredMethods());
        }
        catch (Throwable e) {
            e = ExceptionUtils.unwrapThrowable(e);
            throw new ContextException("An Exception Occurred When Destroy a bean: [" + def.getName() + "], With Msg: [" + e + "]", e);
        }
    }

    @Override
    public void destroyBean(String name) {

        BeanDefinition beanDefinition = getBeanDefinition(name);

        if (beanDefinition == null && name.charAt(0) == FACTORY_BEAN_PREFIX_CHAR) {
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

        final BeanDefinition beanDefinition = getBeanDefinition(getBeanNameCreator().create(beanClass));
        if (beanDefinition != null && beanClass.isAssignableFrom(beanDefinition.getBeanClass())) {
            return beanDefinition;
        }
        for (final BeanDefinition definition : getBeanDefinitions().values()) {
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

        final Predicate<BeanDefinition> predicate = getPredicate(type, equals);
        final BeanDefinition def = getBeanDefinition(getBeanNameCreator().create(type));
        if (def != null && predicate.test(def)) {
            return true;
        }

        for (final BeanDefinition beanDef : getBeanDefinitions().values()) {
            if (predicate.test(beanDef)) {
                return true;
            }
        }
        return false;
    }

    private final Predicate<BeanDefinition> getPredicate(final Class<?> type, final boolean equals) {
        if (equals) {
            return (beanDef) -> type == beanDef.getBeanClass();
        }
        return (beanDef) -> type.isAssignableFrom(beanDef.getBeanClass());
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

        for (final Entry<String, Object> entry : new HashMap<>(getSingletons()).entrySet()) {

            final String name = entry.getKey();
            final BeanDefinition beanDefinition = getBeanDefinition(name);
            if (beanDefinition == null || beanDefinition.isInitialized()) {
                continue;
            }
            registerSingleton(name, initializingBean(entry.getValue(), name, beanDefinition));
            log.debug("Pre initialize singleton bean is being stored in the name of [{}].", name);

            beanDefinition.setInitialized(true);
        }
    }

    // -----------------------------------------------------
    @Override
    public void refresh(String name) {

        final BeanDefinition def = getBeanDefinition(name);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(name);
        }

        try {

            if (def.isInitialized()) {
                log.warn("A bean named: [{}] has already initialized", name);
                return;
            }

            initializingBean(createBeanInstance(name, def), name, def);

            def.setInitialized(true);
        }
        catch (Throwable ex) {
            throw new ContextException(ex);
        }
    }

    @Override
    public Object refresh(BeanDefinition def) {
        try {
            final Object initializingBean = initializingBean(createBeanInstance(def), def.getName(), def);
            def.setInitialized(true);
            return initializingBean;
        }
        catch (Throwable ex) {
            throw new ContextException(ex);
        }
    }

    // -----------------------------

    public abstract BeanDefinitionLoader getBeanDefinitionLoader();

    /**
     * Get a bean name creator
     * 
     * @return {@link BeanNameCreator}
     */
    public BeanNameCreator getBeanNameCreator() {
        final BeanNameCreator beanNameCreator = this.beanNameCreator;
        if (beanNameCreator == null) {
            return this.beanNameCreator = createBeanNameCreator();
        }
        return beanNameCreator;
    }

    /**
     * create {@link BeanNameCreator}
     * 
     * @return a default {@link BeanNameCreator}
     */
    protected BeanNameCreator createBeanNameCreator() {
        return new DefaultBeanNameCreator(true);
    }

    public List<BeanPostProcessor> getPostProcessors() {
        return postProcessors;
    }

    @Override
    public final void enableFullPrototype() {
        setFullPrototype(true);
    }

    @Override
    public final void enableFullLifecycle() {
        setFullLifecycle(true);
    }

    public final boolean isFullPrototype() {
        return fullPrototype;
    }

    public final boolean isFullLifecycle() {
        return fullLifecycle;
    }

    public final void setFullPrototype(boolean fullPrototype) {
        this.fullPrototype = fullPrototype;
    }

    public final void setFullLifecycle(boolean fullLifecycle) {
        this.fullLifecycle = fullLifecycle;
    }

    public final void setBeanNameCreator(BeanNameCreator beanNameCreator) {
        this.beanNameCreator = beanNameCreator;
    }
}
