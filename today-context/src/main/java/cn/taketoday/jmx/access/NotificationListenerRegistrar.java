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

package cn.taketoday.jmx.access;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.jmx.JmxException;
import cn.taketoday.jmx.MBeanServerNotFoundException;
import cn.taketoday.jmx.support.NotificationListenerHolder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;

/**
 * Registrar object that associates a specific {@link javax.management.NotificationListener}
 * with one or more MBeans in an {@link javax.management.MBeanServer}
 * (typically via a {@link MBeanServerConnection}).
 *
 * @author Juergen Hoeller
 * @see #setServer
 * @see #setMappedObjectNames
 * @see #setNotificationListener
 * @since 4.0
 */
public class NotificationListenerRegistrar extends NotificationListenerHolder
        implements InitializingBean, DisposableBean {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final ConnectorDelegate connector = new ConnectorDelegate();

  @Nullable
  private MBeanServerConnection server;

  @Nullable
  private JMXServiceURL serviceUrl;

  @Nullable
  private Map<String, ?> environment;

  @Nullable
  private String agentId;

  @Nullable
  private ObjectName[] actualObjectNames;

  /**
   * Set the {@code MBeanServerConnection} used to connect to the
   * MBean which all invocations are routed to.
   */
  public void setServer(MBeanServerConnection server) {
    this.server = server;
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
   * Set the service URL of the remote {@code MBeanServer}.
   */
  public void setServiceUrl(String url) throws MalformedURLException {
    this.serviceUrl = new JMXServiceURL(url);
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

  @Override
  public void afterPropertiesSet() {
    if (getNotificationListener() == null) {
      throw new IllegalArgumentException("Property 'notificationListener' is required");
    }
    if (CollectionUtils.isEmpty(this.mappedObjectNames)) {
      throw new IllegalArgumentException("Property 'mappedObjectName' is required");
    }
    prepare();
  }

  /**
   * Registers the specified {@code NotificationListener}.
   * <p>Ensures that an {@code MBeanServerConnection} is configured and attempts
   * to detect a local connection if one is not supplied.
   */
  public void prepare() {
    if (this.server == null) {
      this.server = this.connector.connect(this.serviceUrl, this.environment, this.agentId);
    }
    try {
      this.actualObjectNames = getResolvedObjectNames();
      if (this.actualObjectNames != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("Registering NotificationListener for MBeans {}", Arrays.asList(this.actualObjectNames));
        }
        for (ObjectName actualObjectName : this.actualObjectNames) {
          this.server.addNotificationListener(
                  actualObjectName, getNotificationListener(), getNotificationFilter(), getHandback());
        }
      }
    }
    catch (IOException ex) {
      throw new MBeanServerNotFoundException(
              "Could not connect to remote MBeanServer at URL [" + this.serviceUrl + "]", ex);
    }
    catch (Exception ex) {
      throw new JmxException("Unable to register NotificationListener", ex);
    }
  }

  /**
   * Unregisters the specified {@code NotificationListener}.
   */
  @Override
  public void destroy() {
    try {
      if (this.server != null && this.actualObjectNames != null) {
        for (ObjectName actualObjectName : this.actualObjectNames) {
          try {
            this.server.removeNotificationListener(
                    actualObjectName, getNotificationListener(), getNotificationFilter(), getHandback());
          }
          catch (Exception ex) {
            if (logger.isDebugEnabled()) {
              logger.debug("Unable to unregister NotificationListener", ex);
            }
          }
        }
      }
    }
    finally {
      this.connector.close();
    }
  }

}
