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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.factory;

import static cn.taketoday.context.utils.ClassUtils.getAnnotationAttributes;
import static cn.taketoday.context.utils.ClassUtils.getAnnotationAttributesArray;
import static cn.taketoday.context.utils.ContextUtils.findNames;
import static cn.taketoday.context.utils.ContextUtils.resolveInitMethod;
import static cn.taketoday.context.utils.ContextUtils.resolveParameter;
import static java.util.Objects.requireNonNull;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.aware.ImportAware;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.bean.FactoryBeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.bean.StandardBeanDefinition;
import cn.taketoday.context.env.DefaultBeanNameCreator;
import cn.taketoday.context.event.LoadingMissingBeanEvent;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.loader.BeanDefinitionImporter;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.loader.CandidateComponentScanner;
import cn.taketoday.context.loader.ImportSelector;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * Standard {@link BeanFactory} implementation
 * 
 * @author TODAY <br>
 *         2019-03-23 15:00
 */
public class StandardBeanFactory extends AbstractBeanFactory implements ConfigurableBeanFactory, BeanDefinitionLoader {

    private static final Logger log = LoggerFactory.getLogger(StandardBeanFactory.class);

    private final ConfigurableApplicationContext applicationContext;
    private final HashSet<Method> missingMethods = new HashSet<>(32);
    private final LinkedList<AnnotatedElement> componentScanned = new LinkedList<>();

    public StandardBeanFactory(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void awareInternal(Object bean, String name) {
        super.awareInternal(bean, name);

        if (bean instanceof ApplicationContextAware) {
            ((ApplicationContextAware) bean).setApplicationContext(getApplicationContext());
        }

        if (bean instanceof EnvironmentAware) {
            ((EnvironmentAware) bean).setEnvironment(getApplicationContext().getEnvironment());
        }
    }

    /**
     * If {@link BeanDefinition} is {@link StandardBeanDefinition} will create bean
     * from {@link StandardBeanDefinition#getFactoryMethod()}
     */
    @Override
    protected Object createBeanInstance(String name, BeanDefinition def) throws Exception {

        if (def instanceof StandardBeanDefinition) {
            final StandardBeanDefinition stdDef = (StandardBeanDefinition) def;

            if (def.isSingleton()) {
                Object bean = getSingleton(name);
                if (bean == null) {
                    registerSingleton(name, bean = createFromFactoryMethod(stdDef.getFactoryMethod(), stdDef));
                }
                return bean;
            }
            else {
                return createFromFactoryMethod(stdDef.getFactoryMethod(), stdDef);
            }
        }
        else {
            return super.createBeanInstance(name, def);
        }
    }

    protected Object createFromFactoryMethod(final Method factoryMethod,
                                             final StandardBeanDefinition stdDef) throws Exception {

        return ClassUtils.makeAccessible(factoryMethod)
                .invoke(getDeclaringInstance(stdDef.getDeclaringName()), resolveParameter(factoryMethod, this));
    }

    @Override
    protected Object getImplementation(String currentBeanName, BeanDefinition currentBeanDefinition) throws Throwable {
        // fix: #3 when get annotated beans that StandardBeanDefinition missed
        if (currentBeanDefinition instanceof StandardBeanDefinition) {
            return initializeSingleton(currentBeanName, currentBeanDefinition);
        }
        return super.getImplementation(currentBeanName, currentBeanDefinition);
    }

    // -----------------------------------------

    /**
     * Get declaring instance
     * 
     * @param declaringName
     *            declaring name
     * @return
     * @throws Throwable
     */
    protected Object getDeclaringInstance(String declaringName) throws Exception {
        final BeanDefinition declaringBeanDef = getBeanDefinition(declaringName);

        if (declaringBeanDef == null) {
            throw new NoSuchBeanDefinitionException(declaringName);
        }

        if (declaringBeanDef.isInitialized()) {
            return getSingleton(declaringName);
        }

        // fix: declaring bean not initialized
        final Object declaringSingleton = //
                super.initializingBean(createBeanInstance(declaringName, declaringBeanDef), declaringName, declaringBeanDef);

        // put declaring object
        if (declaringBeanDef.isSingleton()) {
//            registerSingleton(declaringName, declaringSingleton);
            declaringBeanDef.setInitialized(true);
        }
        return declaringSingleton;
    }

    /**
     * Resolve bean from a class which annotated with @{@link Configuration}
     */
    public void loadConfigurationBeans() {

        log.debug("Loading Configuration Beans");

        for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
            if (entry.getValue().isAnnotationPresent(Configuration.class)) {
                // @Configuration bean
                loadConfigurationBeans(entry.getValue().getBeanClass());
            }
        }
    }

    /**
     * Load {@link Configuration} beans from input bean class
     * 
     * @param beanClass
     *            current {@link Configuration} bean
     * @since 2.1.7
     */
    protected void loadConfigurationBeans(final Class<?> beanClass) {

        final Collection<Method> missingMethods = this.missingMethods;

        for (final Method method : beanClass.getDeclaredMethods()) {

            final AnnotationAttributes[] components = getAnnotationAttributesArray(method, Component.class);
            if (ObjectUtils.isEmpty(components)) {
                if (method.isAnnotationPresent(MissingBean.class) && ContextUtils.conditional(method)) {
                    missingMethods.add(method);
                }
            }
            else if (ContextUtils.conditional(method)) { // pass the condition
                registerConfigurationBean(method, components);
            }
        }
    }

    /**
     * Create {@link Configuration} bean definition, and register it
     *
     * @param method
     *            factory method
     * @param components
     *            {@link AnnotationAttributes}
     */
    protected void registerConfigurationBean(final Method method, final AnnotationAttributes[] components)
            throws BeanDefinitionStoreException //
    {
        final Class<?> returnType = method.getReturnType();
        final BeanNameCreator beanNameCreator = getBeanNameCreator();

        final ConfigurableApplicationContext applicationContext = getApplicationContext();

        //final String defaultBeanName = beanNameCreator.create(returnType); // @Deprecated in v2.1.7, use method name instead
        final String defaultBeanName = method.getName(); // @since v2.1.7
        final String declaringBeanName = beanNameCreator.create(method.getDeclaringClass());

        for (final AnnotationAttributes component : components) {
            final Scope scope = component.getEnum(Constant.SCOPE);
            final String[] initMethods = component.getStringArray(Constant.INIT_METHODS);
            final String[] destroyMethods = component.getStringArray(Constant.DESTROY_METHODS);

            for (final String name : findNames(defaultBeanName, component.getStringArray(Constant.VALUE))) {

                // register
                final StandardBeanDefinition def = new StandardBeanDefinition(name, returnType);

                def.setScope(scope);
                def.setDestroyMethods(destroyMethods);
                def.setInitMethods(resolveInitMethod(initMethods, returnType));
                def.setPropertyValues(ContextUtils.resolvePropertyValue(returnType));

                def.setDeclaringName(declaringBeanName)//
                        .setFactoryMethod(method);
                // resolve @Props on a bean

                ContextUtils.resolveProps(def, applicationContext.getEnvironment());

                register(name, def);
            }
        }
    }

    /**
     * Load missing beans, default beans
     * 
     * @param beanClasses
     *            Class set
     */
    public void loadMissingBean(final Collection<Class<?>> beanClasses) {

        log.debug("Loading lost beans");

        final ConfigurableApplicationContext context = getApplicationContext();
        context.publishEvent(new LoadingMissingBeanEvent(context, beanClasses));

        for (final Class<?> beanClass : beanClasses) {

            final MissingBean missingBean = beanClass.getAnnotation(MissingBean.class);

            if (ContextUtils.isMissedBean(missingBean, beanClass, this)) {
                registerMissingBean(missingBean, new DefaultBeanDefinition(getBeanName(missingBean, beanClass), beanClass));
            }
        }

        final BeanNameCreator beanNameCreator = getBeanNameCreator();

        for (final Method method : missingMethods) {

            final Class<?> beanClass = method.getReturnType();
            final MissingBean missingBean = method.getAnnotation(MissingBean.class);

            if (ContextUtils.isMissedBean(missingBean, beanClass, this)) {

                // @Configuration use default bean name
                StandardBeanDefinition beanDefinition = //
                        new StandardBeanDefinition(getBeanName(missingBean, beanClass), beanClass)//
                                .setFactoryMethod(method)//
                                .setDeclaringName(beanNameCreator.create(method.getDeclaringClass()));

                if (method.isAnnotationPresent(Props.class)) {
                    // @Props on method
                    final List<PropertyValue> resolvedProps = //
                            ContextUtils.resolveProps(method, context.getEnvironment().getProperties());

                    beanDefinition.addPropertyValue(resolvedProps);
                }
                registerMissingBean(missingBean, beanDefinition);
            }
        }
        missingMethods.clear();
    }

    /**
     * Register {@link MissingBean}
     * 
     * @param missingBean
     *            {@link MissingBean} metadata
     * @param beanDefinition
     *            Target {@link BeanDefinition}
     */
    protected void registerMissingBean(final MissingBean missingBean, final BeanDefinition beanDefinition) {

        final Class<?> beanClass = beanDefinition.getBeanClass();

        beanDefinition.setScope(missingBean.scope())//
                .setDestroyMethods(missingBean.destroyMethods())//
                .setInitMethods(resolveInitMethod(missingBean.initMethods(), beanClass))//
                .setPropertyValues(ContextUtils.resolvePropertyValue(beanClass));

        ContextUtils.resolveProps(beanDefinition, getApplicationContext().getEnvironment());

        // register missed bean
        register(beanDefinition.getName(), beanDefinition);
    }

    /**
     * Get bean name
     * 
     * @param missingBean
     *            {@link MissingBean}
     * @param beanClass
     *            Bean class
     * @return Bean name
     */
    protected String getBeanName(final MissingBean missingBean, final Class<?> beanClass) {
        String beanName = missingBean.value();
        if (StringUtils.isEmpty(beanName)) {
            beanName = getBeanNameCreator().create(beanClass);
        }
        return beanName;
    }

    /**
     * Resolve bean from META-INF/beans
     * 
     * @since 2.1.6
     */
    public Set<Class<?>> loadMetaInfoBeans() {

        log.debug("Loading META-INF/beans");

        // Load the META-INF/beans @since 2.1.6
        // ---------------------------------------------------

        final Set<Class<?>> beans = ContextUtils.loadFromMetaInfo("META-INF/beans");

        final BeanNameCreator beanNameCreator = getBeanNameCreator();
        for (final Class<?> beanClass : beans) {

            if (ContextUtils.conditional(beanClass) && !beanClass.isAnnotationPresent(MissingBean.class)) {
                // can't be a missed bean. MissingBean load after normal loading beans
                ContextUtils.buildBeanDefinitions(beanClass, beanNameCreator.create(beanClass))
                        .forEach(this::register);
            }
        }
        return beans;
    }

    /**
     * Load {@link Import} beans from input bean classes
     * 
     * @param beans
     *            Input bean classes
     * @since 2.1.7
     */
    public void loadImportBeans(Class<?>... beans) {
        for (final Class<?> bean : requireNonNull(beans)) {
            loadImportBeans(createBeanDefinition(bean));
        }
    }

    /**
     * Load {@link Import} beans from input {@link BeanDefinition}s
     * 
     * @param defs
     *            Input {@link BeanDefinition}s
     * @since 2.1.7
     */
    public void loadImportBeans(final Set<BeanDefinition> defs) {

        for (final BeanDefinition def : defs) {
            loadImportBeans(def);
        }
    }

    /**
     * Load {@link Import} beans from input {@link BeanDefinition}
     * 
     * @param def
     *            Input {@link BeanDefinition}
     * @since 2.1.7
     */
    public void loadImportBeans(final BeanDefinition def) {

        for (final AnnotationAttributes attr : getAnnotationAttributesArray(def, Import.class)) {
            for (final Class<?> importClass : attr.getAttribute(Constant.VALUE, Class[].class)) {
                if (!containsBeanDefinition(importClass, true)) {
                    selectImport(def, importClass);
                }
            }
        }
    }

    /**
     * Select import
     * 
     * @since 2.1.7
     */
    protected void selectImport(final BeanDefinition def, final Class<?> importClass) {

        log.debug("Importing: [{}]", importClass);

        BeanDefinition importDef = createBeanDefinition(importClass);

        register(importDef);

        loadConfigurationBeans(importClass); // scan config bean

        if (ImportSelector.class.isAssignableFrom(importClass)) {
            for (final String select : createImporter(importDef, ImportSelector.class).selectImports(def)) {
                register(createBeanDefinition(ClassUtils.loadClass(select)));
            }
        }
        if (BeanDefinitionImporter.class.isAssignableFrom(importClass)) {
            createImporter(importDef, BeanDefinitionImporter.class).registerBeanDefinitions(def, this);
        }
        if (ApplicationListener.class.isAssignableFrom(importClass)) {
            getApplicationContext().addApplicationListener(createImporter(importDef, ApplicationListener.class));
        }
    }

    /**
     * Create {@link ImportSelector} ,or {@link BeanDefinitionImporter},
     * {@link ApplicationListener} object
     * 
     * @param target
     *            Must be {@link ImportSelector} ,or {@link BeanDefinitionImporter}
     * @return {@link ImportSelector} object
     */
    protected <T> T createImporter(BeanDefinition importDef, Class<T> target) {
        try {
            final Object bean = getBean(importDef);
            if (bean instanceof ImportAware) {
                ((ImportAware) bean).setImportBeanDefinition(importDef);
            }
            return target.cast(bean);
        }
        catch (Throwable e) {
            throw new BeanDefinitionStoreException("Can't initialize a target: [" + importDef + "]");
        }
    }

    @Override
    protected BeanNameCreator createBeanNameCreator() {
        return new DefaultBeanNameCreator(getApplicationContext().getEnvironment());
    }

    @Override
    public final BeanDefinitionLoader getBeanDefinitionLoader() {
        return this;
    }

    // BeanDefinitionLoader @since 2.1.7
    // ---------------------------------------------

    @Override
    public void loadBeanDefinition(final Class<?> candidate) throws BeanDefinitionStoreException {

        // don't load abstract class
        if (!Modifier.isAbstract(candidate.getModifiers()) && ContextUtils.conditional(candidate)) {
            register(candidate);
        }
    }

    @Override
    public void loadBeanDefinitions(final Collection<Class<?>> beans) throws BeanDefinitionStoreException {
        for (Class<?> clazz : beans) {
            loadBeanDefinition(clazz);
        }
    }

    @Override
    public void loadBeanDefinition(final String name, final Class<?> beanClass) throws BeanDefinitionStoreException {

        final AnnotationAttributes[] annotationAttributes = getAnnotationAttributesArray(beanClass, Component.class);

        if (ObjectUtils.isEmpty(annotationAttributes)) {
            register(name, build(beanClass, null, name));
        }
        else {
            for (final AnnotationAttributes attributes : annotationAttributes) {
                register(name, build(beanClass, attributes, name));
            }
        }
    }

    @Override
    public void loadBeanDefinition(final String... locations) throws BeanDefinitionStoreException {
        loadBeanDefinitions(new CandidateComponentScanner().scan(locations));
    }

    @Override
    public void register(final Class<?> candidate) throws BeanDefinitionStoreException {

        final AnnotationAttributes[] annotationAttributes = getAnnotationAttributesArray(candidate, Component.class);

        if (ObjectUtils.isNotEmpty(annotationAttributes)) {

            final String defaultBeanName = getBeanNameCreator().create(candidate);
            for (final AnnotationAttributes attributes : annotationAttributes) {
                for (final String name : findNames(defaultBeanName, attributes.getStringArray(Constant.VALUE))) {
                    register(name, build(candidate, attributes, name));
                }
            }
        }
    }

    /**
     * Build a bean definition
     * 
     * @param beanClass
     *            Given bean class
     * @param attributes
     *            {@link AnnotationAttributes}
     * @param beanName
     *            Bean name
     * @return A default {@link BeanDefinition}
     * @throws Throwable
     *             If any {@link Exception} occurred
     */
    protected BeanDefinition build(final Class<?> beanClass, final AnnotationAttributes attributes, final String beanName) {
        return ContextUtils.buildBeanDefinition(beanClass, attributes, beanName);
    }

    /**
     * Register bean definition with given name
     * 
     * @param name
     *            Bean name
     * @param beanDefinition
     *            Bean definition
     * @throws BeanDefinitionStoreException
     *             If can't store bean
     */
    @Override
    public void register(final String name, final BeanDefinition beanDefinition) throws BeanDefinitionStoreException {

        ContextUtils.validateBeanDefinition(beanDefinition);

        String nameToUse = name;
        final Class<?> beanClass = beanDefinition.getBeanClass();

        try {
            if (containsBeanDefinition(name)) {
                final BeanDefinition existBeanDefinition = getBeanDefinition(name);
                Class<?> existClass = existBeanDefinition.getBeanClass();
                log.info("=====================|START|=====================");
                log.info("There is already a bean called: [{}], its bean class: [{}].", name, existClass);

                if (beanClass.equals(existClass)) {
                    log.warn("They have same bean class: [{}]. We will override it.", beanClass);
                }
                else {
                    nameToUse = beanClass.getName();
                    log.warn("Current bean class: [{}]. You are supposed to change your bean name creater or bean name.", beanClass);
                    log.warn("Current bean definition: [{}] will be registed as: [{}].", beanDefinition, nameToUse);
                }
                log.info("======================|END|======================");
            }

            if (FactoryBean.class.isAssignableFrom(beanClass)) { // process FactoryBean
                registerFactoryBean(nameToUse, beanDefinition);
            }
            else {
                registerBeanDefinition(nameToUse, beanDefinition);
            }

            postProcessRegisterBeanDefinition(beanDefinition);
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            throw new BeanDefinitionStoreException("An Exception Occurred When Register Bean Definition: [" + //
                    name + "], With Msg: [" + ex + "]", ex);
        }
    }

    /**
     * Process after register {@link BeanDefinition}
     * 
     * @param beanDefinition
     *            Target {@link BeanDefinition}
     */
    protected void postProcessRegisterBeanDefinition(final BeanDefinition beanDefinition) {
        if (beanDefinition.isAnnotationPresent(Import.class)) { // @since 2.1.7
            loadImportBeans(beanDefinition);
        }
        if (beanDefinition.isAnnotationPresent(ComponentScan.class)) {
            componentScan(beanDefinition);
        }
    }

    /**
     * Import beans from given package locations
     * 
     * @param source
     *            {@link BeanDefinition} that annotated {@link ComponentScan}
     */
    protected void componentScan(final AnnotatedElement source) {
        if (!componentScanned.contains(source)) {
            componentScanned.add(source);
            for (final AnnotationAttributes attribute : getAnnotationAttributesArray(source, ComponentScan.class)) {
                loadBeanDefinition(attribute.getStringArray(Constant.VALUE));
            }
        }
    }

    /**
     * Register {@link FactoryBeanDefinition} to the {@link BeanFactory}
     * 
     * @param beanName
     *            Target bean name
     * @param factoryDef
     *            {@link FactoryBean} Bean definition
     * @throws Throwable
     *             If any {@link Throwable} occurred
     */
    protected void registerFactoryBean(final String oldBeanName, final BeanDefinition factoryDef) throws Throwable {

        final FactoryBeanDefinition<?> def = factoryDef instanceof FactoryBeanDefinition
                ? (FactoryBeanDefinition<?>) factoryDef
                : factoryDef.isSingleton()
                        ? new FactoryBeanDefinition<>(factoryDef, new FactoryBeanSupplier<>(factoryDef, this))
                        : new FactoryBeanDefinition<>(factoryDef, () -> {
                            try {
                                return (FactoryBean<?>) ClassUtils.newInstance(factoryDef, this);
                            }
                            catch (ReflectiveOperationException e) {
                                throw new ConfigurationException("Cannot create a bean: [" + factoryDef + "]");
                            }
                        });

        registerBeanDefinition(oldBeanName, def);
    }

    @Override
    public BeanDefinitionRegistry getRegistry() {
        return this;
    }

    @Override
    public BeanDefinition createBeanDefinition(final Class<?> beanClass) {
        return build(beanClass,
                     getAnnotationAttributes(Component.class, beanClass),
                     getBeanNameCreator().create(beanClass));
    }

    public ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

}
