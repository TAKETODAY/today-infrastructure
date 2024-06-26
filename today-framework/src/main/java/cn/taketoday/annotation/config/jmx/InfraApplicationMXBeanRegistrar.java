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

package cn.taketoday.annotation.config.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.EnvironmentAware;
import cn.taketoday.context.event.GenericApplicationListener;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.framework.context.event.ApplicationReadyEvent;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.server.context.WebServerApplicationContext;

/**
 * Register a {@link InfraApplicationMXBean} implementation to the platform
 * {@link MBeanServer}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InfraApplicationMXBeanRegistrar implements ApplicationContextAware,
        GenericApplicationListener, EnvironmentAware, InitializingBean, DisposableBean {

  private final Logger logger = LoggerFactory.getLogger(getClass());

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
    return ApplicationReadyEvent.class.isAssignableFrom(type);
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
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  void onApplicationReadyEvent(ApplicationReadyEvent event) {
    var context = event.getApplicationContext();
    if (this.applicationContext.equals(context)) {
      this.ready = true;
    }
    if (ClassUtils.isPresent("cn.taketoday.web.server.context.WebServerApplicationContext", context.getClassLoader())) {
      if (context instanceof WebServerApplicationContext) {
        this.embeddedWebApplication = true;
      }
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    server.registerMBean(new ApplicationMXBeanImpl(), this.objectName);
    logger.debug("Application Admin MBean registered with name '{}'", objectName);
  }

  @Override
  public void destroy() throws Exception {
    ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.objectName);
  }

  private class ApplicationMXBeanImpl implements InfraApplicationMXBean {

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
