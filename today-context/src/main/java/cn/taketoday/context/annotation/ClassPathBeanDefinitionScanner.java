/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.annotation;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionDefaults;
import cn.taketoday.beans.factory.support.BeanDefinitionReaderUtils;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.EnvironmentCapable;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * A bean definition scanner that detects bean candidates on the classpath,
 * registering corresponding bean definitions with a given registry ({@code BeanFactory}
 * or {@code ApplicationContext}).
 *
 * <p>Candidate classes are detected through configurable type filters. The
 * default filters include classes that are annotated with Framework's
 * {@link cn.taketoday.stereotype.Component @Component},
 * {@link cn.taketoday.stereotype.Repository @Repository},
 * {@link cn.taketoday.stereotype.Service @Service}, or
 * {@link cn.taketoday.stereotype.Controller @Controller} stereotype.
 *
 * <p>Also supports Jakarta EE's {@link jakarta.annotation.ManagedBean} and
 * JSR-330's {@link jakarta.inject.Named} annotations, if available.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotationConfigApplicationContext#scan
 * @see cn.taketoday.stereotype.Component
 * @see cn.taketoday.stereotype.Repository
 * @see cn.taketoday.stereotype.Service
 * @see cn.taketoday.stereotype.Controller
 * @since 4.0 2021/12/9 22:42
 */
public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {

  private final BeanDefinitionRegistry registry;

  private BeanDefinitionDefaults beanDefinitionDefaults = new BeanDefinitionDefaults();

  @Nullable
  private String[] autowireCandidatePatterns;

  private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

  private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

  private boolean includeAnnotationConfig = true;

  /**
   * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory.
   *
   * @param registry the {@code BeanFactory} to load bean definitions into, in the form
   * of a {@code BeanDefinitionRegistry}
   */
  public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
    this(registry, true);
  }

  /**
   * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory.
   * <p>If the passed-in bean factory does not only implement the
   * {@code BeanDefinitionRegistry} interface but also the {@code ResourceLoader}
   * interface, it will be used as default {@code ResourceLoader} as well. This will
   * usually be the case for {@link cn.taketoday.context.ApplicationContext}
   * implementations.
   * <p>If given a plain {@code BeanDefinitionRegistry}, the default {@code ResourceLoader}
   * will be a {@link cn.taketoday.core.io.PathMatchingPatternResourceLoader}.
   * <p>If the passed-in bean factory also implements {@link EnvironmentCapable} its
   * environment will be used by this reader.  Otherwise, the reader will initialize and
   * use a {@link cn.taketoday.core.env.StandardEnvironment}. All
   * {@code ApplicationContext} implementations are {@code EnvironmentCapable}, while
   * normal {@code BeanFactory} implementations are not.
   *
   * @param registry the {@code BeanFactory} to load bean definitions into, in the form
   * of a {@code BeanDefinitionRegistry}
   * @param useDefaultFilters whether to include the default filters for the
   * {@link cn.taketoday.stereotype.Component @Component},
   * {@link cn.taketoday.stereotype.Repository @Repository},
   * {@link cn.taketoday.stereotype.Service @Service}, and
   * {@link cn.taketoday.stereotype.Controller @Controller} stereotype annotations
   * @see #setResourceLoader
   * @see #setEnvironment
   */
  public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
    this(registry, useDefaultFilters, getOrCreateEnvironment(registry));
  }

  /**
   * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory and
   * using the given {@link Environment} when evaluating bean definition profile metadata.
   * <p>If the passed-in bean factory does not only implement the {@code
   * BeanDefinitionRegistry} interface but also the {@link ResourceLoader} interface, it
   * will be used as default {@code ResourceLoader} as well. This will usually be the
   * case for {@link cn.taketoday.context.ApplicationContext} implementations.
   * <p>If given a plain {@code BeanDefinitionRegistry}, the default {@code ResourceLoader}
   * will be a {@link cn.taketoday.core.io.PathMatchingPatternResourceLoader}.
   *
   * @param registry the {@code BeanFactory} to load bean definitions into, in the form
   * of a {@code BeanDefinitionRegistry}
   * @param useDefaultFilters whether to include the default filters for the
   * {@link cn.taketoday.stereotype.Component @Component},
   * {@link cn.taketoday.stereotype.Repository @Repository},
   * {@link cn.taketoday.stereotype.Service @Service}, and
   * {@link cn.taketoday.stereotype.Controller @Controller} stereotype annotations
   * @param environment the Framework {@link Environment} to use when evaluating bean
   * definition profile metadata
   * @see #setResourceLoader
   */
  public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry,
          boolean useDefaultFilters, Environment environment) {
    this(registry, useDefaultFilters, environment,
            registry instanceof ResourceLoader ? (ResourceLoader) registry : null);
  }

  /**
   * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory and
   * using the given {@link Environment} when evaluating bean definition profile metadata.
   *
   * @param registry the {@code BeanFactory} to load bean definitions into, in the form
   * of a {@code BeanDefinitionRegistry}
   * @param useDefaultFilters whether to include the default filters for the
   * {@link cn.taketoday.stereotype.Component @Component},
   * {@link cn.taketoday.stereotype.Repository @Repository},
   * {@link cn.taketoday.stereotype.Service @Service}, and
   * {@link cn.taketoday.stereotype.Controller @Controller} stereotype annotations
   * @param environment the Framework {@link Environment} to use when evaluating bean
   * definition profile metadata
   * @param resourceLoader the {@link ResourceLoader} to use
   */
  public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
          Environment environment, @Nullable ResourceLoader resourceLoader) {

    Assert.notNull(registry, "BeanDefinitionRegistry is required");
    this.registry = registry;

    if (useDefaultFilters) {
      registerDefaultFilters();
    }
    setEnvironment(environment);
    setResourceLoader(resourceLoader);
  }

  /**
   * Return the BeanDefinitionRegistry that this scanner operates on.
   */
  @Override
  public final BeanDefinitionRegistry getRegistry() {
    return this.registry;
  }

  /**
   * Set the defaults to use for detected beans.
   *
   * @see BeanDefinitionDefaults
   */
  public void setBeanDefinitionDefaults(@Nullable BeanDefinitionDefaults beanDefinitionDefaults) {
    this.beanDefinitionDefaults =
            beanDefinitionDefaults != null ? beanDefinitionDefaults : new BeanDefinitionDefaults();
  }

  /**
   * Return the defaults to use for detected beans (never {@code null}).
   */
  public BeanDefinitionDefaults getBeanDefinitionDefaults() {
    return this.beanDefinitionDefaults;
  }

  /**
   * Set the name-matching patterns for determining autowire candidates.
   *
   * @param autowireCandidatePatterns the patterns to match against
   */
  public void setAutowireCandidatePatterns(@Nullable String... autowireCandidatePatterns) {
    this.autowireCandidatePatterns = autowireCandidatePatterns;
  }

  /**
   * Set the BeanNameGenerator to use for detected bean classes.
   * <p>Default is a {@link AnnotationBeanNameGenerator}.
   */
  public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
    this.beanNameGenerator =
            beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE;
  }

  /**
   * Set the ScopeMetadataResolver to use for detected bean classes.
   * Note that this will override any custom "scopedProxyMode" setting.
   * <p>The default is an {@link AnnotationScopeMetadataResolver}.
   */
  public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
    this.scopeMetadataResolver =
            scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver();
  }

  /**
   * Specify the proxy behavior for non-singleton scoped beans.
   * Note that this will override any custom "scopeMetadataResolver" setting.
   * <p>The default is {@link ScopedProxyMode#NO}.
   *
   * @see #setScopeMetadataResolver
   */
  public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
    this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(scopedProxyMode);
  }

  /**
   * Specify whether to register annotation config post-processors.
   * <p>The default is to register the post-processors. Turn this off
   * to be able to ignore the annotations or to process them differently.
   */
  public void setIncludeAnnotationConfig(boolean includeAnnotationConfig) {
    this.includeAnnotationConfig = includeAnnotationConfig;
  }

  /**
   * Perform a scan within the specified base packages.
   *
   * @param basePackages the packages to check for annotated classes
   * @return number of beans registered
   */
  public int scan(String... basePackages) {
    int beanCountAtScanStart = this.registry.getBeanDefinitionCount();

    scan((UnaryOperator<BeanDefinitionHolder>) null, basePackages);

    // Register annotation config processors, if necessary.
    if (this.includeAnnotationConfig) {
      AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
    }

    return this.registry.getBeanDefinitionCount() - beanCountAtScanStart;
  }

  /**
   * Perform a scan within the specified base packages,
   * returning the registered bean definitions.
   * <p>This method does <i>not</i> register an annotation config processor
   * but rather leaves this up to the caller.
   *
   * @param basePackages the packages to check for annotated classes
   * @return set of beans registered if any for tooling registration purposes (never {@code null})
   */
  public Set<BeanDefinitionHolder> collectHolders(String... basePackages) {
    Assert.notEmpty(basePackages, "At least one base package must be specified");
    LinkedHashSet<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
    scan(beanDefinitions::add, basePackages);
    return beanDefinitions;
  }

  /**
   * Perform a scan within the specified base packages and consume the registered
   * bean definitions.
   * <p>This method does <i>not</i> register an annotation config processor
   * but rather leaves this up to the caller.
   *
   * @param consumer set of beans registered if any for tooling registration purposes
   * @param basePackages the packages to check for annotated classes
   */
  public void scan(@Nullable Consumer<BeanDefinitionHolder> consumer, String... basePackages) {
    if (consumer != null) {
      scan(holder -> {
        consumer.accept(holder);
        return holder;
      }, basePackages);
    }
    else {
      scan((UnaryOperator<BeanDefinitionHolder>) null, basePackages);
    }
  }

  /**
   * Perform a scan within the specified base packages and consume the registered
   * bean definitions.
   * <p>This method does <i>not</i> register an annotation config processor
   * but rather leaves this up to the caller.
   *
   * @param operator set of beans registered if any for tooling registration purposes
   * @param basePackages the packages to check for annotated classes
   */
  public void scan(@Nullable UnaryOperator<BeanDefinitionHolder> operator, String... basePackages) {
    for (String basePackage : basePackages) {
      try {
        scanCandidateComponents(basePackage, (metadataReader, factory) -> {
          ScannedGenericBeanDefinition candidate = new ScannedGenericBeanDefinition(metadataReader);
          Resource resource = metadataReader.getResource();
          candidate.setSource(resource);
          candidate.setResource(resource);

          ScopeMetadata scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(candidate);
          candidate.setScope(scopeMetadata.getScopeName());
          String beanName = beanNameGenerator.generateBeanName(candidate, registry);

          postProcessBeanDefinition(candidate, beanName);
          AnnotationConfigUtils.processCommonDefinitionAnnotations(candidate);

          if (checkCandidate(beanName, candidate)) {
            BeanDefinitionHolder holder = new BeanDefinitionHolder(candidate, beanName);
            holder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, holder, registry);
            if (operator != null) {
              holder = operator.apply(holder);
            }
            registerBeanDefinition(holder, registry);
          }
        });
      }
      catch (IOException ex) {
        throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
      }
    }
  }

  /**
   * Apply further settings to the given bean definition,
   * beyond the contents retrieved from scanning the component class.
   *
   * @param beanDefinition the scanned bean definition
   * @param beanName the generated bean name for the given bean
   */
  protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
    beanDefinition.applyDefaults(beanDefinitionDefaults);
    if (autowireCandidatePatterns != null) {
      beanDefinition.setAutowireCandidate(StringUtils.simpleMatch(autowireCandidatePatterns, beanName));
    }
  }

  /**
   * Register the specified bean with the given registry.
   * <p>Can be overridden in subclasses, e.g. to adapt the registration
   * process or to register further bean definitions for each scanned bean.
   *
   * @param definitionHolder the bean definition plus bean name for the bean
   * @param registry the BeanDefinitionRegistry to register the bean with
   */
  protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
    BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
  }

  /**
   * Check the given candidate's bean name, determining whether the corresponding
   * bean definition needs to be registered or conflicts with an existing definition.
   *
   * @param beanName the suggested name for the bean
   * @param beanDefinition the corresponding bean definition
   * @return {@code true} if the bean can be registered as-is;
   * {@code false} if it should be skipped because there is an
   * existing, compatible bean definition for the specified name
   * @throws IllegalStateException if an existing, incompatible bean definition
   * has been found for the specified name
   */
  protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
    if (!this.registry.containsBeanDefinition(beanName)) {
      return true;
    }
    BeanDefinition existingDef = this.registry.getBeanDefinition(beanName);
    BeanDefinition originatingDef = existingDef.getOriginatingBeanDefinition();
    if (originatingDef != null) {
      existingDef = originatingDef;
    }

    // Explicitly registered overriding bean?
    if (!(existingDef instanceof ScannedGenericBeanDefinition) &&
            (this.registry.isBeanDefinitionOverridable(beanName) || ObjectUtils.nullSafeEquals(
                    beanDefinition.getBeanClassName(), existingDef.getBeanClassName()))) {
      return false;
    }

    // Scanned same file or equivalent class twice?
    if (isCompatible(beanDefinition, existingDef)) {
      return false;
    }
    throw new ConflictingBeanDefinitionException("Annotation-specified bean name '" + beanName +
            "' for bean class [" + beanDefinition.getBeanClassName() + "] conflicts with existing, " +
            "non-compatible bean definition of same name and class [" + existingDef.getBeanClassName() + "]");
  }

  /**
   * Determine whether the given new bean definition is compatible with
   * the given existing bean definition.
   * <p>The default implementation considers them as compatible when the existing
   * bean definition comes from the same source or from a non-scanning source.
   *
   * @param newDef the new bean definition, originated from scanning
   * @param existingDef the existing bean definition, potentially an
   * explicitly defined one or a previously generated one from scanning
   * @return whether the definitions are considered as compatible, with the
   * new definition to be skipped in favor of the existing definition
   */
  protected boolean isCompatible(BeanDefinition newDef, BeanDefinition existingDef) {
    return (newDef.getSource() != null && newDef.getSource().equals(existingDef.getSource()))
            || newDef.equals(existingDef);
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

}
