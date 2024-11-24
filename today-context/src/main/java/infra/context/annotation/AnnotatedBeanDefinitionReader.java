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

package infra.context.annotation;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionCustomizer;
import infra.beans.factory.config.BeanDefinitionCustomizers;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.support.AutowireCandidateQualifier;
import infra.beans.factory.support.BeanDefinitionReaderUtils;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.BeanNameGenerator;
import infra.core.env.Environment;
import infra.core.env.EnvironmentCapable;
import infra.core.env.StandardEnvironment;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;

/**
 * Convenient adapter for programmatic registration of bean classes.
 *
 * <p>This is an alternative to {@link ClassPathBeanDefinitionScanner}, applying
 * the same resolution of annotations but for explicitly registered classes only.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Phillip Webb
 * @author TODAY 2021/10/1 16:46
 * @since 4.0
 */
public class AnnotatedBeanDefinitionReader extends BeanDefinitionCustomizers {

  private final BeanDefinitionRegistry registry;

  private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

  private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

  private ConditionEvaluator conditionEvaluator;

  /**
   * Create a new {@code AnnotatedBeanDefinitionReader} for the given registry.
   * <p>If the registry is {@link EnvironmentCapable}, e.g. is an {@code ApplicationContext},
   * the {@link Environment} will be inherited, otherwise a new
   * {@link StandardEnvironment} will be created and used.
   *
   * @param registry the {@code BeanFactory} to load bean definitions into,
   * in the form of a {@code BeanDefinitionRegistry}
   * @see #AnnotatedBeanDefinitionReader(BeanDefinitionRegistry, Environment)
   * @see #setEnvironment(Environment)
   */
  public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
    this(registry, getOrCreateEnvironment(registry));
  }

  /**
   * Create a new {@code AnnotatedBeanDefinitionReader} for the given registry,
   * using the given {@link Environment}.
   *
   * @param registry the {@code BeanFactory} to load bean definitions into,
   * in the form of a {@code BeanDefinitionRegistry}
   * @param environment the {@code Environment} to use when evaluating bean definition
   * profiles.
   */
  public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
    Assert.notNull(registry, "BeanDefinitionRegistry is required");
    Assert.notNull(environment, "Environment is required");
    this.registry = registry;
    this.conditionEvaluator = new ConditionEvaluator(environment, null, registry);
    AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
  }

  /**
   * Get the BeanDefinitionRegistry that this reader operates on.
   */
  public final BeanDefinitionRegistry getRegistry() {
    return this.registry;
  }

  /**
   * Set the {@code Environment} to use when evaluating whether
   * {@link Conditional @Conditional}-annotated component classes should be registered.
   * <p>The default is a {@link StandardEnvironment}.
   *
   * @see #registerBean(Class, String, Class...)
   */
  public void setEnvironment(Environment environment) {
    this.conditionEvaluator = new ConditionEvaluator(environment, null, registry);
  }

  /**
   * Set the {@code BeanNameGenerator} to use for detected bean classes.
   * <p>The default is a {@link AnnotationBeanNameGenerator}.
   */
  public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
    this.beanNameGenerator =
            (beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
  }

  /**
   * Set the {@code ScopeMetadataResolver} to use for registered component classes.
   * <p>The default is an {@link AnnotationScopeMetadataResolver}.
   */
  public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
    this.scopeMetadataResolver =
            (scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
  }

  /**
   * Register one or more component classes to be processed.
   * <p>Calls to {@code register} are idempotent; adding the same
   * component class more than once has no additional effect.
   *
   * @param componentClasses one or more component classes,
   * e.g. {@link Configuration @Configuration} classes
   * @throws BeanDefinitionStoreException if registration failed
   */
  public void register(Class<?>... componentClasses) {
    for (Class<?> componentClass : componentClasses) {
      registerBean(componentClass);
    }
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations.
   *
   * @param beanClass the class of the bean
   * @throws BeanDefinitionStoreException if registration failed
   */
  public void registerBean(Class<?> beanClass) {
    doRegisterBean(beanClass, null, null, null, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations.
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean
   * (or {@code null} for generating a default bean name)
   * @throws BeanDefinitionStoreException if registration failed
   */
  public void registerBean(Class<?> beanClass, @Nullable String name) {
    doRegisterBean(beanClass, name, null, null, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations.
   *
   * @param beanClass the class of the bean
   * @param qualifiers specific qualifier annotations to consider,
   * in addition to qualifiers at the bean class level
   * @throws BeanDefinitionStoreException if registration failed
   */
  @SuppressWarnings("unchecked")
  public void registerBean(Class<?> beanClass, Class<? extends Annotation>... qualifiers) {
    doRegisterBean(beanClass, null, qualifiers, null, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations.
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean
   * (or {@code null} for generating a default bean name)
   * @param qualifiers specific qualifier annotations to consider,
   * in addition to qualifiers at the bean class level
   * @throws BeanDefinitionStoreException if registration failed
   */
  @SuppressWarnings("unchecked")
  public void registerBean(Class<?> beanClass, @Nullable String name,
          Class<? extends Annotation>... qualifiers) {

    doRegisterBean(beanClass, name, qualifiers, null, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations, using the given supplier for obtaining a new
   * instance (possibly declared as a lambda expression or method reference).
   *
   * @param beanClass the class of the bean
   * @param supplier a callback for creating an instance of the bean
   * (may be {@code null})
   * @throws BeanDefinitionStoreException if registration failed
   */
  public <T> void registerBean(Class<T> beanClass, @Nullable Supplier<T> supplier) {
    doRegisterBean(beanClass, null, null, supplier, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations, using the given supplier for obtaining a new
   * instance (possibly declared as a lambda expression or method reference).
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean
   * (or {@code null} for generating a default bean name)
   * @param supplier a callback for creating an instance of the bean
   * (may be {@code null})
   * @throws BeanDefinitionStoreException if registration failed
   */
  public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier) {
    doRegisterBean(beanClass, name, null, supplier, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations.
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean
   * (or {@code null} for generating a default bean name)
   * @param supplier a callback for creating an instance of the bean
   * (may be {@code null})
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   * @throws BeanDefinitionStoreException if registration failed
   */
  public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier,
          BeanDefinitionCustomizer... customizers) {

    doRegisterBean(beanClass, name, null, supplier, customizers);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations.
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean
   * @param qualifiers specific qualifier annotations to consider, if any,
   * in addition to qualifiers at the bean class level
   * @param supplier a callback for creating an instance of the bean
   * (may be {@code null})
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   * @throws BeanDefinitionStoreException if registration failed
   */
  private <T> void doRegisterBean(Class<T> beanClass, @Nullable String name,
          @Nullable Class<? extends Annotation>[] qualifiers, @Nullable Supplier<T> supplier,
          @Nullable BeanDefinitionCustomizer[] customizers) {

    var definition = new AnnotatedGenericBeanDefinition(beanClass);
    if (this.conditionEvaluator.shouldSkip(definition.getMetadata())) {
      return;
    }

    definition.setAttribute(ConfigurationClassUtils.CANDIDATE_ATTRIBUTE, Boolean.TRUE);
    definition.setInstanceSupplier(supplier);
    ScopeMetadata scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(definition);
    definition.setScope(scopeMetadata.getScopeName());
    String beanName = name != null ? name : beanNameGenerator.generateBeanName(definition, registry);

    AnnotationConfigUtils.processCommonDefinitionAnnotations(definition);
    if (qualifiers != null) {
      for (var qualifier : qualifiers) {
        if (Primary.class == qualifier) {
          definition.setPrimary(true);
        }
        else if (Fallback.class == qualifier) {
          definition.setFallback(true);
        }
        else if (Lazy.class == qualifier) {
          definition.setLazyInit(true);
        }
        else {
          definition.addQualifier(new AutowireCandidateQualifier(qualifier));
        }
      }
    }

    applyDynamicCustomizers(definition, customizers);

    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(definition, beanName);
    definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(
            scopeMetadata, definitionHolder, this.registry);
    BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
  }

  /**
   * Get the Environment from the given registry if possible, otherwise return a new
   * StandardEnvironment.
   */
  private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
    Assert.notNull(registry, "BeanDefinitionRegistry is required");
    if (registry instanceof EnvironmentCapable) {
      return ((EnvironmentCapable) registry).getEnvironment();
    }
    return new StandardEnvironment();
  }

  private void applyDynamicCustomizers(BeanDefinition definition,
          @Nullable BeanDefinitionCustomizer[] dynamicCustomizers) {

    // dynamic customize
    if (ObjectUtils.isNotEmpty(dynamicCustomizers)) {
      for (BeanDefinitionCustomizer dynamicCustomizer : dynamicCustomizers) {
        dynamicCustomizer.customize(definition);
      }
    }

    // static customize
    if (CollectionUtils.isNotEmpty(customizers)) {
      for (BeanDefinitionCustomizer customizer : customizers) {
        customizer.customize(definition);
      }
    }

  }
}
