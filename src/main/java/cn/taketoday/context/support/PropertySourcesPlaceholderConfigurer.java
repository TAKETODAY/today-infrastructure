/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.support;

import java.io.IOException;
import java.util.Properties;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanInitializationException;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.PlaceholderConfigurerSupport;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.ConfigurablePropertyResolver;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertiesPropertySource;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.PropertySourcesPropertyResolver;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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
 * <p>See {@link cn.taketoday.core.env.ConfigurableEnvironment} and related javadocs
 * for details on manipulating environment property sources.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.core.env.ConfigurableEnvironment
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
   * <li>all {@linkplain cn.taketoday.core.env.ConfigurableEnvironment#getPropertySources
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
        PropertyResolver propertyResolver = this.environment;
        // If the ignoreUnresolvablePlaceholders flag is set to true, we have to create a
        // local PropertyResolver to enforce that setting, since the Environment is most
        // likely not configured with ignoreUnresolvablePlaceholders set to true.
        // See https://github.com/spring-projects/spring-framework/issues/27947
        if (this.ignoreUnresolvablePlaceholders &&
                (this.environment instanceof ConfigurableEnvironment configurableEnvironment)) {
          PropertySourcesPropertyResolver resolver =
                  new PropertySourcesPropertyResolver(configurableEnvironment.getPropertySources());
          resolver.setIgnoreUnresolvableNestedPlaceholders(true);
          propertyResolver = resolver;
        }
        PropertyResolver propertyResolverToUse = propertyResolver;
        this.propertySources.addLast(
                new PropertySource<>(ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, this.environment) {
                  @Override
                  @Nullable
                  public String getProperty(String key) {
                    return propertyResolverToUse.getProperty(key);
                  }
                }
        );
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

    processProperties(beanFactory, new PropertySourcesPropertyResolver(this.propertySources));
    this.appliedPropertySources = this.propertySources;
  }

  /**
   * Visit each bean definition in the given bean factory and attempt to replace ${...} property
   * placeholders with values from the given properties.
   */
  protected void processProperties(
          ConfigurableBeanFactory beanFactoryToProcess,
          ConfigurablePropertyResolver propertyResolver) throws BeansException {

    propertyResolver.setValueSeparator(valueSeparator);
    propertyResolver.setPlaceholderPrefix(placeholderPrefix);
    propertyResolver.setPlaceholderSuffix(placeholderSuffix);

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

}
