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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Supplier;

import cn.taketoday.context.Scope;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchPropertyException;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.factory.FactoryBeanSupplier;
import cn.taketoday.context.utils.ContextUtils;

/**
 * FactoryBean's BeanDefinition
 * 
 * @author TODAY <br>
 *         2019-02-01 12:29
 * @since 2.1.7
 */
public class FactoryBeanDefinition<T> implements BeanDefinition {

    private final BeanDefinition factoryDef;
    private Supplier<FactoryBean<T>> factorySupplier;

    public FactoryBeanDefinition(String name, Class<?> factoryClass, FactoryBean<T> factoryBean) {
        this(ContextUtils.buildBeanDefinition(factoryClass, null, name), () -> factoryBean);
    }

    public FactoryBeanDefinition(BeanDefinition factoryDef, Supplier<FactoryBean<T>> factorySupplier) {
        this.factoryDef = factoryDef;
        this.factorySupplier = factorySupplier;
    }

    public FactoryBeanDefinition(String name, Class<T> factoryClass, AbstractBeanFactory beanFactory) {
        this.factoryDef = ContextUtils.buildBeanDefinition(factoryClass, null, name);
        this.factorySupplier = new FactoryBeanSupplier<>(factoryDef, beanFactory);
    }

    public FactoryBeanDefinition(String name, Class<?> factoryClass, Supplier<FactoryBean<T>> factorySupplier) {
        this(ContextUtils.buildBeanDefinition(factoryClass, null, name), factorySupplier);
    }

    public Class<T> getBeanClass() {
        return getFactory().getBeanClass();
    }

    @Override
    public String getName() {
        return getFactoryBeanDefinition().getName();
    }

    @Override
    public boolean isFactoryBean() {
        return true;
    }

    public T getBean() {
        return getFactory().getBean();
    }

    public FactoryBean<T> getFactory() {
        return ConfigurationException.nonNull(factorySupplier.get(), "The provided FactoryBean cannot be null");
    }

    public void setFactory(FactoryBean<T> factory) {
        this.factorySupplier = () -> factory;
    }

    public void setFactorySupplier(Supplier<FactoryBean<T>> supplier) {
        this.factorySupplier = supplier;
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return getFactoryBeanDefinition().getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return getFactoryBeanDefinition().getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getFactoryBeanDefinition().getDeclaredAnnotations();
    }

    @Override
    public PropertyValue getPropertyValue(String name) throws NoSuchPropertyException {
        return getFactoryBeanDefinition().getPropertyValue(name);
    }

    @Override
    public boolean isSingleton() {
        return getFactoryBeanDefinition().isSingleton();
    }

    @Override
    public Method[] getInitMethods() {
        return getFactoryBeanDefinition().getInitMethods();
    }

    @Override
    public String[] getDestroyMethods() {
        return getFactoryBeanDefinition().getDestroyMethods();
    }

    @Override
    public Scope getScope() {
        return getFactoryBeanDefinition().getScope();
    }

    @Override
    public boolean isInitialized() {
        return getFactoryBeanDefinition().isInitialized();
    }

    @Override
    public boolean isAbstract() {
        return getFactoryBeanDefinition().isAbstract();
    }

    @Override
    public PropertyValue[] getPropertyValues() {
        return getFactoryBeanDefinition().getPropertyValues();
    }

    @Override
    public void addPropertyValue(PropertyValue... propertyValues) {
        getFactoryBeanDefinition().addPropertyValue(propertyValues);
    }

    @Override
    public void addPropertyValue(Collection<PropertyValue> propertyValues) {
        getFactoryBeanDefinition().addPropertyValue(propertyValues);
    }

    @Override
    public FactoryBeanDefinition<T> setInitialized(boolean initialized) {
        getFactoryBeanDefinition().setInitialized(initialized);
        return this;
    }

    @Override
    public BeanDefinition setName(String name) {
        return this;
    }

    @Override
    public FactoryBeanDefinition<T> setScope(Scope scope) {
        getFactoryBeanDefinition().setScope(scope);
        return this;
    }

    @Override
    public BeanDefinition setInitMethods(Method... initMethods) {
        getFactoryBeanDefinition().setInitMethods(initMethods);
        return this;
    }

    @Override
    public FactoryBeanDefinition<T> setDestroyMethods(String... destroyMethods) {
        getFactoryBeanDefinition().setDestroyMethods(destroyMethods);
        return this;
    }

    @Override
    public FactoryBeanDefinition<T> setPropertyValues(PropertyValue... propertyValues) {
        getFactoryBeanDefinition().setPropertyValues(propertyValues);
        return this;
    }

    @Override
    public FactoryBeanDefinition<T> setFactoryBean(boolean factoryBean) {
        return this;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        getFactoryBeanDefinition().isAnnotationPresent(annotation);
        return false;
    }

    @Override
    public String getChildBean() {
        return getFactoryBeanDefinition().getChildBean();
    }

    @Override
    public FactoryBeanDefinition<T> setInitMethods(String... initMethods) {
        getFactoryBeanDefinition().setInitMethods(initMethods);
        return this;
    }

    public final BeanDefinition getFactoryBeanDefinition() {
        return factoryDef;
    }
}
