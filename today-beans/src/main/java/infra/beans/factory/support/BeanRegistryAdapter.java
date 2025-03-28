/*
 * Copyright 2017 - 2025 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.util.function.Consumer;
import java.util.function.Function;

import infra.beans.BeanUtils;
import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanRegistrar;
import infra.beans.factory.BeanRegistry;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionCustomizer;
import infra.core.ParameterizedTypeReference;
import infra.core.ResolvableType;
import infra.core.env.Environment;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.MultiValueMap;

/**
 * {@link BeanRegistry} implementation that delegates to
 * {@link BeanDefinitionRegistry} and {@link BeanFactory}.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class BeanRegistryAdapter implements BeanRegistry {

  private final BeanDefinitionRegistry beanRegistry;

  private final BeanFactory beanFactory;

  private final Environment environment;

  private final Class<? extends BeanRegistrar> beanRegistrarClass;

  @Nullable
  private final MultiValueMap<String, BeanDefinitionCustomizer> customizers;

  public BeanRegistryAdapter(StandardBeanFactory beanFactory, Environment environment,
          Class<? extends BeanRegistrar> beanRegistrarClass) {

    this(beanFactory, beanFactory, environment, beanRegistrarClass, null);
  }

  public BeanRegistryAdapter(BeanDefinitionRegistry beanRegistry, BeanFactory beanFactory,
          Environment environment, Class<? extends BeanRegistrar> beanRegistrarClass) {

    this(beanRegistry, beanFactory, environment, beanRegistrarClass, null);
  }

  public BeanRegistryAdapter(BeanDefinitionRegistry beanRegistry, BeanFactory beanFactory,
          Environment environment, Class<? extends BeanRegistrar> beanRegistrarClass,
          @Nullable MultiValueMap<String, BeanDefinitionCustomizer> customizers) {

    this.beanRegistry = beanRegistry;
    this.beanFactory = beanFactory;
    this.environment = environment;
    this.beanRegistrarClass = beanRegistrarClass;
    this.customizers = customizers;
  }

  @Override
  public <T> String registerBean(Class<T> beanClass) {
    String beanName = BeanDefinitionReaderUtils.uniqueBeanName(beanClass.getName(), this.beanRegistry);
    registerBean(beanName, beanClass);
    return beanName;
  }

  @Override
  public <T> String registerBean(Class<T> beanClass, Consumer<Spec<T>> customizer) {
    String beanName = BeanDefinitionReaderUtils.uniqueBeanName(beanClass.getName(), this.beanRegistry);
    registerBean(beanName, beanClass, customizer);
    return beanName;
  }

  @Override
  public <T> void registerBean(String name, Class<T> beanClass) {
    BeanRegistrarBeanDefinition beanDefinition = new BeanRegistrarBeanDefinition(beanClass, this.beanRegistrarClass);
    if (this.customizers != null && this.customizers.containsKey(name)) {
      for (BeanDefinitionCustomizer customizer : this.customizers.get(name)) {
        customizer.customize(beanDefinition);
      }
    }
    this.beanRegistry.registerBeanDefinition(name, beanDefinition);
  }

  @Override
  public <T> void registerBean(String name, Class<T> beanClass, Consumer<Spec<T>> spec) {
    BeanRegistrarBeanDefinition beanDefinition = new BeanRegistrarBeanDefinition(beanClass, this.beanRegistrarClass);
    spec.accept(new BeanSpecAdapter<>(beanDefinition, this.beanFactory));
    if (this.customizers != null && this.customizers.containsKey(name)) {
      for (BeanDefinitionCustomizer customizer : this.customizers.get(name)) {
        customizer.customize(beanDefinition);
      }
    }
    this.beanRegistry.registerBeanDefinition(name, beanDefinition);
  }

  @Override
  public void register(BeanRegistrar registrar) {
    Assert.notNull(registrar, "'registrar' is required");
    registrar.register(this, this.environment);
  }

  @Override
  public void registerAlias(String name, String alias) {
    this.beanRegistry.registerAlias(name, alias);
  }

  /**
   * {@link RootBeanDefinition} subclass for {@code #registerBean} based
   * registrations with constructors resolution match{@link BeanUtils#getConstructor}
   * behavior. It also sets the bean registrar class as the source.
   */
  @SuppressWarnings("serial")
  private static class BeanRegistrarBeanDefinition extends RootBeanDefinition {

    public BeanRegistrarBeanDefinition(Class<?> beanClass, Class<? extends BeanRegistrar> beanRegistrarClass) {
      super(beanClass);
      this.setSource(beanRegistrarClass);
      this.setAttribute("aotProcessingIgnoreRegistration", true);
    }

    public BeanRegistrarBeanDefinition(BeanRegistrarBeanDefinition original) {
      super(original);
    }

    @Nullable
    @Override
    public Constructor<?>[] getPreferredConstructors() {
      if (this.getInstanceSupplier() != null) {
        return null;
      }
      try {
        return new Constructor<?>[] { BeanUtils.getConstructor(getBeanClass()) };
      }
      catch (IllegalStateException ex) {
        return null;
      }
    }

    @Override
    public RootBeanDefinition cloneBeanDefinition() {
      return new BeanRegistrarBeanDefinition(this);
    }
  }

  static class BeanSpecAdapter<T> implements Spec<T> {

    private final RootBeanDefinition beanDefinition;

    private final BeanFactory beanFactory;

    public BeanSpecAdapter(RootBeanDefinition beanDefinition, BeanFactory beanFactory) {
      this.beanDefinition = beanDefinition;
      this.beanFactory = beanFactory;
    }

    @Override
    public Spec<T> backgroundInit() {
      this.beanDefinition.setBackgroundInit(true);
      return this;
    }

    @Override
    public Spec<T> fallback() {
      this.beanDefinition.setFallback(true);
      return this;
    }

    @Override
    public Spec<T> primary() {
      this.beanDefinition.setPrimary(true);
      return this;
    }

    @Override
    public Spec<T> description(String description) {
      this.beanDefinition.setDescription(description);
      return this;
    }

    @Override
    public Spec<T> infrastructure() {
      this.beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      return this;
    }

    @Override
    public Spec<T> lazyInit() {
      this.beanDefinition.setLazyInit(true);
      return this;
    }

    @Override
    public Spec<T> notAutowirable() {
      this.beanDefinition.setAutowireCandidate(false);
      return this;
    }

    @Override
    public Spec<T> order(int order) {
      this.beanDefinition.setAttribute(AbstractBeanDefinition.ORDER_ATTRIBUTE, order);
      return this;
    }

    @Override
    public Spec<T> prototype() {
      this.beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
      return this;
    }

    @Override
    public Spec<T> supplier(Function<SupplierContext, T> supplier) {
      this.beanDefinition.setInstanceSupplier(() ->
              supplier.apply(new SupplierContextAdapter(this.beanFactory)));
      return this;
    }

    @Override
    public Spec<T> targetType(ParameterizedTypeReference<? extends T> targetType) {
      this.beanDefinition.setTargetType(ResolvableType.forType(targetType));
      return this;
    }

    @Override
    public Spec<T> targetType(ResolvableType targetType) {
      this.beanDefinition.setTargetType(targetType);
      return this;
    }
  }

  static class SupplierContextAdapter implements SupplierContext {

    private final BeanFactory beanFactory;

    public SupplierContextAdapter(BeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

    @Override
    public <T> T bean(Class<T> requiredType) throws BeansException {
      return this.beanFactory.getBean(requiredType);
    }

    @Override
    public <T> T bean(String name, Class<T> requiredType) throws BeansException {
      return this.beanFactory.getBean(name, requiredType);
    }

    @Override
    public <T> ObjectProvider<T> beanProvider(Class<T> requiredType) {
      return this.beanFactory.getBeanProvider(requiredType);
    }

    @Override
    public <T> ObjectProvider<T> beanProvider(ResolvableType requiredType) {
      return this.beanFactory.getBeanProvider(requiredType);
    }
  }

}
