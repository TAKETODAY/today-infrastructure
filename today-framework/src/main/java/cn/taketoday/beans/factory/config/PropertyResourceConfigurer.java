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

package cn.taketoday.beans.factory.config;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanInitializationException;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.core.io.PropertiesLoaderSupport;
import cn.taketoday.util.ObjectUtils;

/**
 * Allows for configuration of individual bean property values from a property resource,
 * i.e. a properties file. Useful for custom config files targeted at system
 * administrators that override bean properties configured in the application context.
 *
 * <p>Two concrete implementations are provided in the distribution:
 * <ul>
 * <li>{@link PropertyOverrideConfigurer} for "beanName.property=value" style overriding
 * (<i>pushing</i> values from a properties file into bean definitions)
 * </ul>
 *
 * <p>Property values can be converted after reading them in, through overriding
 * the {@link #convertPropertyValue} method. For example, encrypted values
 * can be detected and decrypted accordingly before processing them.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyOverrideConfigurer
 * @since 4.0 2021/12/12 14:37
 */
public abstract class PropertyResourceConfigurer
        extends PropertiesLoaderSupport implements BeanFactoryPostProcessor, PriorityOrdered {

  private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

  /**
   * Set the order value of this object for sorting purposes.
   *
   * @see PriorityOrdered
   */
  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  /**
   * {@linkplain #mergeProperties Merge}, {@linkplain #convertProperties convert} and
   * {@linkplain #processProperties process} properties against the given bean factory.
   *
   * @throws BeanInitializationException if any properties cannot be loaded
   */
  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    try {
      Properties mergedProps = mergeProperties();

      // Convert the merged properties, if necessary.
      convertProperties(mergedProps);

      // Let the subclass process the properties.
      processProperties(beanFactory, mergedProps);
    }
    catch (IOException ex) {
      throw new BeanInitializationException("Could not load properties: " + ex.getMessage(), ex);
    }
  }

  /**
   * Convert the given merged properties, converting property values
   * if necessary. The result will then be processed.
   * <p>The default implementation will invoke {@link #convertPropertyValue}
   * for each property value, replacing the original with the converted value.
   *
   * @param props the Properties to convert
   * @see #processProperties
   */
  protected void convertProperties(Properties props) {
    Enumeration<?> propertyNames = props.propertyNames();
    while (propertyNames.hasMoreElements()) {
      String propertyName = (String) propertyNames.nextElement();
      String propertyValue = props.getProperty(propertyName);
      String convertedValue = convertProperty(propertyName, propertyValue);
      if (!ObjectUtils.nullSafeEquals(propertyValue, convertedValue)) {
        props.setProperty(propertyName, convertedValue);
      }
    }
  }

  /**
   * Convert the given property from the properties source to the value
   * which should be applied.
   * <p>The default implementation calls {@link #convertPropertyValue(String)}.
   *
   * @param propertyName the name of the property that the value is defined for
   * @param propertyValue the original value from the properties source
   * @return the converted value, to be used for processing
   * @see #convertPropertyValue(String)
   */
  protected String convertProperty(String propertyName, String propertyValue) {
    return convertPropertyValue(propertyValue);
  }

  /**
   * Convert the given property value from the properties source to the value
   * which should be applied.
   * <p>The default implementation simply returns the original value.
   * Can be overridden in subclasses, for example to detect
   * encrypted values and decrypt them accordingly.
   *
   * @param originalValue the original value from the properties source
   * (properties file or local "properties")
   * @return the converted value, to be used for processing
   * @see #setProperties
   * @see #setLocations
   * @see #setLocation
   * @see #convertProperty(String, String)
   */
  protected String convertPropertyValue(String originalValue) {
    return originalValue;
  }

  /**
   * Apply the given Properties to the given BeanFactory.
   *
   * @param beanFactory the BeanFactory used by the application context
   * @param props the Properties to apply
   * @throws BeansException in case of errors
   */
  protected abstract void processProperties(ConfigurableBeanFactory beanFactory, Properties props)
          throws BeansException;

}
