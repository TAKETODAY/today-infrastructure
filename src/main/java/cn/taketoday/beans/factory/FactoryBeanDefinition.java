/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.beans.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.beans.FactoryBean;
import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.SingletonSupplier;

/**
 * FactoryBean's BeanDefinition
 *
 * @author TODAY <br>
 * 2019-02-01 12:29
 * @since 2.1.7
 */
public class FactoryBeanDefinition<T>
        extends AttributeAccessorSupport implements BeanDefinition {
  /**
   * @since 4.0
   */
  private FactoryBean<T> factoryBean;

  private final BeanDefinition factoryDef;
  private Supplier<FactoryBean<T>> factorySupplier;

  public FactoryBeanDefinition(BeanDefinition factoryDef, AbstractBeanFactory beanFactory) {
    this(factoryDef);
    this.factorySupplier = new FactoryBeanSupplier<>(factoryDef, beanFactory);
  }

  public FactoryBeanDefinition(BeanDefinition factoryDef, FactoryBean<T> factoryBean) {
    this(factoryDef);
    this.factoryBean = factoryBean;
  }

  public FactoryBeanDefinition(BeanDefinition factoryDef) {
    Assert.notNull(factoryDef, "factory BeanDefinition cannot be null");
    this.factoryDef = factoryDef;
  }

  @Override
  public Class<T> getBeanClass() {
    return getFactory().getBeanClass();
  }

  @Nullable
  @Override
  public String getBeanClassName() {
    return factoryDef.getBeanClassName();
  }

  @Override
  public boolean hasBeanClass() {
    return factoryDef.hasBeanClass();
  }

  @Override
  public void setBeanClassName(@Nullable String beanClassName) {
    factoryDef.setBeanClassName(beanClassName);
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
    FactoryBean<T> factoryBean = getFactoryBean();
    if (factoryBean == null) {
      Supplier<FactoryBean<T>> supplier = getFactorySupplier();
      Assert.state(supplier != null, "factorySupplier must not be null");
      FactoryBean<T> obj = supplier.get();
      Assert.state(obj != null, "The provided FactoryBean cannot be null");
      return obj;
    }
    return factoryBean;
  }

  public void setFactory(FactoryBean<T> factory) {
    this.factorySupplier = SingletonSupplier.of(factory);
  }

  public void setFactorySupplier(Supplier<FactoryBean<T>> supplier) {
    this.factorySupplier = supplier;
  }

  /**
   * @since 4.0
   */
  public void setFactoryBean(FactoryBean<T> factoryBean) {
    this.factoryBean = factoryBean;
  }

  /**
   * @since 4.0
   */
  public FactoryBean<T> getFactoryBean() {
    return factoryBean;
  }

  //---------------------------------------------------------------------
  // Implementation of BeanDefinition interface
  //---------------------------------------------------------------------

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
  public Set<PropertyValue> getPropertyValues() {
    return factoryDef.getPropertyValues();
  }

  @Override
  public void addPropertyValues(PropertyValue... propertyValues) {
    factoryDef.addPropertyValues(propertyValues);
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
  public void addPropertyValue(String name, Object value) {
    factoryDef.addPropertyValue(name, value);
  }

  @Override
  public void setInitialized(boolean initialized) {
    factoryDef.setInitialized(initialized);
  }

  @Override
  public void setName(String name) {
    factoryDef.setName(name);
  }

  @Override
  public void setScope(String scope) {
    factoryDef.setScope(scope);
  }

  @Override
  public void setInitMethods(Method... initMethods) {
    factoryDef.setInitMethods(initMethods);
  }

  @Override
  public void setDestroyMethods(String... destroyMethods) {
    factoryDef.setDestroyMethods(destroyMethods);
  }

  @Override
  public void setPropertyValues(PropertyValue... propertyValues) {
    factoryDef.setPropertyValues(propertyValues);
  }

  @Override
  public void setPropertyValues(Collection<PropertyValue> propertyValues) {
    factoryDef.setPropertyValues(propertyValues);
  }

  @Override
  public void setFactoryBean(boolean factoryBean) { }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
    return factoryDef.isAnnotationPresent(annotation);
  }

  @Override
  public BeanDefinition getChild() {
    return factoryDef.getChild();
  }

  @Override
  public Object newInstance(BeanFactory factory) {
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

  @Override
  public void setSynthetic(boolean synthetic) {
    factoryDef.setSynthetic(synthetic);
  }

  @Override
  public boolean isSynthetic() {
    return factoryDef.isSynthetic();
  }

  @Override
  public void setRole(int role) {
    factoryDef.setRole(role);
  }

  @Override
  public int getRole() {
    return factoryDef.getRole();
  }

  @Override
  public boolean isPrimary() {
    return factoryDef.isPrimary();
  }

  @Override
  public boolean isAssignableTo(ResolvableType typeToMatch) {
    return ResolvableType.fromClass(getBeanClass())
            .isAssignableFrom(typeToMatch);
  }

  @Override
  public boolean isAssignableTo(Class<?> typeToMatch) {
    return typeToMatch.isAssignableFrom(getBeanClass());
  }

  @Override
  public void setPrimary(boolean primary) {
    factoryDef.setPrimary(primary);
  }

  public Supplier<FactoryBean<T>> getFactorySupplier() {
    return factorySupplier;
  }

  public final BeanDefinition getFactoryDefinition() {
    return factoryDef;
  }

}
