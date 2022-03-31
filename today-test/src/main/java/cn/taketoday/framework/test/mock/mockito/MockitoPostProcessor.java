/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.test.mock.mockito;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.scope.ScopedProxyUtils;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.DependenciesBeanPostProcessor;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import cn.taketoday.beans.factory.config.InstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.beans.factory.support.DefaultBeanNameGenerator;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.ConfigurationClassPostProcessor;
import cn.taketoday.core.Conventions;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.junit4.Runner;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * A {@link BeanFactoryPostProcessor} used to register and inject
 * {@link MockBean @MockBeans} with the {@link ApplicationContext}. An initial set of
 * definitions can be passed to the processor with additional definitions being
 * automatically created from {@code @Configuration} classes that use
 * {@link MockBean @MockBean}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Andreas Neiser
 * @since 4.0
 */
public class MockitoPostProcessor implements InstantiationAwareBeanPostProcessor, BeanClassLoaderAware, DependenciesBeanPostProcessor,
        BeanFactoryAware, BeanFactoryPostProcessor, Ordered {

  private static final String BEAN_NAME = MockitoPostProcessor.class.getName();

  private static final String CONFIGURATION_CLASS_ATTRIBUTE = Conventions
          .getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");

  private static final BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();

  private final Set<Definition> definitions;

  private ClassLoader classLoader;

  private BeanFactory beanFactory;

  private final MockitoBeans mockitoBeans = new MockitoBeans();

  private final Map<Definition, String> beanNameRegistry = new HashMap<>();

  private final Map<Field, String> fieldRegistry = new HashMap<>();

  private final Map<String, SpyDefinition> spies = new HashMap<>();

  /**
   * Create a new {@link MockitoPostProcessor} instance with the given initial
   * definitions.
   *
   * @param definitions the initial definitions
   */
  public MockitoPostProcessor(Set<Definition> definitions) {
    this.definitions = definitions;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory,
            "Mock beans can only be used with a ConfigurableBeanFactory");
    this.beanFactory = beanFactory;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    Assert.isInstanceOf(BeanDefinitionRegistry.class, beanFactory,
            "@MockBean can only be used on bean factories that implement BeanDefinitionRegistry");
    postProcessBeanFactory(beanFactory, (BeanDefinitionRegistry) beanFactory);
  }

  private void postProcessBeanFactory(ConfigurableBeanFactory beanFactory, BeanDefinitionRegistry registry) {
    beanFactory.registerSingleton(MockitoBeans.class.getName(), this.mockitoBeans);
    DefinitionsParser parser = new DefinitionsParser(this.definitions);
    for (Class<?> configurationClass : getConfigurationClasses(beanFactory)) {
      parser.parse(configurationClass);
    }
    Set<Definition> definitions = parser.getDefinitions();
    for (Definition definition : definitions) {
      Field field = parser.getField(definition);
      register(beanFactory, registry, definition, field);
    }
  }

  private Set<Class<?>> getConfigurationClasses(ConfigurableBeanFactory beanFactory) {
    Set<Class<?>> configurationClasses = new LinkedHashSet<>();
    for (BeanDefinition beanDefinition : getConfigurationBeanDefinitions(beanFactory).values()) {
      configurationClasses.add(ClassUtils.resolveClassName(beanDefinition.getBeanClassName(), this.classLoader));
    }
    return configurationClasses;
  }

  private Map<String, BeanDefinition> getConfigurationBeanDefinitions(ConfigurableBeanFactory beanFactory) {
    Map<String, BeanDefinition> definitions = new LinkedHashMap<>();
    for (String beanName : beanFactory.getBeanDefinitionNames()) {
      BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
      if (definition.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE) != null) {
        definitions.put(beanName, definition);
      }
    }
    return definitions;
  }

  private void register(ConfigurableBeanFactory beanFactory, BeanDefinitionRegistry registry,
          Definition definition, Field field) {
    if (definition instanceof MockDefinition) {
      registerMock(beanFactory, registry, (MockDefinition) definition, field);
    }
    else if (definition instanceof SpyDefinition) {
      registerSpy(beanFactory, registry, (SpyDefinition) definition, field);
    }
  }

  private void registerMock(ConfigurableBeanFactory beanFactory, BeanDefinitionRegistry registry,
          MockDefinition definition, Field field) {
    RootBeanDefinition beanDefinition = createBeanDefinition(definition);
    String beanName = getBeanName(beanFactory, registry, definition, beanDefinition);
    String transformedBeanName = BeanFactoryUtils.transformedBeanName(beanName);
    if (registry.containsBeanDefinition(transformedBeanName)) {
      BeanDefinition existing = registry.getBeanDefinition(transformedBeanName);
      copyBeanDefinitionDetails(existing, beanDefinition);
      registry.removeBeanDefinition(transformedBeanName);
    }
    registry.registerBeanDefinition(transformedBeanName, beanDefinition);
    Object mock = definition.createMock(beanName + " bean");
    beanFactory.registerSingleton(transformedBeanName, mock);
    this.mockitoBeans.add(mock);
    this.beanNameRegistry.put(definition, beanName);
    if (field != null) {
      this.fieldRegistry.put(field, beanName);
    }
  }

  private RootBeanDefinition createBeanDefinition(MockDefinition mockDefinition) {
    RootBeanDefinition definition = new RootBeanDefinition(mockDefinition.getTypeToMock().resolve());
    definition.setTargetType(mockDefinition.getTypeToMock());
    if (mockDefinition.getQualifier() != null) {
      mockDefinition.getQualifier().applyTo(definition);
    }
    return definition;
  }

  private String getBeanName(ConfigurableBeanFactory beanFactory, BeanDefinitionRegistry registry,
          MockDefinition mockDefinition, RootBeanDefinition beanDefinition) {
    if (StringUtils.isNotEmpty(mockDefinition.getName())) {
      return mockDefinition.getName();
    }
    Set<String> existingBeans = getExistingBeans(beanFactory, mockDefinition.getTypeToMock(),
            mockDefinition.getQualifier());
    if (existingBeans.isEmpty()) {
      return MockitoPostProcessor.beanNameGenerator.generateBeanName(beanDefinition, registry);
    }
    if (existingBeans.size() == 1) {
      return existingBeans.iterator().next();
    }
    String primaryCandidate = determinePrimaryCandidate(registry, existingBeans, mockDefinition.getTypeToMock());
    if (primaryCandidate != null) {
      return primaryCandidate;
    }
    throw new IllegalStateException("Unable to register mock bean " + mockDefinition.getTypeToMock()
            + " expected a single matching bean to replace but found " + existingBeans);
  }

  private void copyBeanDefinitionDetails(BeanDefinition from, RootBeanDefinition to) {
    to.setPrimary(from.isPrimary());
  }

  private void registerSpy(ConfigurableBeanFactory beanFactory, BeanDefinitionRegistry registry,
          SpyDefinition spyDefinition, Field field) {
    Set<String> existingBeans = getExistingBeans(beanFactory, spyDefinition.getTypeToSpy(),
            spyDefinition.getQualifier());
    if (ObjectUtils.isEmpty(existingBeans)) {
      createSpy(registry, spyDefinition, field);
    }
    else {
      registerSpies(registry, spyDefinition, field, existingBeans);
    }
  }

  private Set<String> getExistingBeans(ConfigurableBeanFactory beanFactory, ResolvableType type,
          QualifierDefinition qualifier) {
    Set<String> candidates = new TreeSet<>();
    for (String candidate : getExistingBeans(beanFactory, type)) {
      if (qualifier == null || qualifier.matches(beanFactory, candidate)) {
        candidates.add(candidate);
      }
    }
    return candidates;
  }

  private Set<String> getExistingBeans(ConfigurableBeanFactory beanFactory, ResolvableType type) {
    Set<String> beans = new LinkedHashSet<>(beanFactory.getBeanNamesForType(type, true, false));
    String typeName = type.resolve(Object.class).getName();
    for (String beanName : beanFactory.getBeanNamesForType(FactoryBean.class, true, false)) {
      beanName = BeanFactoryUtils.transformedBeanName(beanName);
      BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
      if (typeName.equals(beanDefinition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE))) {
        beans.add(beanName);
      }
    }
    beans.removeIf(this::isScopedTarget);
    return beans;
  }

  private boolean isScopedTarget(String beanName) {
    try {
      return ScopedProxyUtils.isScopedTarget(beanName);
    }
    catch (Throwable ex) {
      return false;
    }
  }

  private void createSpy(BeanDefinitionRegistry registry, SpyDefinition spyDefinition, Field field) {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(spyDefinition.getTypeToSpy().resolve());
    String beanName = MockitoPostProcessor.beanNameGenerator.generateBeanName(beanDefinition, registry);
    registry.registerBeanDefinition(beanName, beanDefinition);
    registerSpy(spyDefinition, field, beanName);
  }

  private void registerSpies(BeanDefinitionRegistry registry, SpyDefinition spyDefinition, Field field,
          Collection<String> existingBeans) {
    try {
      String beanName = determineBeanName(existingBeans, spyDefinition, registry);
      registerSpy(spyDefinition, field, beanName);
    }
    catch (RuntimeException ex) {
      throw new IllegalStateException("Unable to register spy bean " + spyDefinition.getTypeToSpy(), ex);
    }
  }

  private String determineBeanName(Collection<String> existingBeans, SpyDefinition definition,
          BeanDefinitionRegistry registry) {
    if (StringUtils.hasText(definition.getName())) {
      return definition.getName();
    }
    if (existingBeans.size() == 1) {
      return existingBeans.iterator().next();
    }
    return determinePrimaryCandidate(registry, existingBeans, definition.getTypeToSpy());
  }

  private String determinePrimaryCandidate(BeanDefinitionRegistry registry, Collection<String> candidateBeanNames,
          ResolvableType type) {
    String primaryBeanName = null;
    for (String candidateBeanName : candidateBeanNames) {
      BeanDefinition beanDefinition = registry.getBeanDefinition(candidateBeanName);
      if (beanDefinition.isPrimary()) {
        if (primaryBeanName != null) {
          throw new NoUniqueBeanDefinitionException(type.resolve(), candidateBeanNames.size(),
                  "more than one 'primary' bean found among candidates: "
                          + Collections.singletonList(candidateBeanNames));
        }
        primaryBeanName = candidateBeanName;
      }
    }
    return primaryBeanName;
  }

  private void registerSpy(SpyDefinition definition, Field field, String beanName) {
    this.spies.put(beanName, definition);
    this.beanNameRegistry.put(definition, beanName);
    if (field != null) {
      this.fieldRegistry.put(field, beanName);
    }
  }

  protected final Object createSpyIfNecessary(Object bean, String beanName) throws BeansException {
    SpyDefinition definition = this.spies.get(beanName);
    if (definition != null) {
      bean = definition.createSpy(beanName, bean);
    }
    return bean;
  }

  @Nullable
  @Override
  public PropertyValues processDependencies(@Nullable PropertyValues pvs, Object bean, String beanName) {
    ReflectionUtils.doWithFields(bean.getClass(), (field) -> postProcessField(bean, field));
    return pvs;
  }

  private void postProcessField(Object bean, Field field) {
    String beanName = this.fieldRegistry.get(field);
    if (StringUtils.hasText(beanName)) {
      inject(field, bean, beanName);
    }
  }

  void inject(Field field, Object target, Definition definition) {
    String beanName = this.beanNameRegistry.get(definition);
    Assert.state(StringUtils.isNotEmpty(beanName), () -> "No bean found for definition " + definition);
    inject(field, target, beanName);
  }

  private void inject(Field field, Object target, String beanName) {
    try {
      field.setAccessible(true);
      Object existingValue = ReflectionUtils.getField(field, target);
      Object bean = this.beanFactory.getBean(beanName, field.getType());
      if (existingValue == bean) {
        return;
      }
      Assert.state(existingValue == null, () -> "The existing value '" + existingValue + "' of field '" + field
              + "' is not the same as the new value '" + bean + "'");
      ReflectionUtils.setField(field, target, bean);
    }
    catch (Throwable ex) {
      throw new BeanCreationException("Could not inject field: " + field, ex);
    }
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 10;
  }

  /**
   * Register the processor with a {@link BeanDefinitionRegistry}. Not required when
   * using the {@link Runner} as registration is automatic.
   *
   * @param registry the bean definition registry
   */
  public static void register(BeanDefinitionRegistry registry) {
    register(registry, null);
  }

  /**
   * Register the processor with a {@link BeanDefinitionRegistry}. Not required when
   * using the {@link Runner} as registration is automatic.
   *
   * @param registry the bean definition registry
   * @param definitions the initial mock/spy definitions
   */
  public static void register(BeanDefinitionRegistry registry, Set<Definition> definitions) {
    register(registry, MockitoPostProcessor.class, definitions);
  }

  /**
   * Register the processor with a {@link BeanDefinitionRegistry}. Not required when
   * using the {@link Runner} as registration is automatic.
   *
   * @param registry the bean definition registry
   * @param postProcessor the post processor class to register
   * @param definitions the initial mock/spy definitions
   */
  @SuppressWarnings("unchecked")
  public static void register(BeanDefinitionRegistry registry, Class<? extends MockitoPostProcessor> postProcessor,
          Set<Definition> definitions) {
    SpyPostProcessor.register(registry);
    BeanDefinition definition = getOrAddBeanDefinition(registry, postProcessor);
    ValueHolder constructorArg = definition.getConstructorArgumentValues().getIndexedArgumentValue(0, Set.class);
    Set<Definition> existing = (Set<Definition>) constructorArg.getValue();
    if (definitions != null) {
      existing.addAll(definitions);
    }
  }

  private static BeanDefinition getOrAddBeanDefinition(BeanDefinitionRegistry registry,
          Class<? extends MockitoPostProcessor> postProcessor) {
    if (!registry.containsBeanDefinition(BEAN_NAME)) {
      RootBeanDefinition definition = new RootBeanDefinition(postProcessor);
      definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      ConstructorArgumentValues constructorArguments = definition.getConstructorArgumentValues();
      constructorArguments.addIndexedArgumentValue(0, new LinkedHashSet<MockDefinition>());
      registry.registerBeanDefinition(BEAN_NAME, definition);
      return definition;
    }
    return registry.getBeanDefinition(BEAN_NAME);
  }

  /**
   * {@link BeanPostProcessor} to handle {@link SpyBean} definitions. Registered as a
   * separate processor so that it can be ordered above AOP post processors.
   */
  static class SpyPostProcessor implements SmartInstantiationAwareBeanPostProcessor, InitializationBeanPostProcessor, PriorityOrdered {

    private static final String BEAN_NAME = SpyPostProcessor.class.getName();

    private final Map<String, Object> earlySpyReferences = new ConcurrentHashMap<>(16);

    private final MockitoPostProcessor mockitoPostProcessor;

    SpyPostProcessor(MockitoPostProcessor mockitoPostProcessor) {
      this.mockitoPostProcessor = mockitoPostProcessor;
    }

    @Override
    public int getOrder() {
      return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
      if (bean instanceof FactoryBean) {
        return bean;
      }
      this.earlySpyReferences.put(getCacheKey(bean, beanName), bean);
      return this.mockitoPostProcessor.createSpyIfNecessary(bean, beanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      if (bean instanceof FactoryBean) {
        return bean;
      }
      if (this.earlySpyReferences.remove(getCacheKey(bean, beanName)) != bean) {
        return this.mockitoPostProcessor.createSpyIfNecessary(bean, beanName);
      }
      return bean;
    }

    private String getCacheKey(Object bean, String beanName) {
      return StringUtils.isNotEmpty(beanName) ? beanName : bean.getClass().getName();
    }

    static void register(BeanDefinitionRegistry registry) {
      if (!registry.containsBeanDefinition(BEAN_NAME)) {
        RootBeanDefinition definition = new RootBeanDefinition(SpyPostProcessor.class);
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        ConstructorArgumentValues constructorArguments = definition.getConstructorArgumentValues();
        constructorArguments.addIndexedArgumentValue(0,
                new RuntimeBeanReference(MockitoPostProcessor.BEAN_NAME));
        registry.registerBeanDefinition(BEAN_NAME, definition);
      }
    }

  }

}
