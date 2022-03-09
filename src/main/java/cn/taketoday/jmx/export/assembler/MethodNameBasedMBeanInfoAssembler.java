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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.jmx.export.MBeanExporter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Subclass of {@code AbstractReflectiveMBeanInfoAssembler} that allows
 * to specify method names to be exposed as MBean operations and attributes.
 * JavaBean getters and setters will automatically be exposed as JMX attributes.
 *
 * <p>You can supply an array of method names via the {@code managedMethods}
 * property. If you have multiple beans and you wish each bean to use a different
 * set of method names, then you can map bean keys (that is the name used to pass
 * the bean to the {@code MBeanExporter}) to a list of method names using the
 * {@code methodMappings} property.
 *
 * <p>If you specify values for both {@code methodMappings} and
 * {@code managedMethods}, Framework will attempt to find method names in the
 * mappings first. If no method names for the bean are found, it will use the
 * method names defined by {@code managedMethods}.
 *
 * @author Juergen Hoeller
 * @see #setManagedMethods
 * @see #setMethodMappings
 * @see InterfaceBasedMBeanInfoAssembler
 * @see SimpleReflectiveMBeanInfoAssembler
 * @see MethodExclusionMBeanInfoAssembler
 * @see MBeanExporter
 * @since 4.0
 */
public class MethodNameBasedMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler {

  /**
   * Stores the set of method names to use for creating the management interface.
   */
  @Nullable
  private Set<String> managedMethods;

  /**
   * Stores the mappings of bean keys to an array of method names.
   */
  @Nullable
  private Map<String, Set<String>> methodMappings;

  /**
   * Set the array of method names to use for creating the management info.
   * These method names will be used for a bean if no entry corresponding to
   * that bean is found in the {@code methodMappings} property.
   *
   * @param methodNames an array of method names indicating the methods to use
   * @see #setMethodMappings
   */
  public void setManagedMethods(String... methodNames) {
    this.managedMethods = new HashSet<>(Arrays.asList(methodNames));
  }

  /**
   * Set the mappings of bean keys to a comma-separated list of method names.
   * The property key should match the bean key and the property value should match
   * the list of method names. When searching for method names for a bean, Spring
   * will check these mappings first.
   *
   * @param mappings the mappings of bean keys to method names
   */
  public void setMethodMappings(Properties mappings) {
    this.methodMappings = new HashMap<>();
    for (String beanKey : mappings.stringPropertyNames()) {
      String[] methodNames = StringUtils.commaDelimitedListToStringArray(mappings.getProperty(beanKey));
      this.methodMappings.put(beanKey, CollectionUtils.newHashSet(methodNames));
    }
  }

  @Override
  protected boolean includeReadAttribute(Method method, String beanKey) {
    return isMatch(method, beanKey);
  }

  @Override
  protected boolean includeWriteAttribute(Method method, String beanKey) {
    return isMatch(method, beanKey);
  }

  @Override
  protected boolean includeOperation(Method method, String beanKey) {
    return isMatch(method, beanKey);
  }

  protected boolean isMatch(Method method, String beanKey) {
    if (this.methodMappings != null) {
      Set<String> methodNames = this.methodMappings.get(beanKey);
      if (methodNames != null) {
        return methodNames.contains(method.getName());
      }
    }
    return managedMethods != null && managedMethods.contains(method.getName());
  }

}
