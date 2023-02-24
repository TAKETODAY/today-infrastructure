/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyEditorRegistry;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.properties.bind.AbstractBindHandler;
import cn.taketoday.context.properties.bind.BindContext;
import cn.taketoday.context.properties.bind.BindHandler;
import cn.taketoday.context.properties.bind.BindResult;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Bindable.BindRestriction;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.bind.BoundPropertiesTrackingBindHandler;
import cn.taketoday.context.properties.bind.PropertySourcesPlaceholdersResolver;
import cn.taketoday.context.properties.bind.handler.IgnoreErrorsBindHandler;
import cn.taketoday.context.properties.bind.handler.IgnoreTopLevelConverterNotFoundBindHandler;
import cn.taketoday.context.properties.bind.handler.NoUnboundElementsBindHandler;
import cn.taketoday.context.properties.bind.validation.ValidationBindHandler;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.context.properties.source.UnboundElementsSourceFilter;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.validation.Validator;
import cn.taketoday.validation.annotation.Validated;

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

  private static final String BEAN_NAME = "cn.taketoday.context.internalConfigurationPropertiesBinder";

  private static final String VALIDATOR_BEAN_NAME = EnableConfigurationProperties.VALIDATOR_BEAN_NAME;

  private final ApplicationContext applicationContext;

  private final PropertySources propertySources;

  @Nullable
  private final Validator configurationPropertiesValidator;

  private final boolean jsr303Present;

  @Nullable
  private volatile Validator jsr303Validator;

  @Nullable
  private volatile Binder binder;

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

  private ArrayList<Validator> getValidators(Bindable<?> target) {
    ArrayList<Validator> validators = new ArrayList<>(3);
    if (configurationPropertiesValidator != null) {
      validators.add(configurationPropertiesValidator);
    }
    if (jsr303Present && target.getAnnotation(Validated.class) != null) {
      validators.add(getJsr303Validator());
    }
    if (target.getValue() != null && target.getValue().get() instanceof Validator validator) {
      validators.add(validator);
    }
    return validators;
  }

  private Validator getJsr303Validator() {
    Validator jsr303Validator = this.jsr303Validator;

    if (jsr303Validator == null) {
      this.jsr303Validator = jsr303Validator = new ConfigurationPropertiesJsr303Validator(this.applicationContext);
    }
    return jsr303Validator;
  }

  private List<ConfigurationPropertiesBindHandlerAdvisor> getBindHandlerAdvisors() {
    return this.applicationContext.getBeanProvider(
            ConfigurationPropertiesBindHandlerAdvisor.class).orderedStream().collect(Collectors.toList());
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
    if (this.applicationContext instanceof ConfigurableApplicationContext) {
      return ((ConfigurableApplicationContext) this.applicationContext).getBeanFactory()::copyRegisteredEditorsTo;
    }
    return null;
  }

  static void register(BeanDefinitionRegistry registry) {
    if (!registry.containsBeanDefinition(BEAN_NAME)) {
      var definition = BeanDefinitionBuilder.rootBeanDefinition(
              ConfigurationPropertiesBinderFactory.class).getBeanDefinition();
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
             ? target.withBindRestrictions(BindRestriction.NO_DIRECT_PROPERTY) : target;
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

}
