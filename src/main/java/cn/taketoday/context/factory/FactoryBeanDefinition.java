/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Supplier;

import cn.taketoday.context.AttributeAccessorSupport;
import cn.taketoday.context.exception.NoSuchPropertyException;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.SingletonSupplier;

import static cn.taketoday.context.utils.ContextUtils.createBeanDefinition;

/**
 * FactoryBean's BeanDefinition
 *
 * @author TODAY <br>
 * 2019-02-01 12:29
 * @since 2.1.7
 */
public class FactoryBeanDefinition<T>
        extends AttributeAccessorSupport implements BeanDefinition {

  private final BeanDefinition factoryDef;
  private Supplier<FactoryBean<T>> factorySupplier;

  public FactoryBeanDefinition(String name, Class<?> factoryClass, FactoryBean<T> factoryBean) {
    this(createBeanDefinition(name, factoryClass), SingletonSupplier.of(factoryBean));
  }

  public FactoryBeanDefinition(String name, Class<?> factoryClass, Supplier<FactoryBean<T>> factorySupplier) {
    this(createBeanDefinition(name, factoryClass), factorySupplier);
  }

  public FactoryBeanDefinition(BeanDefinition factoryDef, AbstractBeanFactory beanFactory) {
    this(factoryDef, new FactoryBeanSupplier<>(factoryDef, beanFactory));
  }

  public FactoryBeanDefinition(BeanDefinition factoryDef, Supplier<FactoryBean<T>> factorySupplier) {
    Assert.notNull(factoryDef, "factory BeanDefinition cannot be null");
    this.factoryDef = factoryDef;
    this.factorySupplier = factorySupplier;
  }

  public FactoryBeanDefinition(String name, Class<T> factoryClass, AbstractBeanFactory beanFactory) {
    this.factoryDef = beanFactory.getBeanDefinitionLoader().createBeanDefinition(name, factoryClass);
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
  public final boolean isFactoryBean() {
    return true;
  }

  public T getBean() {
    return getFactory().getBean();
  }

  public FactoryBean<T> getFactory() {
    final Supplier<FactoryBean<T>> supplier = getFactorySupplier();
    Assert.state(supplier != null, "factorySupplier must not be null");
    final FactoryBean<T> obj = supplier.get();
    Assert.state(obj != null, "The provided FactoryBean cannot be null");
    return obj;
  }

  public void setFactory(FactoryBean<T> factory) {
    this.factorySupplier = SingletonSupplier.of(factory);
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
  public PropertySetter getPropertyValue(String name) throws NoSuchPropertyException {
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
  public PropertySetter[] getPropertySetters() {
    return factoryDef.getPropertySetters();
  }

  @Override
  public void addPropertyValue(String name, Object value) {
    factoryDef.addPropertyValue(name, value);
  }

  @Override
  public void addPropertySetter(PropertySetter... propertySetters) {
    factoryDef.addPropertySetter(propertySetters);
  }

  @Override
  public void addPropertySetter(Collection<PropertySetter> propertySetters) {
    factoryDef.addPropertySetter(propertySetters);
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
  public FactoryBeanDefinition<T> setPropertyValues(PropertySetter... propertySetters) {
    factoryDef.setPropertyValues(propertySetters);
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

  @Override
  public Object newInstance(final BeanFactory factory) {
    return factoryDef.newInstance(factory);
  }

  @Override
  public Object newInstance(BeanFactory factory, Object... args) {
    return factoryDef.newInstance(factory, args);
  }

  @Override
  public boolean isLazyInit() {
    return factoryDef.isLazyInit();
  }

  @Override
  public void copy(BeanDefinition newDef) {
    factoryDef.copy(newDef);
  }

  @Override
  public void setLazyInit(boolean lazyInit) {
    factoryDef.setLazyInit(lazyInit);
  }

  @Override
  public <T> void setSupplier(Supplier<T> supplier) {
    factoryDef.setSupplier(supplier);
  }

  public Supplier<FactoryBean<T>> getFactorySupplier() {
    return factorySupplier;
  }

  public final BeanDefinition getFactoryDefinition() {
    return factoryDef;
  }

}
