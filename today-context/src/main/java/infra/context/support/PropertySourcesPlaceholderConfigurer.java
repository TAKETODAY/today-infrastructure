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

package infra.context.support;

import java.io.IOException;
import java.util.Properties;

import infra.beans.BeansException;
import infra.beans.factory.BeanInitializationException;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.PlaceholderConfigurerSupport;
import infra.context.EnvironmentAware;
import infra.core.StringValueResolver;
import infra.core.conversion.ConversionService;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.ConfigurablePropertyResolver;
import infra.core.env.Environment;
import infra.core.env.PropertiesPropertySource;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.core.env.PropertySourcesPropertyResolver;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Specialization of {@link PlaceholderConfigurerSupport} that resolves ${...} placeholders
 * within bean definition property values and {@code @Value} annotations against the current
 * Framework {@link Environment} and its set of {@link PropertySources}.
 *
 * <p>Any local properties (e.g. those added via {@link #setProperties}, {@link #setLocations}
 * et al.) are added as a {@code PropertySource}. Search precedence of local properties is
 * based on the value of the {@link #setLocalOverride localOverride} property, which is by
 * default {@code false} meaning that local properties are to be searched last, after all
 * environment property sources.
 *
 * <p>See {@link ConfigurableEnvironment} and related javadocs
 * for details on manipulating environment property sources.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurableEnvironment
 * @see PlaceholderConfigurerSupport
 * @since 4.0 2021/12/12 14:39
 */
public class PropertySourcesPlaceholderConfigurer extends PlaceholderConfigurerSupport implements EnvironmentAware {

  /**
   * {@value} is the name given to the {@link PropertySource} for the set of
   * {@linkplain #mergeProperties() merged properties} supplied to this configurer.
   */
  public static final String LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME = "localProperties";

  /**
   * {@value} is the name given to the {@link PropertySource} that wraps the
   * {@linkplain #setEnvironment environment} supplied to this configurer.
   */
  public static final String ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME = "environmentProperties";

  @Nullable
  private PropertySources propertySources;

  @Nullable
  private PropertySources appliedPropertySources;

  @Nullable
  private Environment environment;

  /**
   * Customize the set of {@link PropertySources} to be used by this configurer.
   * <p>Setting this property indicates that environment property sources and
   * local properties should be ignored.
   *
   * @see #postProcessBeanFactory
   */
  public void setPropertySources(PropertySources propertySources) {
    this.propertySources = new PropertySources(propertySources);
  }

  /**
   * {@code PropertySources} from the given {@link Environment}
   * will be searched when replacing ${...} placeholders.
   *
   * @see #setPropertySources
   * @see #postProcessBeanFactory
   */
  @Override
  public void setEnvironment(@Nullable Environment environment) {
    this.environment = environment;
  }

  /**
   * Processing occurs by replacing ${...} placeholders in bean definitions by resolving each
   * against this configurer's set of {@link PropertySources}, which includes:
   * <ul>
   * <li>all {@linkplain ConfigurableEnvironment#getPropertySources
   * environment property sources}, if an {@code Environment} {@linkplain #setEnvironment is present}
   * <li>{@linkplain #mergeProperties merged local properties}, if {@linkplain #setLocation any}
   * {@linkplain #setLocations have} {@linkplain #setProperties been}
   * {@linkplain #setPropertiesArray specified}
   * <li>any property sources set by calling {@link #setPropertySources}
   * </ul>
   * <p>If {@link #setPropertySources} is called, <strong>environment and local properties will be
   * ignored</strong>. This method is designed to give the user fine-grained control over property
   * sources, and once set, the configurer makes no assumptions about adding additional sources.
   */
  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    if (this.propertySources == null) {
      this.propertySources = new PropertySources();
      if (this.environment != null) {
        PropertySource<?> environmentPropertySource = this.environment instanceof ConfigurableEnvironment ce
                ? new ConfigurableEnvironmentPropertySource(ce)
                : new FallbackEnvironmentPropertySource(this.environment);
        this.propertySources.addLast(environmentPropertySource);
      }
      try {
        PropertySource<?> localPropertySource =
                new PropertiesPropertySource(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME, mergeProperties());
        if (this.localOverride) {
          this.propertySources.addFirst(localPropertySource);
        }
        else {
          this.propertySources.addLast(localPropertySource);
        }
      }
      catch (IOException ex) {
        throw new BeanInitializationException("Could not load properties", ex);
      }
    }

    processProperties(beanFactory, createPropertyResolver(this.propertySources));
    this.appliedPropertySources = this.propertySources;
  }

  /**
   * Create a {@link ConfigurablePropertyResolver} for the specified property sources.
   *
   * @param propertySources the property sources to use
   */
  protected ConfigurablePropertyResolver createPropertyResolver(PropertySources propertySources) {
    return new PropertySourcesPropertyResolver(propertySources);
  }

  /**
   * Visit each bean definition in the given bean factory and attempt to replace ${...} property
   * placeholders with values from the given properties.
   */
  protected void processProperties(ConfigurableBeanFactory beanFactoryToProcess,
          ConfigurablePropertyResolver propertyResolver) throws BeansException {

    propertyResolver.setValueSeparator(valueSeparator);
    propertyResolver.setPlaceholderPrefix(placeholderPrefix);
    propertyResolver.setPlaceholderSuffix(placeholderSuffix);
    propertyResolver.setEscapeCharacter(escapeCharacter);

    StringValueResolver valueResolver = strVal -> {
      String resolved = ignoreUnresolvablePlaceholders ?
              propertyResolver.resolvePlaceholders(strVal) :
              propertyResolver.resolveRequiredPlaceholders(strVal);
      if (this.trimValues) {
        resolved = resolved.trim();
      }
      return resolved.equals(nullValue) ? null : resolved;
    };

    doProcessProperties(beanFactoryToProcess, valueResolver);
  }

  /**
   * Implemented for compatibility with
   * {@link PlaceholderConfigurerSupport}.
   *
   * @throws UnsupportedOperationException in this implementation
   * use {@link #processProperties(ConfigurableBeanFactory, ConfigurablePropertyResolver)}
   */
  @Override
  protected void processProperties(ConfigurableBeanFactory beanFactory, Properties props) {
    throw new UnsupportedOperationException(
            "Call processProperties(ConfigurableBeanFactory, ConfigurablePropertyResolver) instead");
  }

  /**
   * Return the property sources that were actually applied during
   * {@link #postProcessBeanFactory(ConfigurableBeanFactory) post-processing}.
   *
   * @return the property sources that were applied
   * @throws IllegalStateException if the property sources have not yet been applied
   */
  public PropertySources getAppliedPropertySources() throws IllegalStateException {
    Assert.state(this.appliedPropertySources != null, "PropertySources have not yet been applied");
    return this.appliedPropertySources;
  }

  /**
   * Custom {@link PropertySource} that delegates to the
   * {@link ConfigurableEnvironment#getPropertySources() PropertySources} in a
   * {@link ConfigurableEnvironment}.
   *
   * @since 5.0
   */
  private static class ConfigurableEnvironmentPropertySource extends PropertySource<ConfigurableEnvironment> {

    ConfigurableEnvironmentPropertySource(ConfigurableEnvironment environment) {
      super(ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, environment);
    }

    @Override
    public boolean containsProperty(String name) {
      for (PropertySource<?> propertySource : super.source.getPropertySources()) {
        if (propertySource.containsProperty(name)) {
          return true;
        }
      }
      return false;
    }

    @Override
    @Nullable
    // Declare String as covariant return type, since a String is actually required.
    public String getProperty(String name) {
      for (PropertySource<?> propertySource : super.source.getPropertySources()) {
        Object candidate = propertySource.getProperty(name);
        if (candidate != null) {
          return convertToString(candidate);
        }
      }
      return null;
    }

    /**
     * Convert the supplied value to a {@link String} using the {@link ConversionService}
     * from the {@link Environment}.
     * <p>This is a modified version of
     * {@link infra.core.env.AbstractPropertyResolver#convertValueIfNecessary(Object, Class)}.
     *
     * @param value the value to convert
     * @return the converted value, or the original value if no conversion is necessary
     */
    @Nullable
    private String convertToString(Object value) {
      if (value instanceof String string) {
        return string;
      }
      return super.source.getConversionService().convert(value, String.class);
    }

    @Override
    public String toString() {
      return "ConfigurableEnvironmentPropertySource {propertySources=" + super.source.getPropertySources() + "}";
    }
  }

  /**
   * Fallback {@link PropertySource} that delegates to a raw {@link Environment}.
   * <p>Should never apply in a regular scenario, since the {@code Environment}
   * in an {@code ApplicationContext} should always be a {@link ConfigurableEnvironment}.
   *
   * @since 5.0
   */
  private static class FallbackEnvironmentPropertySource extends PropertySource<Environment> {

    FallbackEnvironmentPropertySource(Environment environment) {
      super(ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, environment);
    }

    @Override
    public boolean containsProperty(String name) {
      return super.source.containsProperty(name);
    }

    @Nullable
    @Override
    // Declare String as covariant return type, since a String is actually required.
    public String getProperty(String name) {
      return super.source.getProperty(name);
    }

    @Override
    public String toString() {
      return "FallbackEnvironmentPropertySource {environment=" + super.source + "}";
    }
  }

}
