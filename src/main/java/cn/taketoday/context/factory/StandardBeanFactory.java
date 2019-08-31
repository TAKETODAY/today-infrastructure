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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.factory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.AbstractApplicationContext;
import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.bean.StandardBeanDefinition;
import cn.taketoday.context.event.LoadingMissingBeanEvent;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY <br>
 *         2019-03-23 15:00
 */
public class StandardBeanFactory extends AbstractBeanFactory implements ConfigurableBeanFactory {

    private final Collection<Method> missingMethods = new HashSet<>(32);

    private final AbstractApplicationContext applicationContext;

    /** resolve beanDefinition */
    private BeanDefinitionLoader beanDefinitionLoader;

    public StandardBeanFactory(AbstractApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void awareInternal(Object bean, String name) {
        super.awareInternal(bean, name);

        if (bean instanceof ApplicationContextAware) {
            ((ApplicationContextAware) bean).setApplicationContext(applicationContext);
        }

        if (bean instanceof EnvironmentAware) {
            ((EnvironmentAware) bean).setEnvironment(applicationContext.getEnvironment());
        }
    }

    /**
     * If {@link BeanDefinition} is {@link StandardBeanDefinition} will create bean
     * from {@link StandardBeanDefinition#getFactoryMethod()}
     */
    @Override
    protected Object createBeanInstance(BeanDefinition beanDefinition) throws Throwable {
        final Object bean = getSingleton(beanDefinition.getName());

        if (bean == null) {
            if (beanDefinition instanceof StandardBeanDefinition) {
                final StandardBeanDefinition standardBeanDefinition = (StandardBeanDefinition) beanDefinition;
                final Method factoryMethod = standardBeanDefinition.getFactoryMethod();

                return ClassUtils.makeAccessible(factoryMethod).invoke(//
                        getDeclaringInstance(standardBeanDefinition.getDeclaringName()), //
                        ContextUtils.resolveParameter(factoryMethod, this)//
                );
            }
            return ClassUtils.newInstance(beanDefinition, this);
        }
        return bean;
    }

    /**
     * 
     */
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
    protected Object getDeclaringInstance(String declaringName) throws Throwable {
        final BeanDefinition declaringBeanDefinition = getBeanDefinition(declaringName);

        if (declaringBeanDefinition.isInitialized()) {
            return getSingleton(declaringName);
        }

        // fix: declaring bean not initialized
        final Object declaringSingleton = super.initializingBean(//
                createBeanInstance(declaringBeanDefinition), declaringName, declaringBeanDefinition//
        );

        // put declaring object
        if (declaringBeanDefinition.isSingleton()) {
            registerSingleton(declaringName, declaringSingleton);
            declaringBeanDefinition.setInitialized(true);
        }
        return declaringSingleton;
    }

    /**
     * Resolve bean from a class which annotated with @{@link Configuration}
     */
    public void loadConfigurationBeans() {

        for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {

            final Class<? extends Object> beanClass = entry.getValue().getBeanClass();
            if (beanClass.isAnnotationPresent(Configuration.class)) {
                // @Configuration bean
                for (final Method method : beanClass.getDeclaredMethods()) {

                    if (ContextUtils.conditional(method)) { // pass the condition

                        final AnnotationAttributes[] components = //
                                ClassUtils.getAnnotationAttributesArray(method, Component.class);

                        if (ObjectUtils.isEmpty(components) && method.isAnnotationPresent(MissingBean.class)) {
                            missingMethods.add(method);
                        }
                        else {
                            registerConfigurationBean(method, components);
                        }
                    }
                }
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
    protected void registerConfigurationBean(final Method method, final AnnotationAttributes[] components) //
            throws BeanDefinitionStoreException //
    {

        final Class<?> returnType = method.getReturnType();
        final BeanNameCreator beanNameCreator = getBeanNameCreator();
        final BeanDefinitionLoader beanDefinitionLoader = getBeanDefinitionLoader();

//        final String defaultBeanName = beanNameCreator.create(returnType); // @Deprecated in v2.1.7, use method name instead
        final String defaultBeanName = method.getName(); // @since v2.1.7
        final String declaringBeanName = beanNameCreator.create(method.getDeclaringClass());

        for (final AnnotationAttributes component : components) {
            final Scope scope = component.getEnum(Constant.SCOPE);
            final String[] initMethods = component.getStringArray(Constant.INIT_METHODS);
            final String[] destroyMethods = component.getStringArray(Constant.DESTROY_METHODS);

            for (final String name : ContextUtils.findNames(defaultBeanName, component.getStringArray(Constant.VALUE))) {

                // register
                final StandardBeanDefinition beanDefinition = new StandardBeanDefinition(name, returnType);

                beanDefinition.setScope(scope);
                beanDefinition.setDestroyMethods(destroyMethods);
                beanDefinition.setInitMethods(ContextUtils.resolveInitMethod(returnType, initMethods));
                beanDefinition.setPropertyValues(ContextUtils.resolvePropertyValue(returnType));

                beanDefinition.setDeclaringName(declaringBeanName)//
                        .setFactoryMethod(method);
                // resolve @Props on a bean
                ContextUtils.resolveProps(beanDefinition, applicationContext.getEnvironment());

                beanDefinitionLoader.register(name, beanDefinition);
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

        applicationContext.publishEvent(new LoadingMissingBeanEvent(applicationContext, beanClasses));

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
                            ContextUtils.resolveProps(method, applicationContext.getEnvironment().getProperties());

                    beanDefinition.addPropertyValue(resolvedProps);
                }
                registerMissingBean(missingBean, beanDefinition);
            }
        }
        missingMethods.clear();
    }

    protected void registerMissingBean(final MissingBean missingBean, final BeanDefinition beanDefinition) {

        final Class<?> beanClass = beanDefinition.getBeanClass();

        beanDefinition.setScope(missingBean.scope())//
                .setDestroyMethods(missingBean.destroyMethods())//
                .setInitMethods(ContextUtils.resolveInitMethod(beanClass, missingBean.initMethods()))//
                .setPropertyValues(ContextUtils.resolvePropertyValue(beanClass));

        ContextUtils.resolveProps(beanDefinition, applicationContext.getEnvironment());

        // register missed bean
        getBeanDefinitionLoader().register(beanDefinition.getName(), beanDefinition);
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

        // Load the META-INF/beans
        // ---------------------------------------------------
        final Set<Class<?>> beans = new HashSet<>();

        try { // @since 2.1.6
            final ClassLoader classLoader = ClassUtils.getClassLoader();
            final Enumeration<URL> resources = classLoader.getResources("META-INF/beans");
            final Charset charset = Constant.DEFAULT_CHARSET;

            while (resources.hasMoreElements()) {
                try (final BufferedReader reader = new BufferedReader(//
                        new InputStreamReader(resources.nextElement().openStream(), charset))) { // fix

                    String str;
                    while ((str = reader.readLine()) != null) {
                        beans.add(classLoader.loadClass(str));
                    }
                }
            }
        }
        catch (IOException | ClassNotFoundException e) {
            LoggerFactory.getLogger(getClass()).error("Exception occurred when load 'META-INF/beans'", e);
            throw ExceptionUtils.newContextException(e);
        }

        final BeanDefinitionLoader beanDefinitionLoader = getBeanDefinitionLoader();

        final BeanNameCreator beanNameCreator = getBeanNameCreator();
        for (final Class<?> beanClass : beans) {

            if (ContextUtils.conditional(beanClass) && !beanClass.isAnnotationPresent(MissingBean.class)) {
                // can't be a missed bean

                ContextUtils.buildBeanDefinitions(beanClass, beanNameCreator.create(beanClass))//
                        .forEach(beanDefinitionLoader::register);
            }
        }
        return beans;
    }

    @Override
    public BeanDefinitionLoader getBeanDefinitionLoader() {
        if (beanDefinitionLoader == null) {
            try {
                // fix: when manually load context some properties can't be loaded
                // not initialize
                applicationContext.loadContext(Collections.emptySet());
            }
            catch (Throwable e) {
                throw ExceptionUtils.newContextException(e);
            }
        }
        return beanDefinitionLoader;
    }

    public void setBeanDefinitionLoader(BeanDefinitionLoader beanDefinitionLoader) {
        this.beanDefinitionLoader = beanDefinitionLoader;
    }

}
