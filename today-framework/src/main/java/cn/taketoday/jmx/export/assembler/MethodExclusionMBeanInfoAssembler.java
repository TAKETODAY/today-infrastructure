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

package cn.taketoday.jmx.export.assembler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.jmx.export.MBeanExporter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@code AbstractReflectiveMBeanInfoAssembler} subclass that allows
 * method names to be explicitly excluded as MBean operations and attributes.
 *
 * <p>Any method not explicitly excluded from the management interface will be exposed to
 * JMX. JavaBean getters and setters will automatically be exposed as JMX attributes.
 *
 * <p>You can supply an array of method names via the {@code ignoredMethods}
 * property. If you have multiple beans and you wish each bean to use a different
 * set of method names, then you can map bean keys (that is the name used to pass
 * the bean to the {@code MBeanExporter}) to a list of method names using the
 * {@code ignoredMethodMappings} property.
 *
 * <p>If you specify values for both {@code ignoredMethodMappings} and
 * {@code ignoredMethods}, Framework will attempt to find method names in the
 * mappings first. If no method names for the bean are found, it will use the
 * method names defined by {@code ignoredMethods}.
 *
 * @author Rob Harrop
 * @author Seth Ladd
 * @see #setIgnoredMethods
 * @see #setIgnoredMethodMappings
 * @see InterfaceBasedMBeanInfoAssembler
 * @see SimpleReflectiveMBeanInfoAssembler
 * @see MethodNameBasedMBeanInfoAssembler
 * @see MBeanExporter
 * @since 4.0
 */
public class MethodExclusionMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler {

  @Nullable
  private Set<String> ignoredMethods;

  @Nullable
  private Map<String, Set<String>> ignoredMethodMappings;

  /**
   * Set the array of method names to be <b>ignored</b> when creating the management info.
   * <p>These method names will be used for a bean if no entry corresponding to
   * that bean is found in the {@code ignoredMethodsMappings} property.
   *
   * @see #setIgnoredMethodMappings(Properties)
   */
  public void setIgnoredMethods(String... ignoredMethodNames) {
    this.ignoredMethods = new HashSet<>(Arrays.asList(ignoredMethodNames));
  }

  /**
   * Set the mappings of bean keys to a comma-separated list of method names.
   * <p>These method names are <b>ignored</b> when creating the management interface.
   * <p>The property key must match the bean key and the property value must match
   * the list of method names. When searching for method names to ignore for a bean,
   * Framework will check these mappings first.
   */
  public void setIgnoredMethodMappings(Properties mappings) {
    this.ignoredMethodMappings = new HashMap<>();
    for (Enumeration<?> en = mappings.keys(); en.hasMoreElements(); ) {
      String beanKey = (String) en.nextElement();
      String[] methodNames = StringUtils.commaDelimitedListToStringArray(mappings.getProperty(beanKey));
      this.ignoredMethodMappings.put(beanKey, new HashSet<>(Arrays.asList(methodNames)));
    }
  }

  @Override
  protected boolean includeReadAttribute(Method method, String beanKey) {
    return isNotIgnored(method, beanKey);
  }

  @Override
  protected boolean includeWriteAttribute(Method method, String beanKey) {
    return isNotIgnored(method, beanKey);
  }

  @Override
  protected boolean includeOperation(Method method, String beanKey) {
    return isNotIgnored(method, beanKey);
  }

  /**
   * Determine whether the given method is supposed to be included,
   * that is, not configured as to be ignored.
   *
   * @param method the operation method
   * @param beanKey the key associated with the MBean in the beans map
   * of the {@code MBeanExporter}
   */
  protected boolean isNotIgnored(Method method, String beanKey) {
    if (this.ignoredMethodMappings != null) {
      Set<String> methodNames = this.ignoredMethodMappings.get(beanKey);
      if (methodNames != null) {
        return !methodNames.contains(method.getName());
      }
    }
    if (this.ignoredMethods != null) {
      return !this.ignoredMethods.contains(method.getName());
    }
    return true;
  }

}
