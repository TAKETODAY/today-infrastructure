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
package cn.taketoday.context.loader;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Objects;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * Default Bean Definition Loader implements
 * 
 * @author TODAY <br>
 *         2018-06-23 11:18:22
 */
public class DefaultBeanDefinitionLoader implements BeanDefinitionLoader {

    /** bean definition registry */
    private final BeanDefinitionRegistry registry;
    /** bean name creator */
    private final BeanNameCreator beanNameCreator;
    private final ConfigurableApplicationContext applicationContext;

    public DefaultBeanDefinitionLoader(ConfigurableApplicationContext applicationContext) {

        this.applicationContext = //
                Objects.requireNonNull(applicationContext, "applicationContext can't be null");

        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        this.registry = environment.getBeanDefinitionRegistry();
        this.beanNameCreator = environment.getBeanNameCreator();
    }

    @Override
    public BeanDefinitionRegistry getRegistry() {
        return this.registry;
    }

    @Override
    public void loadBeanDefinition(Class<?> beanClass) throws BeanDefinitionStoreException {

        if (!Modifier.isAbstract(beanClass.getModifiers())) { // don't load abstract class
            try {

                if (ContextUtils.conditional(beanClass, applicationContext)) {
                    register(beanClass);
                }
            }
            catch (Throwable ex) {
                throw new BeanDefinitionStoreException(ExceptionUtils.unwrapThrowable(ex));
            }
        }
    }

    @Override
    public void loadBeanDefinitions(Collection<Class<?>> beans) throws BeanDefinitionStoreException {
        for (Class<?> clazz : beans) {
            loadBeanDefinition(clazz);
        }
    }

    @Override
    public void loadBeanDefinition(String name, Class<?> beanClass) throws BeanDefinitionStoreException {
        // register
        try {

            final Collection<AnnotationAttributes> annotationAttributes = //
                    ClassUtils.getAnnotationAttributes(beanClass, Component.class);
            
            if (annotationAttributes.isEmpty()) {
                register(name, build(beanClass, null, name));
            }
            else {
                for (AnnotationAttributes attributes : annotationAttributes) {
                    register(name, build(beanClass, attributes, name));
                }
            }
        }
        catch (Throwable e) {
            throw new BeanDefinitionStoreException(ExceptionUtils.unwrapThrowable(e));
        }
    }

    /**
     * Register with given class
     * 
     * @param beanClass
     *            bean class
     * @throws BeanDefinitionStoreException
     *             if {@link BeanDefinition} can't store
     */
    @Override
    public void register(Class<?> beanClass) throws BeanDefinitionStoreException {

        Collection<AnnotationAttributes> annotationAttributes = //
                ClassUtils.getAnnotationAttributes(beanClass, Component.class);

        if (annotationAttributes.isEmpty()) {
            return;
        }

        try {

            final String defaultBeanName = beanNameCreator.create(beanClass);
            for (final AnnotationAttributes attributes : annotationAttributes) {
                for (final String name : ContextUtils.findNames(defaultBeanName, attributes.getStringArray(Constant.VALUE))) {
                    register(name, build(beanClass, attributes, name));
                }
            }
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            throw new ConfigurationException(//
                    "An Exception Occurred When Build Bean Definition: [" + //
                            beanClass.getName() + "], With Msg: [" + ex.getMessage() + "]", ex);
        }
    }

    /**
     * Build a bean definition
     * 
     * @param beanClass
     *            given bean class
     * @param attributes
     *            {@link AnnotationAttributes}
     * @param beanName
     *            bean name
     * @return
     * @throws Throwable
     */
    protected BeanDefinition build(Class<?> beanClass, AnnotationAttributes attributes, String beanName) throws Throwable {

        final BeanDefinition beanDefinition = new DefaultBeanDefinition(beanName, beanClass);//

        if (attributes == null) {
            beanDefinition.setDestroyMethods(Constant.EMPTY_STRING_ARRAY)//
                    .setInitMethods(ContextUtils.resolveInitMethod(beanClass));//
        }
        else {
            beanDefinition.setScope(attributes.getEnum(Constant.SCOPE))//
                    .setDestroyMethods(attributes.getStringArray(Constant.DESTROY_METHODS))//
                    .setInitMethods(ContextUtils.resolveInitMethod(beanClass, attributes.getStringArray(Constant.INIT_METHODS)));
        }

        beanDefinition.setPropertyValues(ContextUtils.resolvePropertyValue(beanClass, this.applicationContext));
        // fix missing @Props injection
        ContextUtils.resolveProps(beanDefinition, this.applicationContext.getEnvironment());

        return beanDefinition;
    }

    /**
     * Register bean definition with given name
     * 
     * @param name
     *            bean name
     * @param beanDefinition
     *            definition
     * @throws BeanDefinitionStoreException
     */
    @Override
    public void register(String name, final BeanDefinition beanDefinition) throws BeanDefinitionStoreException {

        ContextUtils.validateBeanDefinition(beanDefinition, applicationContext);

        try {

            final Class<?> beanClass = beanDefinition.getBeanClass();

            if (applicationContext.containsBeanDefinition(name)) {
                final BeanDefinition existBeanDefinition = applicationContext.getBeanDefinition(name);
                if (beanClass.equals(existBeanDefinition.getBeanClass())) {

                    LoggerFactory.getLogger(DefaultBeanDefinitionLoader.class)//
                            .warn("There is already a bean called: [{}], its bean class: [{}]", //
                                    name, beanClass);
                }
            }

            if (FactoryBean.class.isAssignableFrom(beanClass)) { // process FactoryBean
                name = registerFactoryBean(name, beanDefinition);
            }

            registry.registerBeanDefinition(name, beanDefinition);
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            throw new BeanDefinitionStoreException("An Exception Occurred When Register Bean Definition: [" + //
                    name + "], With Msg: [" + ex.getMessage() + "]", ex);
        }
    }

    /**
     * If bean definition is a {@link FactoryBean} register its factory's instance
     * 
     * @param beanName
     *            old bean name
     * @param beanDefinition
     *            definition
     * @return returns a new bean name
     * @throws Throwable
     */
    private String registerFactoryBean(String beanName, BeanDefinition beanDefinition) throws Throwable {

        FactoryBean<?> $factoryBean = //
                (FactoryBean<?>) applicationContext.getSingleton(BeanFactory.FACTORY_BEAN_PREFIX + beanName);

        boolean registed = true;
        if ($factoryBean == null) { // If not exist declaring instance, create it
            $factoryBean = (FactoryBean<?>) ClassUtils.newInstance(beanDefinition.getBeanClass()); // declaring object
            // not initialized
            registed = false;
        }

        final Class<?> beanClass = $factoryBean.getBeanClass();

        beanName = $factoryBean.getBeanName();
        if (StringUtils.isEmpty(beanName)) {
            beanName = beanNameCreator.create(beanClass);
        }

        // fix
        beanDefinition.setFactoryBean(true)//
                .setBeanClass(beanClass)//
                .setName(beanName);

        if (!registed) {// register it
            applicationContext.registerSingleton(beanName, $factoryBean);
        }

        return beanName;
    }

    @Override
    public BeanDefinition createBeanDefinition(Class<?> beanClass) {

        final Collection<AnnotationAttributes> annotationAttributes = //
                ClassUtils.getAnnotationAttributes(beanClass, Component.class);

        try {

            if (annotationAttributes.isEmpty()) {
                return build(beanClass, null, beanNameCreator.create(beanClass));
            }
            return build(beanClass, annotationAttributes.iterator().next(), beanNameCreator.create(beanClass));
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            throw new BeanDefinitionStoreException(//
                    "An Exception Occurred When Create A Bean Definition, With Msg: [" + ex.getMessage() + "]", //
                    ex//
            );
        }
    }

}
