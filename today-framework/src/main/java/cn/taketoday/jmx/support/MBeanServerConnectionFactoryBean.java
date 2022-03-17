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

package cn.taketoday.jmx.support;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.target.AbstractLazyCreationTargetSource;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.jmx.access.MBeanClientInterceptor;
import cn.taketoday.jmx.access.NotificationListenerRegistrar;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

/**
 * {@link FactoryBean} that creates a JMX 1.2 {@code MBeanServerConnection}
 * to a remote {@code MBeanServer} exposed via a {@code JMXServerConnector}.
 * Exposes the {@code MBeanServer} for bean references.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see MBeanServerFactoryBean
 * @see ConnectorServerFactoryBean
 * @see MBeanClientInterceptor#setServer
 * @see NotificationListenerRegistrar#setServer
 * @since 4.0
 */
public class MBeanServerConnectionFactoryBean
        implements FactoryBean<MBeanServerConnection>, BeanClassLoaderAware, InitializingBean, DisposableBean {

  @Nullable
  private JMXServiceURL serviceUrl;

  private final Map<String, Object> environment = new HashMap<>();

  private boolean connectOnStartup = true;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  @Nullable
  private JMXConnector connector;

  @Nullable
  private MBeanServerConnection connection;

  @Nullable
  private JMXConnectorLazyInitTargetSource connectorTargetSource;

  /**
   * Set the service URL of the remote {@code MBeanServer}.
   */
  public void setServiceUrl(String url) throws MalformedURLException {
    this.serviceUrl = new JMXServiceURL(url);
  }

  /**
   * Set the environment properties used to construct the {@code JMXConnector}
   * as {@code java.util.Properties} (String key/value pairs).
   */
  public void setEnvironment(Properties environment) {
    CollectionUtils.mergePropertiesIntoMap(environment, this.environment);
  }

  /**
   * Set the environment properties used to construct the {@code JMXConnector}
   * as a {@code Map} of String keys and arbitrary Object values.
   */
  public void setEnvironmentMap(@Nullable Map<String, ?> environment) {
    if (environment != null) {
      this.environment.putAll(environment);
    }
  }

  /**
   * Set whether to connect to the server on startup.
   * <p>Default is {@code true}.
   * <p>Can be turned off to allow for late start of the JMX server.
   * In this case, the JMX connector will be fetched on first access.
   */
  public void setConnectOnStartup(boolean connectOnStartup) {
    this.connectOnStartup = connectOnStartup;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  /**
   * Creates a {@code JMXConnector} for the given settings
   * and exposes the associated {@code MBeanServerConnection}.
   */
  @Override
  public void afterPropertiesSet() throws IOException {
    if (this.serviceUrl == null) {
      throw new IllegalArgumentException("Property 'serviceUrl' is required");
    }

    if (this.connectOnStartup) {
      connect();
    }
    else {
      createLazyConnection();
    }
  }

  /**
   * Connects to the remote {@code MBeanServer} using the configured service URL and
   * environment properties.
   */
  private void connect() throws IOException {
    Assert.state(this.serviceUrl != null, "No JMXServiceURL set");
    this.connector = JMXConnectorFactory.connect(this.serviceUrl, this.environment);
    this.connection = this.connector.getMBeanServerConnection();
  }

  /**
   * Creates lazy proxies for the {@code JMXConnector} and {@code MBeanServerConnection}.
   */
  private void createLazyConnection() {
    this.connectorTargetSource = new JMXConnectorLazyInitTargetSource();
    TargetSource connectionTargetSource = new MBeanServerConnectionLazyInitTargetSource();

    this.connector = (JMXConnector)
            new ProxyFactory(JMXConnector.class, this.connectorTargetSource).getProxy(this.beanClassLoader);
    this.connection = (MBeanServerConnection)
            new ProxyFactory(MBeanServerConnection.class, connectionTargetSource).getProxy(this.beanClassLoader);
  }

  @Override
  @Nullable
  public MBeanServerConnection getObject() {
    return this.connection;
  }

  @Override
  public Class<? extends MBeanServerConnection> getObjectType() {
    return (this.connection != null ? this.connection.getClass() : MBeanServerConnection.class);
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  /**
   * Closes the underlying {@code JMXConnector}.
   */
  @Override
  public void destroy() throws IOException {
    if (this.connector != null &&
            (this.connectorTargetSource == null || this.connectorTargetSource.isInitialized())) {
      this.connector.close();
    }
  }

  /**
   * Lazily creates a {@code JMXConnector} using the configured service URL
   * and environment properties.
   *
   * @see MBeanServerConnectionFactoryBean#setServiceUrl(String)
   * @see MBeanServerConnectionFactoryBean#setEnvironment(Properties)
   */
  private class JMXConnectorLazyInitTargetSource extends AbstractLazyCreationTargetSource {

    @Override
    protected Object createObject() throws Exception {
      Assert.state(serviceUrl != null, "No JMXServiceURL set");
      return JMXConnectorFactory.connect(serviceUrl, environment);
    }

    @Override
    public Class<?> getTargetClass() {
      return JMXConnector.class;
    }
  }

  /**
   * Lazily creates an {@code MBeanServerConnection}.
   */
  private class MBeanServerConnectionLazyInitTargetSource extends AbstractLazyCreationTargetSource {

    @Override
    protected Object createObject() throws Exception {
      Assert.state(connector != null, "JMXConnector not initialized");
      return connector.getMBeanServerConnection();
    }

    @Override
    public Class<?> getTargetClass() {
      return MBeanServerConnection.class;
    }
  }

}
