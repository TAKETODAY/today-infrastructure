/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright ©  TODAY & 2017 - 2019 All Rights Reserved.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Scope;
import cn.taketoday.context.exception.NoSuchPropertyException;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.factory.InitializingBean;

/**
 * Default implementation of {@link BeanDefinition}
 * 
 * @author TODAY <br>
 *         2019-02-01 12:23
 */
public class DefaultBeanDefinition implements BeanDefinition {

    /** bean name. */
    private String name;
    /** bean class. */
    private Class<? extends Object> beanClass;
    /** bean scope. */
    private Scope scope = Scope.SINGLETON;

    /**
     * Invoke before {@link InitializingBean#afterPropertiesSet}
     * 
     * @since 2.3.3
     */
    private Method[] initMethods = new Method[0];

    /**
     * Invoke after when publish
     * {@link ApplicationContext#publishEvent(cn.taketoday.context.event.ContextCloseEvent)}
     * 
     * @since 2.3.3
     */
    private String[] destroyMethods = new String[0];

    /** property values */
    private PropertyValue[] propertyValues = new PropertyValue[0];

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

    /**
     * Is Abstract ?
     * 
     * @since 2.0.0
     */
    private boolean Abstract = false;

    public DefaultBeanDefinition() {

    }

    public DefaultBeanDefinition(String name, Class<? extends Object> beanClass) {
        this.beanClass = beanClass;
        this.name = name;
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
        return Abstract;
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
    public BeanDefinition setAbstract(boolean Abstract) {
        this.Abstract = Abstract;
        return this;
    }

    @Override
    public BeanDefinition setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public BeanDefinition setScope(Scope scope) {
        this.scope = scope;
        return this;
    }

    @Override
    public BeanDefinition setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
        return this;
    }

    @Override
    public BeanDefinition setInitMethods(Method... initMethods) {
        this.initMethods = initMethods;
        return this;
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
    public void addPropertyValue(PropertyValue... propertyValues_) {

        if (this.propertyValues == null) {
            this.propertyValues = propertyValues_;
        }
        else {
            List<PropertyValue> propertyValues = new ArrayList<>();
            Collections.addAll(propertyValues, propertyValues_);

            this.propertyValues = propertyValues.toArray(new PropertyValue[0]);
        }
    }

    @Override
    public void addPropertyValue(Collection<PropertyValue> propertyValues) {

        if (propertyValues.isEmpty()) {
            return;
        }

        if (this.propertyValues == null) {
            this.propertyValues = propertyValues.toArray(new PropertyValue[0]);
        }
        else {
            // not null
            Collections.addAll(propertyValues, this.propertyValues);
            this.propertyValues = propertyValues.toArray(new PropertyValue[0]);
        }
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
                .append("\",\n\t\"abstract\":\"").append(Abstract)//
                .append("\"\n}")//
                .toString();
    }

}
