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

package cn.taketoday.beans.factory;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Property resource configurer that overrides bean property values in an application
 * context definition. It <i>pushes</i> values from a properties file into bean definitions.
 *
 * <p>Configuration lines are expected to be of the following form:
 *
 * <pre class="code">beanName.property=value</pre>
 *
 * Example properties file:
 *
 * <pre class="code">dataSource.driverClassName=com.mysql.jdbc.Driver
 * dataSource.url=jdbc:mysql:mydb</pre>
 *
 * In contrast to PropertyPlaceholderConfigurer, the original definition can have default
 * values or no values at all for such bean properties. If an overriding properties file does
 * not have an entry for a certain bean property, the default context definition is used.
 *
 * <p>Note that the context definition <i>is not</i> aware of being overridden;
 * so this is not immediately obvious when looking at the XML definition file.
 * Furthermore, note that specified override values are always <i>literal</i> values;
 * they are not translated into bean references. This also applies when the original
 * value in the XML bean definition specifies a bean reference.
 *
 * <p>In case of multiple PropertyOverrideConfigurers that define different values for
 * the same bean property, the <i>last</i> one will win (due to the overriding mechanism).
 *
 * <p>Property values can be converted after reading them in, through overriding
 * the {@code convertPropertyValue} method. For example, encrypted values
 * can be detected and decrypted accordingly before processing them.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #convertPropertyValue
 * @since 4.0 2021/12/12 14:59
 */
public class PropertyOverrideConfigurer extends PropertyResourceConfigurer {

  /**
   * The default bean name separator.
   */
  public static final String DEFAULT_BEAN_NAME_SEPARATOR = ".";

  private String beanNameSeparator = DEFAULT_BEAN_NAME_SEPARATOR;

  private boolean ignoreInvalidKeys = false;

  /**
   * Contains names of beans that have overrides.
   */
  private final Set<String> beanNames = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

  /**
   * Set the separator to expect between bean name and property path.
   * Default is a dot (".").
   */
  public void setBeanNameSeparator(String beanNameSeparator) {
    this.beanNameSeparator = beanNameSeparator;
  }

  /**
   * Set whether to ignore invalid keys. Default is "false".
   * <p>If you ignore invalid keys, keys that do not follow the 'beanName.property' format
   * (or refer to invalid bean names or properties) will just be logged at debug level.
   * This allows one to have arbitrary other keys in a properties file.
   */
  public void setIgnoreInvalidKeys(boolean ignoreInvalidKeys) {
    this.ignoreInvalidKeys = ignoreInvalidKeys;
  }

  @Override
  protected void processProperties(ConfigurableBeanFactory beanFactory, Properties props)
          throws BeansException {

    for (Enumeration<?> names = props.propertyNames(); names.hasMoreElements(); ) {
      String key = (String) names.nextElement();
      try {
        processKey(beanFactory, key, props.getProperty(key));
      }
      catch (BeansException ex) {
        String msg = "Could not process key '" + key + "' in PropertyOverrideConfigurer";
        if (!this.ignoreInvalidKeys) {
          throw new BeanInitializationException(msg, ex);
        }
        if (logger.isDebugEnabled()) {
          logger.debug(msg, ex);
        }
      }
    }
  }

  /**
   * Process the given key as 'beanName.property' entry.
   */
  protected void processKey(ConfigurableBeanFactory factory, String key, String value)
          throws BeansException {

    int separatorIndex = key.indexOf(this.beanNameSeparator);
    if (separatorIndex == -1) {
      throw new BeanInitializationException("Invalid key '" + key +
              "': expected 'beanName" + this.beanNameSeparator + "property'");
    }
    String beanName = key.substring(0, separatorIndex);
    String beanProperty = key.substring(separatorIndex + 1);
    this.beanNames.add(beanName);
    applyPropertyValue(factory, beanName, beanProperty, value);
    if (logger.isDebugEnabled()) {
      logger.debug("Property '" + key + "' set to value [" + value + "]");
    }
  }

  /**
   * Apply the given property value to the corresponding bean.
   */
  protected void applyPropertyValue(
          ConfigurableBeanFactory factory, String beanName, String property, String value) {

    BeanDefinition bd = BeanFactoryUtils.requiredDefinition(factory, beanName);
    PropertyValue pv = new PropertyValue(property, value);
    bd.propertyValues().add(pv);
  }

  /**
   * Were there overrides for this bean?
   * Only valid after processing has occurred at least once.
   *
   * @param beanName name of the bean to query status for
   * @return whether there were property overrides for the named bean
   */
  public boolean hasPropertyOverridesFor(String beanName) {
    return this.beanNames.contains(beanName);
  }

}

