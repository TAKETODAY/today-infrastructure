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
package cn.taketoday.context.factory;

import static cn.taketoday.context.exception.ConfigurationException.nonNull;
import static cn.taketoday.context.utils.ContextUtils.createBeanDefinition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Supplier;

import cn.taketoday.context.exception.NoSuchPropertyException;
import cn.taketoday.context.utils.Assert;

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
        this(createBeanDefinition(name, factoryClass), () -> factoryBean);
    }

    public FactoryBeanDefinition(String name, Class<?> factoryClass, Supplier<FactoryBean<T>> factorySupplier) {
        this(createBeanDefinition(name, factoryClass), factorySupplier);
    }

    public FactoryBeanDefinition(BeanDefinition factoryDef, AbstractBeanFactory beanFactory) {
        this(factoryDef, new FactoryBeanSupplier<>(factoryDef, beanFactory));
    }

    public FactoryBeanDefinition(BeanDefinition factoryDef, Supplier<FactoryBean<T>> factorySupplier) {
        this.factoryDef = nonNull(factoryDef);
        this.factorySupplier = factorySupplier;
    }

    public FactoryBeanDefinition(String name, Class<T> factoryClass, AbstractBeanFactory beanFactory) {
        this.factoryDef = createBeanDefinition(name, factoryClass);
        this.factorySupplier = new FactoryBeanSupplier<>(factoryDef, beanFactory);
    }

    @Override
    public Class<T> getBeanClass() {
        return getFactory().getBeanClass();
    }

    @Override
    public String getName() {
        return factoryDef.getName();
    }

    @Override
    public boolean isFactoryBean() {
        return true;
    }

    public T getBean() {
        return getFactory().getBean();
    }

    public FactoryBean<T> getFactory() {
        final Supplier<FactoryBean<T>> supplier = getFactorySupplier();
        Assert.state(supplier != null, "factorySupplier must not be null");
        return nonNull(supplier.get(), "The provided FactoryBean cannot be null");
    }

    public void setFactory(FactoryBean<T> factory) {
        this.factorySupplier = () -> factory;
    }

    public void setFactorySupplier(Supplier<FactoryBean<T>> supplier) {
        this.factorySupplier = supplier;
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return factoryDef.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return factoryDef.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return factoryDef.getDeclaredAnnotations();
    }

    @Override
    public PropertyValue getPropertyValue(String name) throws NoSuchPropertyException {
        return factoryDef.getPropertyValue(name);
    }

    @Override
    public boolean isSingleton() {
        return factoryDef.isSingleton();
    }

    @Override
    public boolean isPrototype() {
        return factoryDef.isPrototype();
    }

    @Override
    public Method[] getInitMethods() {
        return factoryDef.getInitMethods();
    }

    @Override
    public String[] getDestroyMethods() {
        return factoryDef.getDestroyMethods();
    }

    @Override
    public String getScope() {
        return factoryDef.getScope();
    }

    @Override
    public boolean isInitialized() {
        return factoryDef.isInitialized();
    }

    @Override
    public boolean isAbstract() {
        return factoryDef.isAbstract();
    }

    @Override
    public PropertyValue[] getPropertyValues() {
        return factoryDef.getPropertyValues();
    }

    @Override
    public void addPropertyValue(PropertyValue... propertyValues) {
        factoryDef.addPropertyValue(propertyValues);
    }

    @Override
    public void addPropertyValue(Collection<PropertyValue> propertyValues) {
        factoryDef.addPropertyValue(propertyValues);
    }

    @Override
    public FactoryBeanDefinition<T> setInitialized(boolean initialized) {
        factoryDef.setInitialized(initialized);
        return this;
    }

    @Override
    public BeanDefinition setName(String name) {
        factoryDef.setName(name);
        return this;
    }

    @Override
    public FactoryBeanDefinition<T> setScope(String scope) {
        factoryDef.setScope(scope);
        return this;
    }

    @Override
    public BeanDefinition setInitMethods(Method... initMethods) {
        factoryDef.setInitMethods(initMethods);
        return this;
    }

    @Override
    public FactoryBeanDefinition<T> setDestroyMethods(String... destroyMethods) {
        factoryDef.setDestroyMethods(destroyMethods);
        return this;
    }

    @Override
    public FactoryBeanDefinition<T> setPropertyValues(PropertyValue... propertyValues) {
        factoryDef.setPropertyValues(propertyValues);
        return this;
    }

    @Override
    public FactoryBeanDefinition<T> setFactoryBean(boolean factoryBean) {
        return this;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return factoryDef.isAnnotationPresent(annotation);
    }

    @Override
    public BeanDefinition getChild() {
        return factoryDef.getChild();
    }

    @Override
    public FactoryBeanDefinition<T> setInitMethods(String... initMethods) {
        factoryDef.setInitMethods(initMethods);
        return this;
    }

    public Supplier<FactoryBean<T>> getFactorySupplier() {
        return factorySupplier;
    }

    public final BeanDefinition getFactoryDefinition() {
        return factoryDef;
    }

}
