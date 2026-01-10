/*
 * Copyright 2002-present the original author or authors.
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

package infra.beans.factory.config;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.BeanNameAware;
import infra.beans.factory.annotation.Value;
import infra.core.StringValueResolver;
import infra.core.env.AbstractPropertyResolver;
import infra.core.env.PropertySource;
import infra.util.PropertyPlaceholderHandler;

/**
 * Abstract base class for property resource configurers that resolve placeholders
 * in bean definition property values. Implementations <em>pull</em> values from a
 * properties file or other {@linkplain PropertySource
 * property source} into bean definitions.
 *
 * <p>The default placeholder syntax follows the Ant / Log4J / JSP EL style:
 *
 * <pre> {@code
 *   ${...}
 * } </pre>
 *
 * Example XML bean definition:
 *
 * <pre>{@code
 * <bean id="dataSource" class="infra.jdbc.datasource.DriverManagerDataSource">
 *   <property name="driverClassName" value="${driver}" />
 *   <property name="url" value="jdbc:${dbname}" />
 * </bean>
 * }</pre>
 *
 * Example properties file:
 *
 * <pre>{@code
 * driver=com.mysql.jdbc.Driver
 * dbname=mysql:mydb
 * }</pre>
 *
 * Annotated bean definitions may take advantage of property replacement using
 * the {@link Value @Value} annotation:
 *
 * <pre>{@code @Value("${person.age}")}</pre>
 *
 * Implementations check simple property values, lists, maps, props, and bean names
 * in bean references. Furthermore, placeholder values can also cross-reference
 * other placeholders, like:
 *
 * <pre>{@code
 * rootPath=myrootdir
 * subPath=${rootPath}/subdir
 * }</pre>
 *
 * Subclasses of this type allow
 * filling in of explicit placeholders in bean definitions.
 *
 * <p>If a configurer cannot resolve a placeholder, a {@link BeanDefinitionStoreException}
 * will be thrown. If you want to check against multiple properties files, specify multiple
 * resources via the {@link #setLocations locations} property. You can also define multiple
 * configurers, each with its <em>own</em> placeholder syntax. Use {@link
 * #ignoreUnresolvablePlaceholders} to intentionally suppress throwing an exception if a
 * placeholder cannot be resolved.
 *
 * <p>Default property values can be defined globally for each configurer instance
 * via the {@link #setProperties properties} property, or on a property-by-property basis
 * using the value separator which is {@code ":"} by default and customizable via
 * {@link #setValueSeparator(String)}.
 *
 * <p>Example XML property with default value:
 *
 * <pre >{@code
 *   <property name="url" value="jdbc:${dbname:defaultdb}" />
 * }</pre>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.context.support.PropertySourcesPlaceholderConfigurer
 * @since 4.0 2021/12/12 14:38
 */
public abstract class PlaceholderConfigurerSupport extends PropertyResourceConfigurer implements BeanNameAware, BeanFactoryAware {

  /** Default placeholder prefix: {@value}. */
  public static final String DEFAULT_PLACEHOLDER_PREFIX = PropertyPlaceholderHandler.PLACEHOLDER_PREFIX;

  /** Default placeholder suffix: {@value}. */
  public static final String DEFAULT_PLACEHOLDER_SUFFIX = PropertyPlaceholderHandler.PLACEHOLDER_SUFFIX;

  /** Default value separator: {@value}. */
  public static final String DEFAULT_VALUE_SEPARATOR = PropertyPlaceholderHandler.VALUE_SEPARATOR;

  /** Defaults to {@value #DEFAULT_PLACEHOLDER_PREFIX}. */
  protected String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

  /** Defaults to {@value #DEFAULT_PLACEHOLDER_SUFFIX}. */
  protected String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

  /** Defaults to {@value #DEFAULT_VALUE_SEPARATOR}. */
  @Nullable
  protected String valueSeparator = DEFAULT_VALUE_SEPARATOR;

  /**
   * The default is determined by {@link AbstractPropertyResolver#getDefaultEscapeCharacter()}.
   */
  @Nullable
  protected Character escapeCharacter = AbstractPropertyResolver.getDefaultEscapeCharacter();

  protected boolean trimValues = false;

  @Nullable
  protected String nullValue;

  protected boolean ignoreUnresolvablePlaceholders = false;

  @Nullable
  private String beanName;

  @Nullable
  private BeanFactory beanFactory;

  /**
   * Set the prefix that a placeholder string starts with.
   * The default is {@value #DEFAULT_PLACEHOLDER_PREFIX}.
   */
  public void setPlaceholderPrefix(String placeholderPrefix) {
    this.placeholderPrefix = placeholderPrefix;
  }

  /**
   * Set the suffix that a placeholder string ends with.
   * The default is {@value #DEFAULT_PLACEHOLDER_SUFFIX}.
   */
  public void setPlaceholderSuffix(String placeholderSuffix) {
    this.placeholderSuffix = placeholderSuffix;
  }

  /**
   * Specify the separating character between the placeholder variable
   * and the associated default value, or {@code null} if no such
   * special character should be processed as a value separator.
   * The default is {@value #DEFAULT_VALUE_SEPARATOR}.
   */
  public void setValueSeparator(@Nullable String valueSeparator) {
    this.valueSeparator = valueSeparator;
  }

  /**
   * Set the escape character to use to ignore the
   * {@linkplain #setPlaceholderPrefix(String) placeholder prefix} and the
   * {@linkplain #setValueSeparator(String) value separator}, or {@code null}
   * if no escaping should take place.
   * <p>The default is determined by {@link AbstractPropertyResolver#getDefaultEscapeCharacter()}.
   */
  public void setEscapeCharacter(@Nullable Character escsEscapeCharacter) {
    this.escapeCharacter = escsEscapeCharacter;
  }

  /**
   * Specify whether to trim resolved values before applying them,
   * removing superfluous whitespace from the beginning and end.
   * <p>Default is {@code false}.
   */
  public void setTrimValues(boolean trimValues) {
    this.trimValues = trimValues;
  }

  /**
   * Set a value that should be treated as {@code null} when resolved
   * as a placeholder value: e.g. "" (empty String) or "null".
   * <p>Note that this will only apply to full property values,
   * not to parts of concatenated values.
   * <p>By default, no such null value is defined. This means that
   * there is no way to express {@code null} as a property value
   * unless you explicitly map a corresponding value here.
   */
  public void setNullValue(@Nullable String nullValue) {
    this.nullValue = nullValue;
  }

  /**
   * Set whether to ignore unresolvable placeholders.
   * <p>Default is "false": An exception will be thrown if a placeholder fails
   * to resolve. Switch this flag to "true" in order to preserve the placeholder
   * String as-is in such a case, leaving it up to other placeholder configurers
   * to resolve it.
   */
  public void setIgnoreUnresolvablePlaceholders(boolean ignoreUnresolvablePlaceholders) {
    this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
  }

  /**
   * Only necessary to check that we're not parsing our own bean definition,
   * to avoid failing on unresolvable placeholders in properties file locations.
   * The latter case can happen with placeholders for system properties in
   * resource locations.
   *
   * @see #setLocations
   */
  @Override
  public void setBeanName(@Nullable String beanName) {
    this.beanName = beanName;
  }

  /**
   * Only necessary to check that we're not parsing our own bean definition,
   * to avoid failing on unresolvable placeholders in properties file locations.
   * The latter case can happen with placeholders for system properties in
   * resource locations.
   *
   * @see #setLocations
   */
  @Override
  public void setBeanFactory(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  protected void doProcessProperties(ConfigurableBeanFactory beanFactoryToProcess, StringValueResolver valueResolver) {
    BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);

    String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
    for (String curName : beanNames) {
      // Check that we're not parsing our own bean definition,
      // to avoid failing on unresolvable placeholders in properties file locations.
      if (!(curName.equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
        BeanDefinition definition = beanFactoryToProcess.getBeanDefinition(curName);
        try {
          visitor.visitBeanDefinition(definition);
        }
        catch (Exception ex) {
          throw new BeanDefinitionStoreException(
                  definition.getResourceDescription(), curName, ex.getMessage(), ex);
        }
      }
    }

    // resolve placeholders in alias target names and aliases as well.
    beanFactoryToProcess.resolveAliases(valueResolver);

    // resolve placeholders in embedded values such as annotation attributes.
    beanFactoryToProcess.addEmbeddedValueResolver(valueResolver);
  }

}
