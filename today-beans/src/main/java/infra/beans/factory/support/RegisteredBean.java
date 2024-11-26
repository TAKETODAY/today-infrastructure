/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.beans.factory.support;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import infra.beans.TypeConverter;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.DependencyDescriptor;
import infra.core.ResolvableType;
import infra.core.style.ToStringBuilder;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.StringUtils;

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
  public static RegisteredBean of(ConfigurableBeanFactory beanFactory, String beanName) {
    Assert.notNull(beanFactory, "'beanFactory' is required");
    Assert.hasLength(beanName, "'beanName' must not be empty");
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
  static RegisteredBean of(ConfigurableBeanFactory beanFactory, String beanName, RootBeanDefinition mbd) {
    return new RegisteredBean(beanFactory, () -> beanName, false, () -> mbd, null);
  }

  /**
   * Create a new {@link RegisteredBean} instance for an inner-bean.
   *
   * @param parent the parent of the inner-bean
   * @param innerBean a {@link BeanDefinitionHolder} for the inner bean
   * @return a new {@link RegisteredBean} instance
   */
  public static RegisteredBean ofInnerBean(RegisteredBean parent, BeanDefinitionHolder innerBean) {
    Assert.notNull(innerBean, "'innerBean' is required");
    return ofInnerBean(parent, innerBean.getBeanName(), innerBean.getBeanDefinition());
  }

  /**
   * Create a new {@link RegisteredBean} instance for an inner-bean.
   *
   * @param parent the parent of the inner-bean
   * @param innerBeanDefinition the inner-bean definition
   * @return a new {@link RegisteredBean} instance
   */
  public static RegisteredBean ofInnerBean(RegisteredBean parent, BeanDefinition innerBeanDefinition) {
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

    Assert.notNull(parent, "'parent' is required");
    Assert.notNull(innerBeanDefinition, "'innerBeanDefinition' is required");
    InnerBeanResolver resolver = new InnerBeanResolver(parent, innerBeanName, innerBeanDefinition);
    Supplier<String> beanName = StringUtils.isNotEmpty(innerBeanName) ? () -> innerBeanName : resolver::resolveBeanName;
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
   * Resolve the {@linkplain InstantiationDescriptor descriptor} to use to
   * instantiate this bean. It defines the {@link java.lang.reflect.Constructor}
   * or {@link java.lang.reflect.Method} to use as well as additional metadata.
   */
  public InstantiationDescriptor resolveInstantiationDescriptor() {
    Executable executable = resolveConstructorOrFactoryMethod();
    if (executable instanceof Method method && !Modifier.isStatic(method.getModifiers())) {
      String factoryBeanName = getMergedBeanDefinition().getFactoryBeanName();
      if (factoryBeanName != null && this.beanFactory.containsBean(factoryBeanName)) {
        return new InstantiationDescriptor(executable,
                this.beanFactory.getMergedBeanDefinition(factoryBeanName).getResolvableType().toClass());
      }
    }
    return new InstantiationDescriptor(executable, executable.getDeclaringClass());
  }

  /**
   * Resolve an autowired argument.
   *
   * @param descriptor the descriptor for the dependency (field/method/constructor)
   * @param typeConverter the TypeConverter to use for populating arrays and collections
   * @param autowiredBeanNames a Set that all names of autowired beans (used for
   * resolving the given dependency) are supposed to be added to
   * @return the resolved object, or {@code null} if none found
   */
  @Nullable
  public Object resolveAutowiredArgument(DependencyDescriptor descriptor, TypeConverter typeConverter, Set<String> autowiredBeanNames) {
    return new ConstructorResolver((AbstractAutowireCapableBeanFactory) getBeanFactory())
            .resolveAutowiredArgument(descriptor, descriptor.getDependencyType(),
                    getBeanName(), autowiredBeanNames, typeConverter, true);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("beanName", getBeanName())
            .append("mergedBeanDefinition", getMergedBeanDefinition()).toString();
  }

  /**
   * Descriptor for how a bean should be instantiated. While the {@code targetClass}
   * is usually the declaring class of the {@code executable} (in case of a constructor
   * or a locally declared factory method), there are cases where retaining the actual
   * concrete class is necessary (e.g. for an inherited factory method).
   *
   * @param executable the {@link Executable} ({@link java.lang.reflect.Constructor}
   * or {@link java.lang.reflect.Method}) to invoke
   * @param targetClass the target {@link Class} of the executable
   */
  public record InstantiationDescriptor(Executable executable, Class<?> targetClass) {

    public InstantiationDescriptor(Executable executable) {
      this(executable, executable.getDeclaringClass());
    }
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

    InnerBeanResolver(RegisteredBean parent, @Nullable String innerBeanName, BeanDefinition innerBeanDefinition) {
      Assert.isInstanceOf(AbstractAutowireCapableBeanFactory.class, parent.getBeanFactory());
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
      return resolveInnerBean((beanName, mergedBeanDefinition) -> mergedBeanDefinition);
    }

    private <T> T resolveInnerBean(BiFunction<String, RootBeanDefinition, T> resolver) {
      // Always use a fresh BeanDefinitionValueResolver in case the parent merged bean definition has changed.
      var beanDefinitionValueResolver = new BeanDefinitionValueResolver(
              (AbstractAutowireCapableBeanFactory) this.parent.getBeanFactory(),
              this.parent.getBeanName(), this.parent.getMergedBeanDefinition());
      return beanDefinitionValueResolver.resolveInnerBean(this.innerBeanName, this.innerBeanDefinition, resolver);
    }
  }

}
