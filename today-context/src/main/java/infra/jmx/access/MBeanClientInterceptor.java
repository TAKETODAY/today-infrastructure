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

package infra.jmx.access;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXServiceURL;

import infra.beans.BeanUtils;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.InitializingBean;
import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.jmx.support.JmxUtils;
import infra.jmx.support.ObjectNameManager;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

/**
 * {@link MethodInterceptor} that routes calls to an
 * MBean running on the supplied {@code MBeanServerConnection}.
 * Works for both local and remote {@code MBeanServerConnection}s.
 *
 * <p>By default, the {@code MBeanClientInterceptor} will connect to the
 * {@code MBeanServer} and cache MBean metadata at startup. This can
 * be undesirable when running against a remote {@code MBeanServer}
 * that may not be running when the application starts. Through setting the
 * {@link #setConnectOnStartup(boolean) connectOnStartup} property to "false",
 * you can defer this process until the first invocation against the proxy.
 *
 * <p>This functionality is usually used through {@link MBeanProxyFactoryBean}.
 * See the javadoc of that class for more information.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see MBeanProxyFactoryBean
 * @see #setConnectOnStartup
 * @since 4.0
 */
public class MBeanClientInterceptor
        implements MethodInterceptor, BeanClassLoaderAware, InitializingBean, DisposableBean {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private MBeanServerConnection server;

  @Nullable
  private JMXServiceURL serviceUrl;

  @Nullable
  private Map<String, ?> environment;

  @Nullable
  private String agentId;

  private boolean connectOnStartup = true;

  private boolean refreshOnConnectFailure = false;

  @Nullable
  private ObjectName objectName;

  private boolean useStrictCasing = true;

  @Nullable
  private Class<?> managementInterface;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  private final ConnectorDelegate connector = new ConnectorDelegate();

  @Nullable
  private MBeanServerConnection serverToUse;

  @Nullable
  private MBeanServerInvocationHandler invocationHandler;

  private Map<String, MBeanAttributeInfo> allowedAttributes = Collections.emptyMap();

  private Map<MethodCacheKey, MBeanOperationInfo> allowedOperations = Collections.emptyMap();

  private final Map<Method, String[]> signatureCache = new HashMap<>();

  private final Object preparationMonitor = new Object();

  /**
   * Set the {@code MBeanServerConnection} used to connect to the
   * MBean which all invocations are routed to.
   */
  public void setServer(MBeanServerConnection server) {
    this.server = server;
  }

  /**
   * Set the service URL of the remote {@code MBeanServer}.
   */
  public void setServiceUrl(String url) throws MalformedURLException {
    this.serviceUrl = new JMXServiceURL(url);
  }

  /**
   * Specify the environment for the JMX connector.
   *
   * @see javax.management.remote.JMXConnectorFactory#connect(JMXServiceURL, Map)
   */
  public void setEnvironment(@Nullable Map<String, ?> environment) {
    this.environment = environment;
  }

  /**
   * Allow Map access to the environment to be set for the connector,
   * with the option to add or override specific entries.
   * <p>Useful for specifying entries directly, for example via
   * "environment[myKey]". This is particularly useful for
   * adding or overriding entries in child bean definitions.
   */
  @Nullable
  public Map<String, ?> getEnvironment() {
    return this.environment;
  }

  /**
   * Set the agent id of the {@code MBeanServer} to locate.
   * <p>Default is none. If specified, this will result in an
   * attempt being made to locate the attendant MBeanServer, unless
   * the {@link #setServiceUrl "serviceUrl"} property has been set.
   *
   * @see javax.management.MBeanServerFactory#findMBeanServer(String)
   * <p>Specifying the empty String indicates the platform MBeanServer.
   */
  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  /**
   * Set whether or not the proxy should connect to the {@code MBeanServer}
   * at creation time ("true") or the first time it is invoked ("false").
   * Default is "true".
   */
  public void setConnectOnStartup(boolean connectOnStartup) {
    this.connectOnStartup = connectOnStartup;
  }

  /**
   * Set whether to refresh the MBeanServer connection on connect failure.
   * Default is "false".
   * <p>Can be turned on to allow for hot restart of the JMX server,
   * automatically reconnecting and retrying in case of an IOException.
   */
  public void setRefreshOnConnectFailure(boolean refreshOnConnectFailure) {
    this.refreshOnConnectFailure = refreshOnConnectFailure;
  }

  /**
   * Set the {@code ObjectName} of the MBean which calls are routed to,
   * as {@code ObjectName} instance or as {@code String}.
   */
  public void setObjectName(Object objectName) throws MalformedObjectNameException {
    this.objectName = ObjectNameManager.getInstance(objectName);
  }

  /**
   * Set whether to use strict casing for attributes. Enabled by default.
   * <p>When using strict casing, a JavaBean property with a getter such as
   * {@code getFoo()} translates to an attribute called {@code Foo}.
   * With strict casing disabled, {@code getFoo()} would translate to just
   * {@code foo}.
   */
  public void setUseStrictCasing(boolean useStrictCasing) {
    this.useStrictCasing = useStrictCasing;
  }

  /**
   * Set the management interface of the target MBean, exposing bean property
   * setters and getters for MBean attributes and conventional Java methods
   * for MBean operations.
   */
  public void setManagementInterface(@Nullable Class<?> managementInterface) {
    this.managementInterface = managementInterface;
  }

  /**
   * Return the management interface of the target MBean,
   * or {@code null} if none specified.
   */
  @Nullable
  protected final Class<?> getManagementInterface() {
    return this.managementInterface;
  }

  @Override
  public void setBeanClassLoader(ClassLoader beanClassLoader) {
    this.beanClassLoader = beanClassLoader;
  }

  /**
   * Prepares the {@code MBeanServerConnection} if the "connectOnStartup"
   * is turned on (which it is by default).
   */
  @Override
  public void afterPropertiesSet() {
    if (this.server != null && this.refreshOnConnectFailure) {
      throw new IllegalArgumentException("'refreshOnConnectFailure' does not work when setting " +
              "a 'server' reference. Prefer 'serviceUrl' etc instead.");
    }
    if (this.connectOnStartup) {
      prepare();
    }
  }

  /**
   * Ensures that an {@code MBeanServerConnection} is configured and attempts
   * to detect a local connection if one is not supplied.
   */
  public void prepare() {
    synchronized(this.preparationMonitor) {
      if (this.server != null) {
        this.serverToUse = this.server;
      }
      else {
        this.serverToUse = null;
        this.serverToUse = this.connector.connect(this.serviceUrl, this.environment, this.agentId);
      }
      this.invocationHandler = null;
      if (this.useStrictCasing) {
        Assert.state(this.objectName != null, "No ObjectName set");
        // Use the JDK's own MBeanServerInvocationHandler, in particular for native MXBean support.
        this.invocationHandler = new MBeanServerInvocationHandler(this.serverToUse, this.objectName,
                (this.managementInterface != null && JMX.isMXBeanInterface(this.managementInterface)));
      }
      else {
        // Non-strict casing can only be achieved through custom invocation handling.
        // Only partial MXBean support available!
        retrieveMBeanInfo(this.serverToUse);
      }
    }
  }

  /**
   * Loads the management interface info for the configured MBean into the caches.
   * This information is used by the proxy when determining whether an invocation matches
   * a valid operation or attribute on the management interface of the managed resource.
   */
  private void retrieveMBeanInfo(MBeanServerConnection server) throws MBeanInfoRetrievalException {
    try {
      MBeanInfo info = server.getMBeanInfo(this.objectName);

      MBeanAttributeInfo[] attributeInfo = info.getAttributes();
      this.allowedAttributes = CollectionUtils.newHashMap(attributeInfo.length);
      for (MBeanAttributeInfo infoEle : attributeInfo) {
        this.allowedAttributes.put(infoEle.getName(), infoEle);
      }

      MBeanOperationInfo[] operationInfo = info.getOperations();
      this.allowedOperations = CollectionUtils.newHashMap(operationInfo.length);
      for (MBeanOperationInfo infoEle : operationInfo) {
        Class<?>[] paramTypes = JmxUtils.parameterInfoToTypes(infoEle.getSignature(), this.beanClassLoader);
        this.allowedOperations.put(new MethodCacheKey(infoEle.getName(), paramTypes), infoEle);
      }
    }
    catch (ClassNotFoundException ex) {
      throw new MBeanInfoRetrievalException("Unable to locate class specified in method signature", ex);
    }
    catch (IntrospectionException ex) {
      throw new MBeanInfoRetrievalException("Unable to obtain MBean info for bean [" + this.objectName + "]", ex);
    }
    catch (InstanceNotFoundException ex) {
      // if we are this far this shouldn't happen, but...
      throw new MBeanInfoRetrievalException("Unable to obtain MBean info for bean [" + this.objectName +
              "]: it is likely that this bean was unregistered during the proxy creation process",
              ex);
    }
    catch (ReflectionException ex) {
      throw new MBeanInfoRetrievalException("Unable to read MBean info for bean [ " + this.objectName + "]", ex);
    }
    catch (IOException ex) {
      throw new MBeanInfoRetrievalException("An IOException occurred when communicating with the " +
              "MBeanServer. It is likely that you are communicating with a remote MBeanServer. " +
              "Check the inner exception for exact details.", ex);
    }
  }

  /**
   * Return whether this client interceptor has already been prepared,
   * i.e. has already looked up the server and cached all metadata.
   */
  protected boolean isPrepared() {
    synchronized(this.preparationMonitor) {
      return (this.serverToUse != null);
    }
  }

  /**
   * Route the invocation to the configured managed resource..
   *
   * @param invocation the {@code MethodInvocation} to re-route
   * @return the value returned as a result of the re-routed invocation
   * @throws Throwable an invocation error propagated to the user
   * @see #doInvoke
   * @see #handleConnectFailure
   */
  @Override
  @Nullable
  public Object invoke(MethodInvocation invocation) throws Throwable {
    // Lazily connect to MBeanServer if necessary.
    synchronized(this.preparationMonitor) {
      if (!isPrepared()) {
        prepare();
      }
    }
    try {
      return doInvoke(invocation);
    }
    catch (MBeanConnectFailureException | IOException ex) {
      return handleConnectFailure(invocation, ex);
    }
  }

  /**
   * Refresh the connection and retry the MBean invocation if possible.
   * <p>If not configured to refresh on connect failure, this method
   * simply rethrows the original exception.
   *
   * @param invocation the invocation that failed
   * @param ex the exception raised on remote invocation
   * @return the result value of the new invocation, if succeeded
   * @throws Throwable an exception raised by the new invocation,
   * if it failed as well
   * @see #setRefreshOnConnectFailure
   * @see #doInvoke
   */
  @Nullable
  protected Object handleConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
    if (this.refreshOnConnectFailure) {
      String msg = "Could not connect to JMX server - retrying";
      if (logger.isDebugEnabled()) {
        logger.warn(msg, ex);
      }
      else if (logger.isWarnEnabled()) {
        logger.warn(msg);
      }
      prepare();
      return doInvoke(invocation);
    }
    else {
      throw ex;
    }
  }

  /**
   * Route the invocation to the configured managed resource. Correctly routes JavaBean property
   * access to {@code MBeanServerConnection.get/setAttribute} and method invocation to
   * {@code MBeanServerConnection.invoke}.
   *
   * @param invocation the {@code MethodInvocation} to re-route
   * @return the value returned as a result of the re-routed invocation
   * @throws Throwable an invocation error propagated to the user
   */
  @Nullable
  protected Object doInvoke(MethodInvocation invocation) throws Throwable {
    Method method = invocation.getMethod();
    try {
      Object result;
      if (this.invocationHandler != null) {
        result = this.invocationHandler.invoke(invocation.getThis(), method, invocation.getArguments());
      }
      else {
        PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
        if (pd != null) {
          result = invokeAttribute(pd, invocation);
        }
        else {
          result = invokeOperation(method, invocation.getArguments());
        }
      }
      return convertResultValueIfNecessary(result, new MethodParameter(method, -1));
    }
    catch (MBeanException ex) {
      throw ex.getTargetException();
    }
    catch (RuntimeMBeanException ex) {
      throw ex.getTargetException();
    }
    catch (RuntimeErrorException ex) {
      throw ex.getTargetError();
    }
    catch (RuntimeOperationsException ex) {
      // This one is only thrown by the JMX 2.0 RI, not by the JDK 1.5 JMX code.
      RuntimeException rex = ex.getTargetException();
      if (rex instanceof RuntimeMBeanException) {
        throw ((RuntimeMBeanException) rex).getTargetException();
      }
      else if (rex instanceof RuntimeErrorException) {
        throw ((RuntimeErrorException) rex).getTargetError();
      }
      else {
        throw rex;
      }
    }
    catch (OperationsException ex) {
      if (ReflectionUtils.declaresException(method, ex.getClass())) {
        throw ex;
      }
      else {
        throw new InvalidInvocationException(ex.getMessage());
      }
    }
    catch (JMException ex) {
      if (ReflectionUtils.declaresException(method, ex.getClass())) {
        throw ex;
      }
      else {
        throw new InvocationFailureException("JMX access failed", ex);
      }
    }
    catch (IOException ex) {
      if (ReflectionUtils.declaresException(method, ex.getClass())) {
        throw ex;
      }
      else {
        throw new MBeanConnectFailureException("I/O failure during JMX access", ex);
      }
    }
  }

  @Nullable
  private Object invokeAttribute(PropertyDescriptor pd, MethodInvocation invocation)
          throws JMException, IOException {

    Assert.state(this.serverToUse != null, "No MBeanServerConnection available");

    String attributeName = JmxUtils.getAttributeName(pd, this.useStrictCasing);
    MBeanAttributeInfo inf = this.allowedAttributes.get(attributeName);
    // If no attribute is returned, we know that it is not defined in the
    // management interface.
    if (inf == null) {
      throw new InvalidInvocationException(
              "Attribute '" + pd.getName() + "' is not exposed on the management interface");
    }

    if (invocation.getMethod().equals(pd.getReadMethod())) {
      if (inf.isReadable()) {
        return this.serverToUse.getAttribute(this.objectName, attributeName);
      }
      else {
        throw new InvalidInvocationException("Attribute '" + attributeName + "' is not readable");
      }
    }
    else if (invocation.getMethod().equals(pd.getWriteMethod())) {
      if (inf.isWritable()) {
        this.serverToUse.setAttribute(this.objectName, new Attribute(attributeName, invocation.getArguments()[0]));
        return null;
      }
      else {
        throw new InvalidInvocationException("Attribute '" + attributeName + "' is not writable");
      }
    }
    else {
      throw new IllegalStateException(
              "Method [" + invocation.getMethod() + "] is neither a bean property getter nor a setter");
    }
  }

  /**
   * Routes a method invocation (not a property get/set) to the corresponding
   * operation on the managed resource.
   *
   * @param method the method corresponding to operation on the managed resource.
   * @param args the invocation arguments
   * @return the value returned by the method invocation.
   */
  private Object invokeOperation(Method method, Object[] args) throws JMException, IOException {
    Assert.state(this.serverToUse != null, "No MBeanServerConnection available");

    MethodCacheKey key = new MethodCacheKey(method.getName(), method.getParameterTypes());
    MBeanOperationInfo info = this.allowedOperations.get(key);
    if (info == null) {
      throw new InvalidInvocationException("Operation '" + method.getName() +
              "' is not exposed on the management interface");
    }

    String[] signature;
    synchronized(this.signatureCache) {
      signature = this.signatureCache.get(method);
      if (signature == null) {
        signature = JmxUtils.getMethodSignature(method);
        this.signatureCache.put(method, signature);
      }
    }

    return this.serverToUse.invoke(this.objectName, method.getName(), args, signature);
  }

  /**
   * Convert the given result object (from attribute access or operation invocation)
   * to the specified target class for returning from the proxy method.
   *
   * @param result the result object as returned by the {@code MBeanServer}
   * @param parameter the method parameter of the proxy method that's been invoked
   * @return the converted result object, or the passed-in object if no conversion
   * is necessary
   */
  @Nullable
  protected Object convertResultValueIfNecessary(@Nullable Object result, MethodParameter parameter) {
    Class<?> targetClass = parameter.getParameterType();
    try {
      if (result == null) {
        return null;
      }
      if (ClassUtils.isAssignableValue(targetClass, result)) {
        return result;
      }
      if (result instanceof CompositeData) {
        Method fromMethod = targetClass.getMethod("from", CompositeData.class);
        return ReflectionUtils.invokeMethod(fromMethod, null, result);
      }
      else if (result instanceof CompositeData[] array) {
        if (targetClass.isArray()) {
          return convertDataArrayToTargetArray(array, targetClass);
        }
        else if (Collection.class.isAssignableFrom(targetClass)) {
          Class<?> elementType =
                  ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
          if (elementType != null) {
            return convertDataArrayToTargetCollection(array, targetClass, elementType);
          }
        }
      }
      else if (result instanceof TabularData) {
        Method fromMethod = targetClass.getMethod("from", TabularData.class);
        return ReflectionUtils.invokeMethod(fromMethod, null, result);
      }
      else if (result instanceof TabularData[] array) {
        if (targetClass.isArray()) {
          return convertDataArrayToTargetArray(array, targetClass);
        }
        else if (Collection.class.isAssignableFrom(targetClass)) {
          Class<?> elementType =
                  ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
          if (elementType != null) {
            return convertDataArrayToTargetCollection(array, targetClass, elementType);
          }
        }
      }
      throw new InvocationFailureException(
              "Incompatible result value [" + result + "] for target type [" + targetClass.getName() + "]");
    }
    catch (NoSuchMethodException ex) {
      throw new InvocationFailureException(
              "Could not obtain 'from(CompositeData)' / 'from(TabularData)' method on target type [" +
                      targetClass.getName() + "] for conversion of MXBean data structure [" + result + "]");
    }
  }

  private Object convertDataArrayToTargetArray(Object[] array, Class<?> targetClass) throws NoSuchMethodException {
    Class<?> targetType = targetClass.getComponentType();
    Method fromMethod = targetType.getMethod("from", array.getClass().getComponentType());
    Object resultArray = Array.newInstance(targetType, array.length);
    for (int i = 0; i < array.length; i++) {
      Array.set(resultArray, i, ReflectionUtils.invokeMethod(fromMethod, null, array[i]));
    }
    return resultArray;
  }

  private Collection<?> convertDataArrayToTargetCollection(Object[] array, Class<?> collectionType, Class<?> elementType)
          throws NoSuchMethodException {

    Method fromMethod = elementType.getMethod("from", array.getClass().getComponentType());
    Collection<Object> resultColl = CollectionUtils.createCollection(collectionType, Array.getLength(array));
    for (Object element : array) {
      resultColl.add(ReflectionUtils.invokeMethod(fromMethod, null, element));
    }
    return resultColl;
  }

  @Override
  public void destroy() {
    this.connector.close();
  }

  /**
   * Simple wrapper class around a method name and its signature.
   * Used as the key when caching methods.
   */
  private record MethodCacheKey(String name, Class<?>[] parameterTypes)
          implements Comparable<MethodCacheKey> {

    /**
     * Create a new instance of {@code MethodCacheKey} with the supplied
     * method name and parameter list.
     *
     * @param name the name of the method
     * @param parameterTypes the arguments in the method signature
     */
    private MethodCacheKey(String name, @Nullable Class<?>[] parameterTypes) {
      this.name = name;
      this.parameterTypes = (parameterTypes != null ? parameterTypes : new Class<?>[0]);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof MethodCacheKey otherKey)) {
        return false;
      }
      return (this.name.equals(otherKey.name) && Arrays.equals(this.parameterTypes, otherKey.parameterTypes));
    }

    @Override
    public int hashCode() {
      return this.name.hashCode();
    }

    @Override
    public String toString() {
      return this.name + "(" + StringUtils.arrayToCommaDelimitedString(this.parameterTypes) + ")";
    }

    @Override
    public int compareTo(MethodCacheKey other) {
      int result = this.name.compareTo(other.name);
      if (result != 0) {
        return result;
      }
      if (this.parameterTypes.length < other.parameterTypes.length) {
        return -1;
      }
      if (this.parameterTypes.length > other.parameterTypes.length) {
        return 1;
      }
      for (int i = 0; i < this.parameterTypes.length; i++) {
        result = this.parameterTypes[i].getName().compareTo(other.parameterTypes[i].getName());
        if (result != 0) {
          return result;
        }
      }
      return 0;
    }
  }

}
