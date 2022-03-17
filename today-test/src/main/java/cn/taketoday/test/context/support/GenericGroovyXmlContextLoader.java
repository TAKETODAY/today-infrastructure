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

package cn.taketoday.test.context.support;

import cn.taketoday.beans.factory.groovy.GroovyBeanDefinitionReader;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.test.context.MergedContextConfiguration;

/**
 * Concrete implementation of {@link AbstractGenericContextLoader} that reads
 * bean definitions from Groovy scripts <em>and</em> XML configuration files.
 *
 * <p>Default resource locations are detected using the suffixes
 * {@code "-context.xml"} and {@code "Context.groovy"}.
 *
 * @author Sam Brannen
 * @see GroovyBeanDefinitionReader
 * @see GenericXmlContextLoader
 * @see AnnotationConfigContextLoader
 * @since 4.1
 */
public class GenericGroovyXmlContextLoader extends GenericXmlContextLoader {

  /**
   * Load bean definitions into the supplied {@link GenericApplicationContext context}
   * from the locations in the supplied {@code MergedContextConfiguration} using a
   * {@link GroovyBeanDefinitionReader}.
   *
   * @param context the context into which the bean definitions should be loaded
   * @param mergedConfig the merged context configuration
   * @see AbstractGenericContextLoader#loadBeanDefinitions
   */
  @Override
  protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
    new GroovyBeanDefinitionReader(context).loadBeanDefinitions(mergedConfig.getLocations());
  }

  /**
   * Returns {@code "-context.xml"} and {@code "Context.groovy"} in order to
   * support detection of a default XML config file or Groovy script.
   */
  @Override
  protected String[] getResourceSuffixes() {
    return new String[] { super.getResourceSuffix(), "Context.groovy" };
  }

  /**
   * {@code GenericGroovyXmlContextLoader} supports both Groovy and XML
   * resource types for detection of defaults. Consequently, this method
   * is not supported.
   *
   * @throws UnsupportedOperationException in this implementation
   * @see #getResourceSuffixes()
   */
  @Override
  protected String getResourceSuffix() {
    throw new UnsupportedOperationException(
            "GenericGroovyXmlContextLoader does not support the getResourceSuffix() method");
  }

}
