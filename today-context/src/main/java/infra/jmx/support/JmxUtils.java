/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.jmx.support;

import java.beans.PropertyDescriptor;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;

import javax.management.DynamicMBean;
import javax.management.JMX;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import infra.beans.BeanProperty;
import infra.jmx.MBeanServerNotFoundException;
import infra.jmx.export.MBeanExporter;
import infra.jmx.export.naming.ObjectNamingStrategy;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;

/**
 * Collection of generic utility methods to support Framework JMX.
 * Includes a convenient method to locate an MBeanServer.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see #locateMBeanServer
 * @since 4.0
 */
public abstract class JmxUtils {

  /**
   * The key used when extending an existing {@link ObjectName} with the
   * identity hash code of its corresponding managed resource.
   */
  public static final String IDENTITY_OBJECT_NAME_KEY = "identity";

  /**
   * Suffix used to identify an MBean interface.
   */
  private static final String MBEAN_SUFFIX = "MBean";

  private static final Logger logger = LoggerFactory.getLogger(JmxUtils.class);

  /**
   * Attempt to find a locally running {@code MBeanServer}. Fails if no
   * {@code MBeanServer} can be found. Logs a warning if more than one
   * {@code MBeanServer} found, returning the first one from the list.
   *
   * @return the {@code MBeanServer} if found
   * @throws MBeanServerNotFoundException if no {@code MBeanServer} could be found
   * @see MBeanServerFactory#findMBeanServer
   */
  public static MBeanServer locateMBeanServer() throws MBeanServerNotFoundException {
    return locateMBeanServer(null);
  }

  /**
   * Attempt to find a locally running {@code MBeanServer}. Fails if no
   * {@code MBeanServer} can be found. Logs a warning if more than one
   * {@code MBeanServer} found, returning the first one from the list.
   *
   * @param agentId the agent identifier of the MBeanServer to retrieve.
   * If this parameter is {@code null}, all registered MBeanServers are considered.
   * If the empty String is given, the platform MBeanServer will be returned.
   * @return the {@code MBeanServer} if found
   * @throws MBeanServerNotFoundException if no {@code MBeanServer} could be found
   * @see MBeanServerFactory#findMBeanServer(String)
   */
  public static MBeanServer locateMBeanServer(@Nullable String agentId) throws MBeanServerNotFoundException {
    MBeanServer server = null;

    // null means any registered server, but "" specifically means the platform server
    if (!"".equals(agentId)) {
      List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(agentId);
      if (CollectionUtils.isNotEmpty(servers)) {
        // Check to see if an MBeanServer is registered.
        if (servers.size() > 1 && logger.isInfoEnabled()) {
          logger.info("Found more than one MBeanServer instance{}. Returning first from list.",
                  (agentId != null ? " with agent id [" + agentId + "]" : ""));
        }
        server = servers.get(0);
      }
    }

    if (server == null && StringUtils.isEmpty(agentId)) {
      // Attempt to load the PlatformMBeanServer.
      try {
        server = ManagementFactory.getPlatformMBeanServer();
      }
      catch (SecurityException ex) {
        throw new MBeanServerNotFoundException("No specific MBeanServer found, " +
                "and not allowed to obtain the Java platform MBeanServer", ex);
      }
    }

    if (server == null) {
      throw new MBeanServerNotFoundException(
              "Unable to locate an MBeanServer instance" +
                      (agentId != null ? " with agent id [" + agentId + "]" : ""));
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Found MBeanServer: {}", server);
    }
    return server;
  }

  /**
   * Convert an array of {@code MBeanParameterInfo} into an array of
   * {@code Class} instances corresponding to the parameters.
   *
   * @param paramInfo the JMX parameter info
   * @return the parameter types as classes
   * @throws ClassNotFoundException if a parameter type could not be resolved
   */
  @Nullable
  public static Class<?>[] parameterInfoToTypes(@Nullable MBeanParameterInfo[] paramInfo)
          throws ClassNotFoundException {

    return parameterInfoToTypes(paramInfo, ClassUtils.getDefaultClassLoader());
  }

  /**
   * Convert an array of {@code MBeanParameterInfo} into an array of
   * {@code Class} instances corresponding to the parameters.
   *
   * @param paramInfo the JMX parameter info
   * @param classLoader the ClassLoader to use for loading parameter types
   * @return the parameter types as classes
   * @throws ClassNotFoundException if a parameter type could not be resolved
   */
  @Nullable
  public static Class<?>[] parameterInfoToTypes(
          @Nullable MBeanParameterInfo[] paramInfo, @Nullable ClassLoader classLoader)
          throws ClassNotFoundException {

    Class<?>[] types = null;
    if (paramInfo != null && paramInfo.length > 0) {
      types = new Class<?>[paramInfo.length];
      for (int x = 0; x < paramInfo.length; x++) {
        types[x] = ClassUtils.forName(paramInfo[x].getType(), classLoader);
      }
    }
    return types;
  }

  /**
   * Create a {@code String[]} representing the argument signature of a
   * method. Each element in the array is the fully qualified class name
   * of the corresponding argument in the methods signature.
   *
   * @param method the method to build an argument signature for
   * @return the signature as array of argument types
   */
  public static String[] getMethodSignature(Method method) {
    Class<?>[] types = method.getParameterTypes();
    String[] signature = new String[types.length];
    for (int x = 0; x < types.length; x++) {
      signature[x] = types[x].getName();
    }
    return signature;
  }

  /**
   * Return the JMX attribute name to use for the given JavaBeans property.
   * <p>When using strict casing, a JavaBean property with a getter method
   * such as {@code getFoo()} translates to an attribute called
   * {@code Foo}. With strict casing disabled, {@code getFoo()}
   * would translate to just {@code foo}.
   *
   * @param property the JavaBeans property descriptor
   * @param useStrictCasing whether to use strict casing
   * @return the JMX attribute name to use
   */
  public static String getAttributeName(PropertyDescriptor property, boolean useStrictCasing) {
    if (useStrictCasing) {
      return StringUtils.capitalize(property.getName());
    }
    else {
      return property.getName();
    }
  }

  /**
   * Return the JMX attribute name to use for the given JavaBeans property.
   * <p>When using strict casing, a JavaBean property with a getter method
   * such as {@code getFoo()} translates to an attribute called
   * {@code Foo}. With strict casing disabled, {@code getFoo()}
   * would translate to just {@code foo}.
   *
   * @param property the JavaBeans BeanProperty
   * @param useStrictCasing whether to use strict casing
   * @return the JMX attribute name to use
   */
  public static String getAttributeName(BeanProperty property, boolean useStrictCasing) {
    if (useStrictCasing) {
      return StringUtils.capitalize(property.getName());
    }
    else {
      return property.getName();
    }
  }

  /**
   * Append an additional key/value pair to an existing {@link ObjectName} with the key being
   * the static value {@code identity} and the value being the identity hash code of the
   * managed resource being exposed on the supplied {@link ObjectName}. This can be used to
   * provide a unique {@link ObjectName} for each distinct instance of a particular bean or
   * class. Useful when generating {@link ObjectName ObjectNames} at runtime for a set of
   * managed resources based on the template value supplied by a
   * {@link ObjectNamingStrategy}.
   *
   * @param objectName the original JMX ObjectName
   * @param managedResource the MBean instance
   * @return an ObjectName with the MBean identity added
   * @throws MalformedObjectNameException in case of an invalid object name specification
   * @see ObjectUtils#getIdentityHexString(Object)
   */
  public static ObjectName appendIdentityToObjectName(ObjectName objectName, Object managedResource)
          throws MalformedObjectNameException {

    Hashtable<String, String> keyProperties = objectName.getKeyPropertyList();
    keyProperties.put(IDENTITY_OBJECT_NAME_KEY, ObjectUtils.getIdentityHexString(managedResource));
    return ObjectNameManager.getInstance(objectName.getDomain(), keyProperties);
  }

  /**
   * Return the class or interface to expose for the given bean.
   * This is the class that will be searched for attributes and operations
   * (for example, checked for annotations).
   * <p>This implementation returns the superclass for a CGLIB proxy and
   * the class of the given bean else (for a JDK proxy or a plain bean class).
   *
   * @param managedBean the bean instance (might be an AOP proxy)
   * @return the bean class to expose
   * @see ClassUtils#getUserClass(Object)
   */
  public static Class<?> getClassToExpose(Object managedBean) {
    return ClassUtils.getUserClass(managedBean);
  }

  /**
   * Return the class or interface to expose for the given bean class.
   * This is the class that will be searched for attributes and operations
   * (for example, checked for annotations).
   * <p>This implementation returns the superclass for a CGLIB proxy and
   * the class of the given bean else (for a JDK proxy or a plain bean class).
   *
   * @param clazz the bean class (might be an AOP proxy class)
   * @return the bean class to expose
   * @see ClassUtils#getUserClass(Class)
   */
  public static Class<?> getClassToExpose(Class<?> clazz) {
    return ClassUtils.getUserClass(clazz);
  }

  /**
   * Determine whether the given bean class qualifies as an MBean as-is.
   * <p>This implementation checks for {@link DynamicMBean}
   * classes as well as classes with corresponding "*MBean" interface
   * (Standard MBeans) or corresponding "*MXBean" interface (Java 6 MXBeans).
   *
   * @param clazz the bean class to analyze
   * @return whether the class qualifies as an MBean
   * @see MBeanExporter#isMBean(Class)
   */
  public static boolean isMBean(@Nullable Class<?> clazz) {
    return (clazz != null &&
            (DynamicMBean.class.isAssignableFrom(clazz) ||
                    (getMBeanInterface(clazz) != null || getMXBeanInterface(clazz) != null)));
  }

  /**
   * Return the Standard MBean interface for the given class, if any
   * (that is, an interface whose name matches the class name of the
   * given class but with suffix "MBean").
   *
   * @param clazz the class to check
   * @return the Standard MBean interface for the given class
   */
  @Nullable
  public static Class<?> getMBeanInterface(@Nullable Class<?> clazz) {
    if (clazz == null || clazz.getSuperclass() == null) {
      return null;
    }
    String mbeanInterfaceName = clazz.getName() + MBEAN_SUFFIX;
    Class<?>[] implementedInterfaces = clazz.getInterfaces();
    for (Class<?> iface : implementedInterfaces) {
      if (iface.getName().equals(mbeanInterfaceName)) {
        return iface;
      }
    }
    return getMBeanInterface(clazz.getSuperclass());
  }

  /**
   * Return the Java 6 MXBean interface exists for the given class, if any
   * (that is, an interface whose name ends with "MXBean" and/or
   * carries an appropriate MXBean annotation).
   *
   * @param clazz the class to check
   * @return whether there is an MXBean interface for the given class
   */
  @Nullable
  public static Class<?> getMXBeanInterface(@Nullable Class<?> clazz) {
    if (clazz == null || clazz.getSuperclass() == null) {
      return null;
    }
    Class<?>[] implementedInterfaces = clazz.getInterfaces();
    for (Class<?> iface : implementedInterfaces) {
      if (JMX.isMXBeanInterface(iface)) {
        return iface;
      }
    }
    return getMXBeanInterface(clazz.getSuperclass());
  }

}
