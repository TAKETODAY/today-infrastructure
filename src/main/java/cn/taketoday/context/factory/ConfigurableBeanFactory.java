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

import java.util.Set;

import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.context.event.ObjectRefreshedEvent;
import cn.taketoday.context.exception.BeanDefinitionStoreException;

/**
 * @author TODAY <br>
 *         2018-11-14 19:40
 */
public interface ConfigurableBeanFactory
        extends BeanFactory, SingletonBeanRegistry, BeanDefinitionRegistry {

    /**
     * Register a bean with the given name and bean definition
     * 
     * @param beanDefinition
     *            Bean definition
     * @throws BeanDefinitionStoreException
     *             If can't store a bean
     * @since 1.2.0
     */
    void registerBean(String name, BeanDefinition beanDefinition) throws BeanDefinitionStoreException;

    /**
     * Register a bean with the given bean definition
     * 
     * @param beanDefinition
     *            Bean definition
     * @throws BeanDefinitionStoreException
     *             If can't store a bean
     * @since 2.1.7
     */
    default void registerBean(BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
        registerBean(beanDefinition.getName(), beanDefinition);
    }

    /**
     * Remove bean with the given name
     * 
     * @param name
     *            bean name
     */
    void removeBean(String name);

    /**
     * Register a bean with the given name and type
     * 
     * @param name
     *            bean name
     * @param clazz
     *            bean class
     * @throws BeanDefinitionStoreException
     *             If can't store a bean
     */
    void registerBean(String name, Class<?> clazz) throws BeanDefinitionStoreException;

    /**
     * Register a bean with the given type
     * 
     * @param clazz
     *            bean class
     * @throws BeanDefinitionStoreException
     *             If can't store a bean
     * 
     */
    void registerBean(Class<?> clazz) throws BeanDefinitionStoreException;

    /**
     * Register a bean with the given types
     * 
     * @param classes
     *            bean classes
     * @throws BeanDefinitionStoreException
     *             If can't store a bean
     */
    void registerBean(Set<Class<?>> classes) throws BeanDefinitionStoreException;

    /**
     * Destroy bean with given name
     * 
     * @param name
     *            the bean name
     * @since 2.1.0
     */
    void destroyBean(String name);

    /**
     * Refresh bean with given name, and publish {@link ObjectRefreshedEvent}.
     * 
     * @param name
     *            bean name
     * @since 1.2.0
     */
    void refresh(String name);

    /**
     * Refresh bean definition, and publish {@link ObjectRefreshedEvent}.
     * 
     * @param beanDefinition
     *            bean definition
     * @since 2.0.0
     * @return initialized object
     */
    Object refresh(BeanDefinition beanDefinition);

    /**
     * Initialize singletons
     * 
     * @throws Throwable
     *             when could not initialize singletons
     * @since 2.1.2
     */
    void initializeSingletons() throws Throwable;

    /**
     * Add a {@link BeanPostProcessor}
     * 
     * @param beanPostProcessor
     *            bean post processor instance
     * @since 2.1.2
     */
    void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

    /**
     * Remove a {@link BeanPostProcessor}
     * 
     * @param beanPostProcessor
     *            bean post processor instance
     * @since 2.1.2
     */
    void removeBeanPostProcessor(BeanPostProcessor beanPostProcessor);

    /**
     * Enable full {@link Prototype} , now {@link PropertyValue} only support
     * interface
     * 
     * @since 2.1.6
     */
    void enableFullPrototype();

    /**
     * Enable full {@link Prototype}'s life cycle, default is not support
     * 
     * @since 2.1.6
     */
    void enableFullLifecycle();

}
