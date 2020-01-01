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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;

/**
 * Bean factory
 * 
 * @author TODAY <br>
 *         2018-06-23 11:22:26
 */
public interface BeanFactory {

    /**
     * If a bean name start with this its a {@link FactoryBean}
     */
    String FACTORY_BEAN_PREFIX = "$";

    char FACTORY_BEAN_PREFIX_CHAR = '$';

    /**
     * Find the bean with the given type
     * 
     * @param name
     *            Bean name
     * @return Bet bean instance, returns null if it doesn't exist .
     * @throws ContextException
     *             Exception Occurred When Getting A Named Bean
     */
    Object getBean(String name) throws ContextException;

    /**
     * Find the bean with the given type,
     * 
     * @param requiredType
     *            Bean type
     * @return Get safe casted bean instance. returns null if it doesn't exist .
     */
    <T> T getBean(Class<T> requiredType);

    /**
     * Find the bean with the given name and cast to required type.
     * 
     * @param name
     *            Bean name
     * @param requiredType
     *            Cast to required type
     * @return get casted bean instance. returns null if it doesn't exist.
     */
    <T> T getBean(String name, Class<T> requiredType);

    /**
     * Is Singleton ?
     * 
     * @param name
     *            Bean name
     * @return If this bean is a singleton
     * @throws NoSuchBeanDefinitionException
     *             If a bean does not exist
     */
    boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

    /**
     * Is Prototype ?
     * 
     * @param name
     *            Bean name
     * @return If this bean is a prototype
     * @throws NoSuchBeanDefinitionException
     *             If a bean does not exist
     */
    boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

    /**
     * Get bean type
     * 
     * @param name
     *            Bean name
     * @return Target bean type
     * @throws NoSuchBeanDefinitionException
     *             If a bean does not exist
     */
    Class<?> getType(String name) throws NoSuchBeanDefinitionException;

    /**
     * Get all bean name
     * 
     * @param type
     *            Bean type
     * @return A set of names with given type
     */
    Set<String> getAliases(Class<?> type);

    /**
     * Get the target class's name
     * 
     * @param beanType
     *            bean type
     * @return Get bane name
     * @since 2.1.2
     */
    String getBeanName(Class<?> beanType) throws NoSuchBeanDefinitionException;

    /**
     * Get a set of beans with given type, this method must invoke after
     * {@link ApplicationContext#loadContext(String...)}
     * 
     * @param requiredType
     *            Given bean type
     * @return A set of beans with given type, never be {@code null}
     * @since 2.1.2
     */
    <T> List<T> getBeans(Class<T> requiredType);

    /**
     * Get a list of annotated beans, this method must invoke after
     * {@link ApplicationContext#loadContext(String...)}
     * 
     * @param annotationType
     *            {@link Annotation} type
     * @return List of annotated beans, never be {@code null}
     * @since 2.1.5
     */
    <A extends Annotation, T> List<T> getAnnotatedBeans(Class<A> annotationType);

    /**
     * Get a map of beans with given type, this method must invoke after
     * {@link ApplicationContext#loadContext(String...)}
     * 
     * @param requiredType
     *            Given bean type
     * @return A map of beans with given type, never be {@code null}
     * @since 2.1.6
     */
    <T> Map<String, T> getBeansOfType(Class<T> requiredType);

    /**
     * Get all {@link BeanDefinition}s
     * 
     * @return All {@link BeanDefinition}s
     * @since 2.1.6
     */
    Map<String, BeanDefinition> getBeanDefinitions();

    /**
     * Create the bean with the given {@link BeanDefinition}
     * 
     * @param def
     *            {@link BeanDefinition}
     * @return Target {@link Object}
     * @since 2.1.7
     */
    Object getBean(BeanDefinition def);

}
