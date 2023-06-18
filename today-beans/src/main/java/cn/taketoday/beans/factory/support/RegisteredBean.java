/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Executable;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.function.SingletonSupplier;

/**
 * A {@code RegisteredBean} represents a bean that has been registered with a
 * {@link BeanFactory}, but has not necessarily been instantiated. It provides
 * access to the bean factory that contains the bean as well as the bean name.
 * In the case of inner-beans, the bean name may have been generated.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class RegisteredBean {

  private final ConfigurableBeanFactory beanFactory;

  private final Supplier<String> beanName;

  private final boolean generatedBeanName;

  private final Supplier<RootBeanDefinition> mergedBeanDefinition;

  @Nullable
  private final RegisteredBean parent;

  private RegisteredBean(ConfigurableBeanFactory beanFactory, Supplier<String> beanName,
          boolean generatedBeanName, Supplier<RootBeanDefinition> mergedBeanDefinition,
          @Nullable RegisteredBean parent) {

    this.beanFactory = beanFactory;
    this.beanName = beanName;
    this.generatedBeanName = generatedBeanName;
    this.mergedBeanDefinition = mergedBeanDefinition;
    this.parent = parent;
  }

  /**
   * Create a new {@link RegisteredBean} instance for a regular bean.
   *
   * @param beanFactory the source bean factory
   * @param beanName the bean name
   * @return a new {@link RegisteredBean} instance
   */
  public static RegisteredBean of(ConfigurableBeanFactory beanFactory,
          String beanName) {

    Assert.notNull(beanFactory, "BeanFactory must not be null");
    Assert.hasLength(beanName, "BeanName must not be empty");
    return new RegisteredBean(beanFactory, () -> beanName, false,
            () -> (RootBeanDefinition) beanFactory.getMergedBeanDefinition(beanName),
            null);
  }

  /**
   * Create a new {@link RegisteredBean} instance for a regular bean.
   *
   * @param beanFactory the source bean factory
   * @param beanName the bean name
   * @param mbd the pre-determined merged bean definition
   * @return a new {@link RegisteredBean} instance
   */
  public static RegisteredBean of(ConfigurableBeanFactory beanFactory, String beanName, RootBeanDefinition mbd) {
    return new RegisteredBean(beanFactory, () -> beanName, false, () -> mbd, null);
  }

  /**
   * Create a new {@link RegisteredBean} instance for an inner-bean.
   *
   * @param parent the parent of the inner-bean
   * @param innerBean a {@link BeanDefinitionHolder} for the inner bean
   * @return a new {@link RegisteredBean} instance
   */
  public static RegisteredBean ofInnerBean(RegisteredBean parent,
          BeanDefinitionHolder innerBean) {

    Assert.notNull(innerBean, "InnerBean must not be null");
    return ofInnerBean(parent, innerBean.getBeanName(),
            innerBean.getBeanDefinition());
  }

  /**
   * Create a new {@link RegisteredBean} instance for an inner-bean.
   *
   * @param parent the parent of the inner-bean
   * @param innerBeanDefinition the inner-bean definition
   * @return a new {@link RegisteredBean} instance
   */
  public static RegisteredBean ofInnerBean(RegisteredBean parent,
          BeanDefinition innerBeanDefinition) {

    return ofInnerBean(parent, null, innerBeanDefinition);
  }

  /**
   * Create a new {@link RegisteredBean} instance for an inner-bean.
   *
   * @param parent the parent of the inner-bean
   * @param innerBeanName the name of the inner bean or {@code null} to
   * generate a name
   * @param innerBeanDefinition the inner-bean definition
   * @return a new {@link RegisteredBean} instance
   */
  public static RegisteredBean ofInnerBean(RegisteredBean parent,
          @Nullable String innerBeanName, BeanDefinition innerBeanDefinition) {

    Assert.notNull(parent, "Parent must not be null");
    Assert.notNull(innerBeanDefinition, "InnerBeanDefinition must not be null");
    InnerBeanResolver resolver = new InnerBeanResolver(parent, innerBeanName, innerBeanDefinition);
    Supplier<String> beanName = StringUtils.isNotEmpty(innerBeanName)
                                ? SingletonSupplier.valueOf(innerBeanName)
                                : resolver::resolveBeanName;
    return new RegisteredBean(parent.getBeanFactory(), beanName,
            innerBeanName == null, resolver::resolveMergedBeanDefinition, parent);
  }

  /**
   * Return the name of the bean.
   *
   * @return the beanName the bean name
   */
  public String getBeanName() {
    return this.beanName.get();
  }

  /**
   * Return if the bean name is generated.
   *
   * @return {@code true} if the name was generated
   */
  public boolean isGeneratedBeanName() {
    return this.generatedBeanName;
  }

  /**
   * Return the bean factory containing the bean.
   *
   * @return the bean factory
   */
  public ConfigurableBeanFactory getBeanFactory() {
    return this.beanFactory;
  }

  /**
   * Return the user-defined class of the bean.
   *
   * @return the bean class
   */
  public Class<?> getBeanClass() {
    return ClassUtils.getUserClass(getBeanType().toClass());
  }

  /**
   * Return the {@link ResolvableType} of the bean.
   *
   * @return the bean type
   */
  public ResolvableType getBeanType() {
    return getMergedBeanDefinition().getResolvableType();
  }

  /**
   * Return the merged bean definition of the bean.
   *
   * @return the merged bean definition
   * @see ConfigurableBeanFactory#getMergedBeanDefinition(String)
   */
  public RootBeanDefinition getMergedBeanDefinition() {
    return this.mergedBeanDefinition.get();
  }

  /**
   * Return if this instance is for an inner-bean.
   *
   * @return if an inner-bean
   */
  public boolean isInnerBean() {
    return this.parent != null;
  }

  /**
   * Return the parent of this instance or {@code null} if not an inner-bean.
   *
   * @return the parent
   */
  @Nullable
  public RegisteredBean getParent() {
    return this.parent;
  }

  /**
   * Resolve the constructor or factory method to use for this bean.
   *
   * @return the {@link java.lang.reflect.Constructor} or {@link java.lang.reflect.Method}
   */
  public Executable resolveConstructorOrFactoryMethod() {
    return new ConstructorResolver((AbstractAutowireCapableBeanFactory) getBeanFactory())
            .resolveConstructorOrFactoryMethod(getBeanName(), getMergedBeanDefinition());
  }

  /**
   * Resolve an autowired argument.
   *
   * @param descriptor the descriptor for the dependency (field/method/constructor)
   * @param typeConverter the TypeConverter to use for populating arrays and collections
   * @param autowiredBeans a Set that all names of autowired beans (used for
   * resolving the given dependency) are supposed to be added to
   * @return the resolved object, or {@code null} if none found
   */
  @Nullable
  public Object resolveAutowiredArgument(DependencyDescriptor descriptor,
          TypeConverter typeConverter, Set<String> autowiredBeans) {
    return new ConstructorResolver((AbstractAutowireCapableBeanFactory) getBeanFactory())
            .resolveAutowiredArgument(descriptor, getBeanName(), autowiredBeans, typeConverter, true);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("beanName", getBeanName())
            .append("mergedBeanDefinition", getMergedBeanDefinition()).toString();
  }

  /**
   * Resolver used to obtain inner-bean details.
   */
  private static class InnerBeanResolver {

    private final RegisteredBean parent;

    @Nullable
    private final String innerBeanName;

    private final BeanDefinition innerBeanDefinition;

    @Nullable
    private volatile String resolvedBeanName;

    InnerBeanResolver(RegisteredBean parent, @Nullable String innerBeanName,
            BeanDefinition innerBeanDefinition) {

      Assert.isInstanceOf(AbstractAutowireCapableBeanFactory.class,
              parent.getBeanFactory());
      this.parent = parent;
      this.innerBeanName = innerBeanName;
      this.innerBeanDefinition = innerBeanDefinition;
    }

    String resolveBeanName() {
      String resolvedBeanName = this.resolvedBeanName;
      if (resolvedBeanName != null) {
        return resolvedBeanName;
      }
      resolvedBeanName = resolveInnerBean((beanName, mergedBeanDefinition) -> beanName);
      this.resolvedBeanName = resolvedBeanName;
      return resolvedBeanName;
    }

    RootBeanDefinition resolveMergedBeanDefinition() {
      return resolveInnerBean(
              (beanName, mergedBeanDefinition) -> mergedBeanDefinition);
    }

    private <T> T resolveInnerBean(
            BiFunction<String, RootBeanDefinition, T> resolver) {

      // Always use a fresh BeanDefinitionValueResolver in case the parent merged bean definition has changed.
      BeanDefinitionValueResolver beanDefinitionValueResolver = new BeanDefinitionValueResolver(
              parent.getBeanFactory().unwrap(AbstractAutowireCapableBeanFactory.class),
              parent.getBeanName(), parent.getMergedBeanDefinition());
      return beanDefinitionValueResolver.resolveInnerBean(innerBeanName, innerBeanDefinition, resolver);
    }

  }

}
