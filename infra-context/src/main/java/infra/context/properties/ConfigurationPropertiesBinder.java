/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.properties;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import infra.beans.BeansException;
import infra.beans.PropertyEditorRegistry;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.ConfigurableApplicationContext;
import infra.context.properties.bind.AbstractBindHandler;
import infra.context.properties.bind.BindContext;
import infra.context.properties.bind.BindHandler;
import infra.context.properties.bind.BindResult;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.context.properties.bind.BoundPropertiesTrackingBindHandler;
import infra.context.properties.bind.PropertySourcesPlaceholdersResolver;
import infra.context.properties.bind.handler.IgnoreErrorsBindHandler;
import infra.context.properties.bind.handler.IgnoreTopLevelConverterNotFoundBindHandler;
import infra.context.properties.bind.handler.NoUnboundElementsBindHandler;
import infra.context.properties.bind.validation.ValidationBindHandler;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.context.properties.source.ConfigurationPropertySources;
import infra.context.properties.source.UnboundElementsSourceFilter;
import infra.core.annotation.MergedAnnotations;
import infra.core.conversion.ConversionService;
import infra.core.env.PropertySources;
import infra.lang.Assert;
import infra.validation.Errors;
import infra.validation.Validator;
import infra.validation.annotation.Validated;

/**
 * Internal class used by the {@link ConfigurationPropertiesBindingPostProcessor} to
 * handle the actual {@link ConfigurationProperties @ConfigurationProperties} binding.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigurationPropertiesBinder {

  private static final String BEAN_NAME = "infra.context.internalConfigurationPropertiesBinder";

  private static final String VALIDATOR_BEAN_NAME = EnableConfigurationProperties.VALIDATOR_BEAN_NAME;

  private final ApplicationContext applicationContext;

  private final PropertySources propertySources;

  @Nullable
  private final Validator configurationPropertiesValidator;

  private final boolean jsr303Present;

  @Nullable
  private volatile Binder binder;

  @Nullable
  private volatile List<ConfigurationPropertiesBindHandlerAdvisor> bindHandlerAdvisors;

  ConfigurationPropertiesBinder(ApplicationContext context) {
    this.applicationContext = context;
    this.propertySources = new PropertySourcesDeducer(context).getPropertySources();
    this.configurationPropertiesValidator = getConfigurationPropertiesValidator(context);
    this.jsr303Present = ConfigurationPropertiesJsr303Validator.isJsr303Present(context);
  }

  BindResult<?> bind(ConfigurationPropertiesBean propertiesBean) {
    Bindable<?> target = propertiesBean.asBindTarget();
    ConfigurationProperties annotation = propertiesBean.getAnnotation();
    BindHandler bindHandler = getBindHandler(target, annotation);
    return getBinder().bind(annotation.prefix(), target, bindHandler);
  }

  Object bindOrCreate(ConfigurationPropertiesBean propertiesBean) {
    Bindable<?> target = propertiesBean.asBindTarget();
    ConfigurationProperties annotation = propertiesBean.getAnnotation();
    BindHandler bindHandler = getBindHandler(target, annotation);
    return getBinder().bindOrCreate(annotation.prefix(), target, bindHandler);
  }

  @Nullable
  private Validator getConfigurationPropertiesValidator(ApplicationContext applicationContext) {
    if (applicationContext.containsBean(VALIDATOR_BEAN_NAME)) {
      return applicationContext.getBean(VALIDATOR_BEAN_NAME, Validator.class);
    }
    return null;
  }

  private <T> BindHandler getBindHandler(Bindable<T> target, ConfigurationProperties annotation) {
    var validators = getValidators(target);
    BindHandler handler = getHandler();
    handler = new ConfigurationPropertiesBindHandler(handler);
    if (annotation.ignoreInvalidFields()) {
      handler = new IgnoreErrorsBindHandler(handler);
    }
    if (!annotation.ignoreUnknownFields()) {
      UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
      handler = new NoUnboundElementsBindHandler(handler, filter);
    }
    if (!validators.isEmpty()) {
      handler = new ValidationBindHandler(handler, validators.toArray(new Validator[0]));
    }
    for (ConfigurationPropertiesBindHandlerAdvisor advisor : getBindHandlerAdvisors()) {
      handler = advisor.apply(handler);
    }
    return handler;
  }

  private IgnoreTopLevelConverterNotFoundBindHandler getHandler() {
    BoundConfigurationProperties bound = BoundConfigurationProperties.get(applicationContext);
    return bound != null
            ? new IgnoreTopLevelConverterNotFoundBindHandler(new BoundPropertiesTrackingBindHandler(bound::add))
            : new IgnoreTopLevelConverterNotFoundBindHandler();
  }

  @SuppressWarnings("NullAway")
  private ArrayList<Validator> getValidators(Bindable<?> target) {
    ArrayList<Validator> validators = new ArrayList<>(3);
    if (configurationPropertiesValidator != null) {
      validators.add(configurationPropertiesValidator);
    }
    if (jsr303Present && target.getAnnotation(Validated.class) != null) {
      validators.add(getJsr303Validator(target.getType().resolve()));
    }
    Validator selfValidator = getSelfValidator(target);
    if (selfValidator != null) {
      validators.add(selfValidator);
    }
    return validators;
  }

  @Nullable
  private Validator getSelfValidator(Bindable<?> target) {
    if (target.getValue() != null) {
      Object value = target.getValue().get();
      return (value instanceof Validator validator) ? validator : null;
    }
    Class<?> type = target.getType().resolve();
    if (type != null && Validator.class.isAssignableFrom(type)) {
      return new SelfValidatingConstructorBoundBindableValidator(type);
    }
    return null;
  }

  private Validator getJsr303Validator(Class<?> type) {
    return new ConfigurationPropertiesJsr303Validator(this.applicationContext, type);
  }

  private List<ConfigurationPropertiesBindHandlerAdvisor> getBindHandlerAdvisors() {
    List<ConfigurationPropertiesBindHandlerAdvisor> bindHandlerAdvisors = this.bindHandlerAdvisors;
    if (bindHandlerAdvisors == null) {
      bindHandlerAdvisors = this.applicationContext
              .getBeanProvider(ConfigurationPropertiesBindHandlerAdvisor.class)
              .orderedStream()
              .toList();
      this.bindHandlerAdvisors = bindHandlerAdvisors;
    }
    return bindHandlerAdvisors;
  }

  private Binder getBinder() {
    Binder binder = this.binder;
    if (binder == null) {
      binder = new Binder(getConfigurationPropertySources(), getPropertySourcesPlaceholdersResolver(),
              getConversionServices(), getPropertyEditorInitializer(), null,
              null);
      this.binder = binder;
    }
    return binder;
  }

  private Iterable<ConfigurationPropertySource> getConfigurationPropertySources() {
    return ConfigurationPropertySources.from(this.propertySources);
  }

  private PropertySourcesPlaceholdersResolver getPropertySourcesPlaceholdersResolver() {
    return new PropertySourcesPlaceholdersResolver(this.propertySources);
  }

  @Nullable
  private List<ConversionService> getConversionServices() {
    return new ConversionServiceDeducer(applicationContext).getConversionServices();
  }

  @Nullable
  private Consumer<PropertyEditorRegistry> getPropertyEditorInitializer() {
    if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
      return configurableContext.getBeanFactory()::copyRegisteredEditorsTo;
    }
    return null;
  }

  static void register(BeanDefinitionRegistry registry) {
    if (!registry.containsBeanDefinition(BEAN_NAME)) {
      BeanDefinition definition = BeanDefinitionBuilder
              .rootBeanDefinition(ConfigurationPropertiesBinderFactory.class)
              .getBeanDefinition();
      definition.setEnableDependencyInjection(false);
      definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      registry.registerBeanDefinition(ConfigurationPropertiesBinder.BEAN_NAME, definition);
    }
  }

  static ConfigurationPropertiesBinder get(BeanFactory beanFactory) {
    return beanFactory.getBean(BEAN_NAME, ConfigurationPropertiesBinder.class);
  }

  /**
   * {@link BindHandler} to deal with
   * {@link ConfigurationProperties @ConfigurationProperties} concerns.
   */
  private static class ConfigurationPropertiesBindHandler extends AbstractBindHandler {

    ConfigurationPropertiesBindHandler(BindHandler handler) {
      super(handler);
    }

    @Override
    public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
      return isConfigurationProperties(target.getType().resolve())
              ? target.withBindRestrictions(Bindable.BindRestriction.NO_DIRECT_PROPERTY) : target;
    }

    private boolean isConfigurationProperties(@Nullable Class<?> target) {
      return target != null && MergedAnnotations.from(target).isPresent(ConfigurationProperties.class);
    }

  }

  /**
   * {@link FactoryBean} to create the {@link ConfigurationPropertiesBinder}.
   */
  static class ConfigurationPropertiesBinderFactory
          implements FactoryBean<ConfigurationPropertiesBinder>, ApplicationContextAware {

    @Nullable
    private ConfigurationPropertiesBinder binder;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      if (binder == null) {
        binder = new ConfigurationPropertiesBinder(applicationContext);
      }
    }

    @Override
    public Class<?> getObjectType() {
      return ConfigurationPropertiesBinder.class;
    }

    @Override
    public ConfigurationPropertiesBinder getObject() throws Exception {
      Assert.state(this.binder != null, "Binder was not created due to missing setApplicationContext call");
      return this.binder;
    }

  }

  /**
   * A {@code Validator} for a constructor-bound {@code Bindable} where the type being
   * bound is itself a {@code Validator} implementation.
   */
  static class SelfValidatingConstructorBoundBindableValidator implements Validator {

    private final Class<?> type;

    SelfValidatingConstructorBoundBindableValidator(Class<?> type) {
      this.type = type;
    }

    @Override
    public boolean supports(Class<?> candidate) {
      return candidate.isAssignableFrom(this.type);
    }

    @Override
    public void validate(Object target, Errors errors) {
      ((Validator) target).validate(target, errors);
    }

  }

}
