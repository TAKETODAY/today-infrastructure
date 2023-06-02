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

package cn.taketoday.beans.factory.config;

import java.util.Properties;

import cn.taketoday.beans.BeansException;
import cn.taketoday.core.Constants;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.core.env.Environment;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.PlaceholderResolver;
import cn.taketoday.util.PropertyPlaceholderHandler;

/**
 * {@link PlaceholderConfigurerSupport} subclass that resolves ${...} placeholders against
 * {@link #setLocation local} {@link #setProperties properties} and/or system properties
 * and environment variables.
 *
 * <p>{@link PropertyPlaceholderConfigurer} is still appropriate for use when:
 * <ul>
 * <li>the {@code context} module is not available (i.e., one is using Framework's
 * {@code BeanFactory} API as opposed to {@code ApplicationContext}).
 * <li>existing configuration makes use of the {@link #setSystemPropertiesMode(int) "systemPropertiesMode"}
 * and/or {@link #setSystemPropertiesModeName(String) "systemPropertiesModeName"} properties.
 * Users are encouraged to move away from using these settings, and rather configure property
 * source search order through the container's {@code Environment}; however, exact preservation
 * of functionality may be maintained by continuing to use {@code PropertyPlaceholderConfigurer}.
 * </ul>
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setSystemPropertiesModeName
 * @see PlaceholderConfigurerSupport
 * @see PropertyOverrideConfigurer
 * @since 4.0 2022/1/5 23:28
 */
public class PropertyPlaceholderConfigurer extends PlaceholderConfigurerSupport {

  /** Never check system properties. */
  public static final int SYSTEM_PROPERTIES_MODE_NEVER = 0;

  /**
   * Check system properties if not resolvable in the specified properties.
   * This is the default.
   */
  public static final int SYSTEM_PROPERTIES_MODE_FALLBACK = 1;

  /**
   * Check system properties first, before trying the specified properties.
   * This allows system properties to override any other property source.
   */
  public static final int SYSTEM_PROPERTIES_MODE_OVERRIDE = 2;

  private static final Constants constants = new Constants(PropertyPlaceholderConfigurer.class);

  private int systemPropertiesMode = SYSTEM_PROPERTIES_MODE_FALLBACK;

  private boolean searchSystemEnvironment = !TodayStrategies.getFlag(Environment.KEY_IGNORE_GETENV);

  /**
   * Set the system property mode by the name of the corresponding constant,
   * e.g. "SYSTEM_PROPERTIES_MODE_OVERRIDE".
   *
   * @param constantName name of the constant
   * @see #setSystemPropertiesMode
   */
  public void setSystemPropertiesModeName(String constantName) throws IllegalArgumentException {
    this.systemPropertiesMode = constants.asNumber(constantName).intValue();
  }

  /**
   * Set how to check system properties: as fallback, as override, or never.
   * For example, will resolve ${user.dir} to the "user.dir" system property.
   * <p>The default is "fallback": If not being able to resolve a placeholder
   * with the specified properties, a system property will be tried.
   * "override" will check for a system property first, before trying the
   * specified properties. "never" will not check system properties at all.
   *
   * @see #SYSTEM_PROPERTIES_MODE_NEVER
   * @see #SYSTEM_PROPERTIES_MODE_FALLBACK
   * @see #SYSTEM_PROPERTIES_MODE_OVERRIDE
   * @see #setSystemPropertiesModeName
   */
  public void setSystemPropertiesMode(int systemPropertiesMode) {
    this.systemPropertiesMode = systemPropertiesMode;
  }

  /**
   * Set whether to search for a matching system environment variable
   * if no matching system property has been found. Only applied when
   * "systemPropertyMode" is active (i.e. "fallback" or "override"), right
   * after checking JVM system properties.
   * <p>Default is "true". Switch this setting off to never resolve placeholders
   * against system environment variables. Note that it is generally recommended
   * to pass external values in as JVM system properties: This can easily be
   * achieved in a startup script, even for existing environment variables.
   *
   * @see #setSystemPropertiesMode
   * @see System#getProperty(String)
   * @see System#getenv(String)
   */
  public void setSearchSystemEnvironment(boolean searchSystemEnvironment) {
    this.searchSystemEnvironment = searchSystemEnvironment;
  }

  /**
   * Resolve the given placeholder using the given properties, performing
   * a system properties check according to the given mode.
   * <p>The default implementation delegates to {@code resolvePlaceholder
   * (placeholder, props)} before/after the system properties check.
   * <p>Subclasses can override this for custom resolution strategies,
   * including customized points for the system properties check.
   *
   * @param placeholder the placeholder to resolve
   * @param props the merged properties of this configurer
   * @param systemPropertiesMode the system properties mode,
   * according to the constants in this class
   * @return the resolved value, of null if none
   * @see #setSystemPropertiesMode
   * @see System#getProperty
   * @see #resolvePlaceholder(String, java.util.Properties)
   */
  @Nullable
  protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
    String propVal = null;
    if (systemPropertiesMode == SYSTEM_PROPERTIES_MODE_OVERRIDE) {
      propVal = resolveSystemProperty(placeholder);
    }
    if (propVal == null) {
      propVal = resolvePlaceholder(placeholder, props);
    }
    if (propVal == null && systemPropertiesMode == SYSTEM_PROPERTIES_MODE_FALLBACK) {
      propVal = resolveSystemProperty(placeholder);
    }
    return propVal;
  }

  /**
   * Resolve the given placeholder using the given properties.
   * The default implementation simply checks for a corresponding property key.
   * <p>Subclasses can override this for customized placeholder-to-key mappings
   * or custom resolution strategies, possibly just using the given properties
   * as fallback.
   * <p>Note that system properties will still be checked before respectively
   * after this method is invoked, according to the system properties mode.
   *
   * @param placeholder the placeholder to resolve
   * @param props the merged properties of this configurer
   * @return the resolved value, of {@code null} if none
   * @see #setSystemPropertiesMode
   */
  @Nullable
  protected String resolvePlaceholder(String placeholder, Properties props) {
    return props.getProperty(placeholder);
  }

  /**
   * Resolve the given key as JVM system property, and optionally also as
   * system environment variable if no matching system property has been found.
   *
   * @param key the placeholder to resolve as system property key
   * @return the system property value, or {@code null} if not found
   * @see #setSearchSystemEnvironment
   * @see System#getProperty(String)
   * @see System#getenv(String)
   */
  @Nullable
  protected String resolveSystemProperty(String key) {
    try {
      String value = System.getProperty(key);
      if (value == null && searchSystemEnvironment) {
        value = System.getenv(key);
      }
      return value;
    }
    catch (Throwable ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Could not access system property '{}': {}", key, ex.toString());
      }
      return null;
    }
  }

  /**
   * Visit each bean definition in the given bean factory and attempt to replace ${...} property
   * placeholders with values from the given properties.
   */
  @Override
  protected void processProperties(ConfigurableBeanFactory beanFactoryToProcess, Properties props)
          throws BeansException {

    StringValueResolver valueResolver = new PlaceholderResolvingStringValueResolver(props);
    doProcessProperties(beanFactoryToProcess, valueResolver);
  }

  private class PlaceholderResolvingStringValueResolver implements StringValueResolver {

    private final PropertyPlaceholderHandler helper;

    private final PlaceholderResolver resolver;

    public PlaceholderResolvingStringValueResolver(Properties props) {
      this.helper = new PropertyPlaceholderHandler(
              placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);
      this.resolver = new PropertyPlaceholderConfigurerResolver(props);
    }

    @Override
    @Nullable
    public String resolveStringValue(String strVal) throws BeansException {
      String resolved = this.helper.replacePlaceholders(strVal, this.resolver);
      if (trimValues) {
        resolved = resolved.trim();
      }
      return resolved.equals(nullValue) ? null : resolved;
    }
  }

  private final class PropertyPlaceholderConfigurerResolver implements PlaceholderResolver {

    private final Properties props;

    private PropertyPlaceholderConfigurerResolver(Properties props) {
      this.props = props;
    }

    @Override
    @Nullable
    public String resolvePlaceholder(String placeholderName) {
      return PropertyPlaceholderConfigurer.this.resolvePlaceholder(
              placeholderName, props, systemPropertiesMode);
    }
  }

}
