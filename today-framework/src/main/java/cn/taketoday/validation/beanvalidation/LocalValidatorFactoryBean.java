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

package cn.taketoday.validation.beanvalidation;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.MessageSource;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ReflectionUtils;
import jakarta.validation.ClockProvider;
import jakarta.validation.Configuration;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.ValidationProviderResolver;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorContext;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.bootstrap.GenericBootstrap;
import jakarta.validation.bootstrap.ProviderSpecificBootstrap;

/**
 * This is the central class for {@code jakarta.validation} (JSR-303) setup in a Framework
 * application context: It bootstraps a {@code jakarta.validation.ValidationFactory} and
 * exposes it through the Framework {@link cn.taketoday.validation.Validator} interface
 * as well as through the JSR-303 {@link Validator} interface and the
 * {@link ValidatorFactory} interface itself.
 *
 * <p>When talking to an instance of this bean through the Framework or JSR-303 Validator interfaces,
 * you'll be talking to the default Validator of the underlying ValidatorFactory. This is very
 * convenient in that you don't have to perform yet another call on the factory, assuming that
 * you will almost always use the default Validator anyway. This can also be injected directly
 * into any target dependency of type {@link cn.taketoday.validation.Validator}!
 *
 * <p>This class is also being used by Framework's MVC configuration namespace, in case of the
 * {@code jakarta.validation} API being present but no explicit Validator having been configured.
 *
 * @author Juergen Hoeller
 * @see ValidatorFactory
 * @see Validator
 * @see Validation#buildDefaultValidatorFactory()
 * @see ValidatorFactory#getValidator()
 * @since 4.0
 */
public class LocalValidatorFactoryBean extends ValidatorAdapter
        implements ValidatorFactory, ApplicationContextAware, InitializingBean, DisposableBean {

  @SuppressWarnings("rawtypes")
  @Nullable
  private Class providerClass;

  @Nullable
  private ValidationProviderResolver validationProviderResolver;

  @Nullable
  private MessageInterpolator messageInterpolator;

  @Nullable
  private TraversableResolver traversableResolver;

  @Nullable
  private ConstraintValidatorFactory constraintValidatorFactory;

  @Nullable
  private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  @Nullable
  private Resource[] mappingLocations;

  private final Map<String, String> validationPropertyMap = new HashMap<>();

  @Nullable
  private ApplicationContext applicationContext;

  @Nullable
  private ValidatorFactory validatorFactory;

  /**
   * Specify the desired provider class, if any.
   * <p>If not specified, JSR-303's default search mechanism will be used.
   *
   * @see Validation#byProvider(Class)
   * @see Validation#byDefaultProvider()
   */
  @SuppressWarnings("rawtypes")
  public void setProviderClass(Class providerClass) {
    this.providerClass = providerClass;
  }

  /**
   * Specify a JSR-303 {@link ValidationProviderResolver} for bootstrapping the
   * provider of choice, as an alternative to {@code META-INF} driven resolution.
   */
  public void setValidationProviderResolver(ValidationProviderResolver validationProviderResolver) {
    this.validationProviderResolver = validationProviderResolver;
  }

  /**
   * Specify a custom MessageInterpolator to use for this ValidatorFactory
   * and its exposed default Validator.
   */
  public void setMessageInterpolator(@Nullable MessageInterpolator messageInterpolator) {
    this.messageInterpolator = messageInterpolator;
  }

  /**
   * Specify a custom Framework MessageSource for resolving validation messages,
   * instead of relying on JSR-303's default "ValidationMessages.properties" bundle
   * in the classpath. This may refer to a Framework context's shared "messageSource" bean,
   * or to some special MessageSource setup for validation purposes only.
   * <p><b>NOTE:</b> This feature requires Hibernate Validator 4.3 or higher on the classpath.
   * You may nevertheless use a different validation provider but Hibernate Validator's
   * {@link ResourceBundleMessageInterpolator} class must be accessible during configuration.
   * <p>Specify either this property or {@link #setMessageInterpolator "messageInterpolator"},
   * not both. If you would like to build a custom MessageInterpolator, consider deriving from
   * Hibernate Validator's {@link ResourceBundleMessageInterpolator} and passing in a
   * Framework-based {@code ResourceBundleLocator} when constructing your interpolator.
   * <p>In order for Hibernate's default validation messages to be resolved still, your
   * {@link MessageSource} must be configured for optional resolution (usually the default).
   * In particular, the {@code MessageSource} instance specified here should not apply
   * {@link cn.taketoday.context.support.AbstractMessageSource#setUseCodeAsDefaultMessage
   * "useCodeAsDefaultMessage"} behavior. Please double-check your setup accordingly.
   *
   * @see ResourceBundleMessageInterpolator
   */
  public void setValidationMessageSource(MessageSource messageSource) {
    this.messageInterpolator = HibernateValidatorDelegate.buildMessageInterpolator(messageSource);
  }

  /**
   * Specify a custom TraversableResolver to use for this ValidatorFactory
   * and its exposed default Validator.
   */
  public void setTraversableResolver(@Nullable TraversableResolver traversableResolver) {
    this.traversableResolver = traversableResolver;
  }

  /**
   * Specify a custom ConstraintValidatorFactory to use for this ValidatorFactory.
   * <p>Default is a {@link ContextConstraintValidatorFactory}, delegating to the
   * containing ApplicationContext for creating autowired ConstraintValidator instances.
   */
  public void setConstraintValidatorFactory(@Nullable ConstraintValidatorFactory constraintValidatorFactory) {
    this.constraintValidatorFactory = constraintValidatorFactory;
  }

  /**
   * Set the ParameterNameDiscoverer to use for resolving method and constructor
   * parameter names if needed for message interpolation.
   * <p>Default is a {@link cn.taketoday.core.DefaultParameterNameDiscoverer}.
   */
  public void setParameterNameDiscoverer(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  /**
   * Specify resource locations to load XML constraint mapping files from, if any.
   */
  public void setMappingLocations(Resource... mappingLocations) {
    this.mappingLocations = mappingLocations;
  }

  /**
   * Specify bean validation properties to be passed to the validation provider.
   * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
   * or a "props" element in XML bean definitions.
   *
   * @see Configuration#addProperty(String, String)
   */
  public void setValidationProperties(Properties jpaProperties) {
    CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.validationPropertyMap);
  }

  /**
   * Specify bean validation properties to be passed to the validation provider as a Map.
   * <p>Can be populated with a "map" or "props" element in XML bean definitions.
   *
   * @see Configuration#addProperty(String, String)
   */
  public void setValidationPropertyMap(@Nullable Map<String, String> validationProperties) {
    if (validationProperties != null) {
      this.validationPropertyMap.putAll(validationProperties);
    }
  }

  /**
   * Allow Map access to the bean validation properties to be passed to the validation provider,
   * with the option to add or override specific entries.
   * <p>Useful for specifying entries directly, for example via "validationPropertyMap[myKey]".
   */
  public Map<String, String> getValidationPropertyMap() {
    return this.validationPropertyMap;
  }

  @Override
  public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void afterPropertiesSet() {
    Configuration<?> configuration;
    if (this.providerClass != null) {
      ProviderSpecificBootstrap bootstrap = Validation.byProvider(this.providerClass);
      if (this.validationProviderResolver != null) {
        bootstrap = bootstrap.providerResolver(this.validationProviderResolver);
      }
      configuration = bootstrap.configure();
    }
    else {
      GenericBootstrap bootstrap = Validation.byDefaultProvider();
      if (this.validationProviderResolver != null) {
        bootstrap = bootstrap.providerResolver(this.validationProviderResolver);
      }
      configuration = bootstrap.configure();
    }

    // Try Hibernate Validator 5.2's externalClassLoader(ClassLoader) method
    if (this.applicationContext != null) {
      try {
        Method eclMethod = configuration.getClass().getMethod("externalClassLoader", ClassLoader.class);
        ReflectionUtils.invokeMethod(eclMethod, configuration, this.applicationContext.getClassLoader());
      }
      catch (NoSuchMethodException ex) {
        // Ignore - no Hibernate Validator 5.2+ or similar provider
      }
    }

    MessageInterpolator targetInterpolator = this.messageInterpolator;
    if (targetInterpolator == null) {
      targetInterpolator = configuration.getDefaultMessageInterpolator();
    }
    configuration.messageInterpolator(new LocaleContextMessageInterpolator(targetInterpolator));

    if (this.traversableResolver != null) {
      configuration.traversableResolver(this.traversableResolver);
    }

    ConstraintValidatorFactory targetConstraintValidatorFactory = this.constraintValidatorFactory;
    if (targetConstraintValidatorFactory == null && this.applicationContext != null) {
      targetConstraintValidatorFactory =
              new ContextConstraintValidatorFactory(this.applicationContext.getAutowireCapableBeanFactory());
    }
    if (targetConstraintValidatorFactory != null) {
      configuration.constraintValidatorFactory(targetConstraintValidatorFactory);
    }

    if (this.parameterNameDiscoverer != null) {
      configureParameterNameProvider(this.parameterNameDiscoverer, configuration);
    }

    List<InputStream> mappingStreams = null;
    if (this.mappingLocations != null) {
      mappingStreams = new ArrayList<>(this.mappingLocations.length);
      for (Resource location : this.mappingLocations) {
        try {
          InputStream stream = location.getInputStream();
          mappingStreams.add(stream);
          configuration.addMapping(stream);
        }
        catch (IOException ex) {
          closeMappingStreams(mappingStreams);
          throw new IllegalStateException("Cannot read mapping resource: " + location);
        }
      }
    }

    this.validationPropertyMap.forEach(configuration::addProperty);

    // Allow for custom post-processing before we actually build the ValidatorFactory.
    postProcessConfiguration(configuration);

    try {
      this.validatorFactory = configuration.buildValidatorFactory();
      setTargetValidator(this.validatorFactory.getValidator());
    }
    finally {
      closeMappingStreams(mappingStreams);
    }
  }

  private void configureParameterNameProvider(ParameterNameDiscoverer discoverer, Configuration<?> configuration) {
    final ParameterNameProvider defaultProvider = configuration.getDefaultParameterNameProvider();
    configuration.parameterNameProvider(new ParameterNameProvider() {
      @Override
      public List<String> getParameterNames(Constructor<?> constructor) {
        String[] paramNames = discoverer.getParameterNames(constructor);
        return (paramNames != null ? Arrays.asList(paramNames) :
                defaultProvider.getParameterNames(constructor));
      }

      @Override
      public List<String> getParameterNames(Method method) {
        String[] paramNames = discoverer.getParameterNames(method);
        return (paramNames != null ? Arrays.asList(paramNames) :
                defaultProvider.getParameterNames(method));
      }
    });
  }

  private void closeMappingStreams(@Nullable List<InputStream> mappingStreams) {
    if (CollectionUtils.isNotEmpty(mappingStreams)) {
      for (InputStream stream : mappingStreams) {
        try {
          stream.close();
        }
        catch (IOException ignored) { }
      }
    }
  }

  /**
   * Post-process the given Bean Validation configuration,
   * adding to or overriding any of its settings.
   * <p>Invoked right before building the {@link ValidatorFactory}.
   *
   * @param configuration the Configuration object, pre-populated with
   * settings driven by LocalValidatorFactoryBean's properties
   */
  protected void postProcessConfiguration(Configuration<?> configuration) { }

  @Override
  public Validator getValidator() {
    return validatorFactory().getValidator();
  }

  @Override
  public ValidatorContext usingContext() {
    return validatorFactory().usingContext();
  }

  @Override
  public MessageInterpolator getMessageInterpolator() {
    return validatorFactory().getMessageInterpolator();
  }

  @Override
  public TraversableResolver getTraversableResolver() {
    return validatorFactory().getTraversableResolver();
  }

  @Override
  public ConstraintValidatorFactory getConstraintValidatorFactory() {
    return validatorFactory().getConstraintValidatorFactory();
  }

  @Override
  public ParameterNameProvider getParameterNameProvider() {
    return validatorFactory().getParameterNameProvider();
  }

  private ValidatorFactory validatorFactory() {
    Assert.state(validatorFactory != null, "No target ValidatorFactory set");
    return validatorFactory;
  }

  @Override
  public ClockProvider getClockProvider() {
    return validatorFactory().getClockProvider();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrap(@Nullable Class<T> type) {
    if (type == null || !ValidatorFactory.class.isAssignableFrom(type)) {
      try {
        return super.unwrap(type);
      }
      catch (ValidationException ex) {
        // Ignore - we'll try ValidatorFactory unwrapping next
      }
    }
    if (this.validatorFactory != null) {
      try {
        return this.validatorFactory.unwrap(type);
      }
      catch (ValidationException ex) {
        // Ignore if just being asked for ValidatorFactory
        if (ValidatorFactory.class == type) {
          return (T) this.validatorFactory;
        }
        throw ex;
      }
    }
    throw new ValidationException("Cannot unwrap to " + type);
  }

  @Override
  public void close() {
    if (this.validatorFactory != null) {
      this.validatorFactory.close();
    }
  }

  @Override
  public void destroy() {
    close();
  }

  /**
   * Inner class to avoid a hard-coded Hibernate Validator dependency.
   */
  private static class HibernateValidatorDelegate {

    public static MessageInterpolator buildMessageInterpolator(MessageSource messageSource) {
      return new ResourceBundleMessageInterpolator(new MessageSourceResourceBundleLocator(messageSource));
    }
  }

}
