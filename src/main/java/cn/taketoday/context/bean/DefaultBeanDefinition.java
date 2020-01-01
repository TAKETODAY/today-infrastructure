/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright ©  TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.context.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.Scope;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchPropertyException;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;

/**
 * Default implementation of {@link BeanDefinition}
 * 
 * @author TODAY <br>
 *         2019-02-01 12:23
 */
public class DefaultBeanDefinition implements BeanDefinition, Ordered {

    /** bean name. */
    private final String name;
    /** bean class. */
    private final Class<? extends Object> beanClass;
    /** bean scope. */
    private Scope scope = Scope.SINGLETON;

    /**
     * Invoke before {@link InitializingBean#afterPropertiesSet}
     * 
     * @since 2.3.3
     */
    private Method[] initMethods = EMPTY_METHOD;

    /**
     * Invoke after when publish
     * {@link ApplicationContext#publishEvent(cn.taketoday.context.event.ContextCloseEvent)}
     * 
     * @since 2.3.3
     */
    private String[] destroyMethods = Constant.EMPTY_STRING_ARRAY;

    /** property values */
    private PropertyValue[] propertyValues = EMPTY_PROPERTY_VALUE;

    /**
     * <p>
     * This is a very important property.
     * <p>
     * If registry contains it's singleton instance, we don't know the instance has
     * initialized or not, so must be have a flag to prove it has initialized
     * 
     * @since 2.0.0
     */
    private boolean initialized = false;

    /**
     * Mark as a {@link FactoryBean}.
     * 
     * @since 2.0.0
     */
    private boolean factoryBean = false;

    /** Child implementation bean name */
    private String childName;

    public DefaultBeanDefinition(String name, Class<? extends Object> beanClass) {
        this.beanClass = beanClass;
        this.name = name;
    }

    /**
     * Build a {@link BeanDefinition} with given child {@link BeanDefinition}
     * 
     * @param beanName
     *            Bean name
     * @param childDef
     *            Child {@link BeanDefinition}
     */
    public DefaultBeanDefinition(String beanName, BeanDefinition childDef) {
        this(beanName, childDef.getBeanClass());

        setScope(childDef.getScope());
        setChildName(childDef.getName());
        setInitMethods(childDef.getInitMethods());
        setDestroyMethods(childDef.getDestroyMethods());
        setPropertyValues(childDef.getPropertyValues());
    }

    @Override
    public PropertyValue getPropertyValue(String name) throws NoSuchPropertyException {
        for (PropertyValue propertyValue : propertyValues) {
            if (propertyValue.getField().getName().equals(name)) {
                return propertyValue;
            }
        }
        throw new NoSuchPropertyException("No such property named: [" + name + "]");
    }

    @Override
    public boolean isSingleton() {
        return scope == Scope.SINGLETON;
    }

    @Override
    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Override
    public Method[] getInitMethods() {
        return initMethods;
    }

    @Override
    public String[] getDestroyMethods() {
        return destroyMethods;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isFactoryBean() {
        return factoryBean;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public boolean isAbstract() {
        return childName != null;
    }

    @Override
    public PropertyValue[] getPropertyValues() {
        return propertyValues;
    }

    // -----------------------

    @Override
    public BeanDefinition setInitialized(boolean initialized) {
        this.initialized = initialized;
        return this;
    }

    @Override
    public BeanDefinition setFactoryBean(boolean factoryBean) {
        this.factoryBean = factoryBean;
        return this;
    }

    @Override
    public BeanDefinition setName(String name) {
        return this;
    }

    @Override
    public BeanDefinition setScope(Scope scope) {
        this.scope = scope;
        return this;
    }

    @Override
    public BeanDefinition setInitMethods(Method... initMethods) {
        this.initMethods = initMethods;
        return this;
    }

    @Override
    public BeanDefinition setInitMethods(String... initMethods) {
        ConfigurationException.nonNull(beanClass, "Bean Class must applied before invoke this method");
        return setInitMethods(ContextUtils.resolveInitMethod(initMethods, beanClass));
    }

    @Override
    public BeanDefinition setDestroyMethods(String... destroyMethods) {
        this.destroyMethods = destroyMethods;
        return this;
    }

    @Override
    public BeanDefinition setPropertyValues(PropertyValue... propertyValues) {
        this.propertyValues = propertyValues;
        return this;
    }

    @Override
    public void addPropertyValue(PropertyValue... newValues) {

        if (ObjectUtils.isNotEmpty(newValues)) { // fix

            final PropertyValue[] propertyValues = this.propertyValues;
            if (propertyValues == null) {
                this.propertyValues = newValues;
            }
            else {
                List<PropertyValue> pool = new ArrayList<>(newValues.length + propertyValues.length);

                Collections.addAll(pool, newValues);
                Collections.addAll(pool, propertyValues);

                this.propertyValues = pool.toArray(new PropertyValue[pool.size()]);
            }
        }
    }

    @Override
    public void addPropertyValue(Collection<PropertyValue> propertyValues) {

        if (propertyValues.isEmpty()) {
            return;
        }

        if (this.propertyValues != null) {
            Collections.addAll(propertyValues, this.propertyValues);
        }
        this.propertyValues = propertyValues.toArray(new PropertyValue[propertyValues.size()]);
    }

    /**
     * {@link BeanDefinition}'s Order
     * 
     * @since 2.1.7
     */
    @Override
    public int getOrder() {
        return OrderUtils.getOrder(getBeanClass());
    }

    @Override
    public String getChildBean() {
        return childName;
    }

    /**
     * Apply the child bean name
     * 
     * @param childName
     *            Child bean name
     * @return {@link DefaultBeanDefinition}
     */
    public DefaultBeanDefinition setChildName(String childName) {
        this.childName = childName;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
               || (obj instanceof BeanDefinition && Objects.equals(((BeanDefinition) obj).getName(), getName()));
    }

    @Override
    public String toString() {

        return new StringBuilder()//
                .append("{\n\t\"name\":\"").append(name)//
                .append("\",\n\t\"scope\":\"").append(scope)//
                .append("\",\n\t\"beanClass\":\"").append(beanClass)//
                .append("\",\n\t\"initMethods\":\"").append(Arrays.toString(initMethods))//
                .append("\",\n\t\"destroyMethods\":\"").append(Arrays.toString(destroyMethods))//
                .append("\",\n\t\"propertyValues\":\"").append(Arrays.toString(propertyValues))//
                .append("\",\n\t\"initialized\":\"").append(initialized)//
                .append("\",\n\t\"factoryBean\":\"").append(factoryBean)//
                .append("\",\n\t\"child\":\"").append(childName)//
                .append("\"\n}")//
                .toString();
    }

    // AnnotatedElement
    // -----------------------------

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return ClassUtils.isAnnotationPresent(getBeanClass(), annotation);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return getBeanClass().getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return getBeanClass().getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getBeanClass().getDeclaredAnnotations();
    }

}
