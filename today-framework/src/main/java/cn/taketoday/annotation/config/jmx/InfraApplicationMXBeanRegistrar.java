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

package cn.taketoday.annotation.config.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.event.GenericApplicationListener;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.framework.context.event.ApplicationReadyEvent;
import cn.taketoday.framework.web.context.WebServerInitializedEvent;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Register a {@link InfraApplicationMBean} implementation to the platform
 * {@link MBeanServer}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InfraApplicationMXBeanRegistrar implements ApplicationContextAware,
        GenericApplicationListener, EnvironmentAware, InitializingBean, DisposableBean {

  private static final Logger logger = LoggerFactory.getLogger(InfraApplicationMXBeanRegistrar.class);

  private ConfigurableApplicationContext applicationContext;

  private Environment environment = new StandardEnvironment();

  private final ObjectName objectName;

  private boolean ready = false;

  private boolean embeddedWebApplication = false;

  public InfraApplicationMXBeanRegistrar(String name) throws MalformedObjectNameException {
    this.objectName = new ObjectName(name);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    Assert.state(applicationContext instanceof ConfigurableApplicationContext,
            "ApplicationContext does not implement ConfigurableApplicationContext");
    this.applicationContext = (ConfigurableApplicationContext) applicationContext;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public boolean supportsEventType(ResolvableType eventType) {
    Class<?> type = eventType.getRawClass();
    if (type == null) {
      return false;
    }
    return ApplicationReadyEvent.class.isAssignableFrom(type)
            || WebServerInitializedEvent.class.isAssignableFrom(type);
  }

  @Override
  public boolean supportsSourceType(Class<?> sourceType) {
    return true;
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof ApplicationReadyEvent readyEvent) {
      onApplicationReadyEvent(readyEvent);
    }
    if (event instanceof WebServerInitializedEvent initializedEvent) {
      onWebServerInitializedEvent(initializedEvent);
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  void onApplicationReadyEvent(ApplicationReadyEvent event) {
    if (this.applicationContext.equals(event.getApplicationContext())) {
      this.ready = true;
    }
  }

  void onWebServerInitializedEvent(WebServerInitializedEvent event) {
    if (this.applicationContext.equals(event.getApplicationContext())) {
      this.embeddedWebApplication = true;
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    server.registerMBean(new ApplicationMBeanImpl(), this.objectName);
    if (logger.isDebugEnabled()) {
      logger.debug("Application Admin MBean registered with name '{}'", objectName);
    }
  }

  @Override
  public void destroy() throws Exception {
    ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.objectName);
  }

  private class ApplicationMBeanImpl implements InfraApplicationMBean {

    @Override
    public boolean isReady() {
      return ready;
    }

    @Override
    public boolean isEmbeddedWebApplication() {
      return embeddedWebApplication;
    }

    @Override
    public String getProperty(String key) {
      return environment.getProperty(key);
    }

    @Override
    public void shutdown() {
      logger.info("Application shutdown requested.");
      applicationContext.close();
    }

  }

}
