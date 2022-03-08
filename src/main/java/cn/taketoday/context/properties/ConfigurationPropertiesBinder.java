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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyEditorRegistry;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
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

  private static final String FACTORY_BEAN_NAME = "cn.taketoday.context.internalConfigurationPropertiesBinderFactory";

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

  ConfigurationPropertiesBinder(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    this.propertySources = new PropertySourcesDeducer(applicationContext).getPropertySources();
    this.configurationPropertiesValidator = getConfigurationPropertiesValidator(applicationContext);
    this.jsr303Present = ConfigurationPropertiesJsr303Validator.isJsr303Present(applicationContext);
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
    List<Validator> validators = getValidators(target);
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
    BoundConfigurationProperties bound = BoundConfigurationProperties.get(this.applicationContext);
    return (bound != null)
           ? new IgnoreTopLevelConverterNotFoundBindHandler(new BoundPropertiesTrackingBindHandler(bound::add))
           : new IgnoreTopLevelConverterNotFoundBindHandler();
  }

  private List<Validator> getValidators(Bindable<?> target) {
    List<Validator> validators = new ArrayList<>(3);
    if (this.configurationPropertiesValidator != null) {
      validators.add(this.configurationPropertiesValidator);
    }
    if (this.jsr303Present && target.getAnnotation(Validated.class) != null) {
      validators.add(getJsr303Validator());
    }
    if (target.getValue() != null && target.getValue().get() instanceof Validator) {
      validators.add((Validator) target.getValue().get());
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
    return this.applicationContext.getObjectSupplier(
            ConfigurationPropertiesBindHandlerAdvisor.class).orderedStream().collect(Collectors.toList());
  }

  private Binder getBinder() {
    Binder binder = this.binder;
    if (binder == null) {
      binder = new Binder(getConfigurationPropertySources(), getPropertySourcesPlaceholdersResolver(),
              getConversionServices(), getPropertyEditorInitializer(), null,
              ConfigurationPropertiesBindConstructorProvider.INSTANCE);
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
    if (!registry.containsBeanDefinition(FACTORY_BEAN_NAME)) {
      BeanDefinition definition = BeanDefinitionBuilder
              .rootBeanDefinition(ConfigurationPropertiesBinder.Factory.class).getBeanDefinition();
      definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      registry.registerBeanDefinition(ConfigurationPropertiesBinder.FACTORY_BEAN_NAME, definition);
    }
    if (!registry.containsBeanDefinition(BEAN_NAME)) {
      BeanDefinition definition = BeanDefinitionBuilder.rootBeanDefinition(ConfigurationPropertiesBinder.class,
                      () -> ((BeanFactory) registry).getBean(FACTORY_BEAN_NAME, ConfigurationPropertiesBinder.Factory.class).create()
              )
              .getBeanDefinition();

      definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      registry.registerBeanDefinition(ConfigurationPropertiesBinder.BEAN_NAME, definition);
    }
  }

  static ConfigurationPropertiesBinder get(BeanFactory beanFactory) {
    return BeanFactoryUtils.requiredBean(beanFactory, BEAN_NAME, ConfigurationPropertiesBinder.class);
  }

  /**
   * Factory bean used to create the {@link ConfigurationPropertiesBinder}. The bean
   * needs to be {@link ApplicationContextAware} since we can't directly inject an
   * {@link ApplicationContext} into the constructor without causing eager
   * {@link FactoryBean} initialization.
   */
  static class Factory implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      this.applicationContext = applicationContext;
    }

    ConfigurationPropertiesBinder create() {
      return new ConfigurationPropertiesBinder(this.applicationContext);
    }

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

}
