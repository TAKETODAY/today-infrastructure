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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.jmx.export.MBeanExporter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Subclass of {@code AbstractReflectiveMBeanInfoAssembler} that allows for
 * the management interface of a bean to be defined using arbitrary interfaces.
 * Any methods or properties that are defined in those interfaces are exposed
 * as MBean operations and attributes.
 *
 * <p>By default, this class votes on the inclusion of each operation or attribute
 * based on the interfaces implemented by the bean class. However, you can supply an
 * array of interfaces via the {@code managedInterfaces} property that will be
 * used instead. If you have multiple beans and you wish each bean to use a different
 * set of interfaces, then you can map bean keys (that is the name used to pass the
 * bean to the {@code MBeanExporter}) to a list of interface names using the
 * {@code interfaceMappings} property.
 *
 * <p>If you specify values for both {@code interfaceMappings} and
 * {@code managedInterfaces}, Framework will attempt to find interfaces in the
 * mappings first. If no interfaces for the bean are found, it will use the
 * interfaces defined by {@code managedInterfaces}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see #setManagedInterfaces
 * @see #setInterfaceMappings
 * @see MethodNameBasedMBeanInfoAssembler
 * @see SimpleReflectiveMBeanInfoAssembler
 * @see MBeanExporter
 * @since 4.0
 */
public class InterfaceBasedMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler
        implements BeanClassLoaderAware, InitializingBean {

  @Nullable
  private Class<?>[] managedInterfaces;

  /** Mappings of bean keys to an array of classes. */
  @Nullable
  private Properties interfaceMappings;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  /** Mappings of bean keys to an array of classes. */
  @Nullable
  private Map<String, Class<?>[]> resolvedInterfaceMappings;

  /**
   * Set the array of interfaces to use for creating the management info.
   * These interfaces will be used for a bean if no entry corresponding to
   * that bean is found in the {@code interfaceMappings} property.
   *
   * @param managedInterfaces an array of classes indicating the interfaces to use.
   * Each entry <strong>MUST</strong> be an interface.
   * @see #setInterfaceMappings
   */
  public void setManagedInterfaces(@Nullable Class<?>... managedInterfaces) {
    if (managedInterfaces != null) {
      for (Class<?> ifc : managedInterfaces) {
        if (!ifc.isInterface()) {
          throw new IllegalArgumentException(
                  "Management interface [" + ifc.getName() + "] is not an interface");
        }
      }
    }
    this.managedInterfaces = managedInterfaces;
  }

  /**
   * Set the mappings of bean keys to a comma-separated list of interface names.
   * <p>The property key should match the bean key and the property value should match
   * the list of interface names. When searching for interfaces for a bean, Spring
   * will check these mappings first.
   *
   * @param mappings the mappings of bean keys to interface names
   */
  public void setInterfaceMappings(@Nullable Properties mappings) {
    this.interfaceMappings = mappings;
  }

  @Override
  public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
    this.beanClassLoader = beanClassLoader;
  }

  @Override
  public void afterPropertiesSet() {
    if (this.interfaceMappings != null) {
      this.resolvedInterfaceMappings = resolveInterfaceMappings(this.interfaceMappings);
    }
  }

  /**
   * Resolve the given interface mappings, turning class names into Class objects.
   *
   * @param mappings the specified interface mappings
   * @return the resolved interface mappings (with Class objects as values)
   */
  private Map<String, Class<?>[]> resolveInterfaceMappings(Properties mappings) {
    Map<String, Class<?>[]> resolvedMappings = CollectionUtils.newHashMap(mappings.size());
    for (Enumeration<?> en = mappings.propertyNames(); en.hasMoreElements(); ) {
      String beanKey = (String) en.nextElement();
      String[] classNames = StringUtils.commaDelimitedListToStringArray(mappings.getProperty(beanKey));
      Class<?>[] classes = resolveClassNames(classNames, beanKey);
      resolvedMappings.put(beanKey, classes);
    }
    return resolvedMappings;
  }

  /**
   * Resolve the given class names into Class objects.
   *
   * @param classNames the class names to resolve
   * @param beanKey the bean key that the class names are associated with
   * @return the resolved Class
   */
  private Class<?>[] resolveClassNames(String[] classNames, String beanKey) {
    Class<?>[] classes = new Class<?>[classNames.length];
    for (int x = 0; x < classes.length; x++) {
      Class<?> cls = ClassUtils.resolveClassName(classNames[x].trim(), this.beanClassLoader);
      if (!cls.isInterface()) {
        throw new IllegalArgumentException(
                "Class [" + classNames[x] + "] mapped to bean key [" + beanKey + "] is no interface");
      }
      classes[x] = cls;
    }
    return classes;
  }

  /**
   * Check to see if the {@code Method} is declared in
   * one of the configured interfaces and that it is public.
   *
   * @param method the accessor {@code Method}.
   * @param beanKey the key associated with the MBean in the
   * {@code beans} {@code Map}.
   * @return {@code true} if the {@code Method} is declared in one of the
   * configured interfaces, otherwise {@code false}.
   */
  @Override
  protected boolean includeReadAttribute(Method method, String beanKey) {
    return isPublicInInterface(method, beanKey);
  }

  /**
   * Check to see if the {@code Method} is declared in
   * one of the configured interfaces and that it is public.
   *
   * @param method the mutator {@code Method}.
   * @param beanKey the key associated with the MBean in the
   * {@code beans} {@code Map}.
   * @return {@code true} if the {@code Method} is declared in one of the
   * configured interfaces, otherwise {@code false}.
   */
  @Override
  protected boolean includeWriteAttribute(Method method, String beanKey) {
    return isPublicInInterface(method, beanKey);
  }

  /**
   * Check to see if the {@code Method} is declared in
   * one of the configured interfaces and that it is public.
   *
   * @param method the operation {@code Method}.
   * @param beanKey the key associated with the MBean in the
   * {@code beans} {@code Map}.
   * @return {@code true} if the {@code Method} is declared in one of the
   * configured interfaces, otherwise {@code false}.
   */
  @Override
  protected boolean includeOperation(Method method, String beanKey) {
    return isPublicInInterface(method, beanKey);
  }

  /**
   * Check to see if the {@code Method} is both public and declared in
   * one of the configured interfaces.
   *
   * @param method the {@code Method} to check.
   * @param beanKey the key associated with the MBean in the beans map
   * @return {@code true} if the {@code Method} is declared in one of the
   * configured interfaces and is public, otherwise {@code false}.
   */
  private boolean isPublicInInterface(Method method, String beanKey) {
    return Modifier.isPublic(method.getModifiers()) && isDeclaredInInterface(method, beanKey);
  }

  /**
   * Checks to see if the given method is declared in a managed
   * interface for the given bean.
   */
  private boolean isDeclaredInInterface(Method method, String beanKey) {
    Class<?>[] ifaces = null;

    if (this.resolvedInterfaceMappings != null) {
      ifaces = this.resolvedInterfaceMappings.get(beanKey);
    }

    if (ifaces == null) {
      ifaces = this.managedInterfaces;
      if (ifaces == null) {
        ifaces = ClassUtils.getAllInterfacesForClass(method.getDeclaringClass());
      }
    }

    for (Class<?> ifc : ifaces) {
      for (Method ifcMethod : ifc.getMethods()) {
        if (ifcMethod.getName().equals(method.getName()) &&
                ifcMethod.getParameterCount() == method.getParameterCount() &&
                Arrays.equals(ifcMethod.getParameterTypes(), method.getParameterTypes())) {
          return true;
        }
      }
    }

    return false;
  }

}
