/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.annotation.config.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import infra.app.context.event.ApplicationReadyEvent;
import infra.beans.BeansException;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.InitializingBean;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.ApplicationEvent;
import infra.context.ConfigurableApplicationContext;
import infra.context.EnvironmentAware;
import infra.context.event.GenericApplicationListener;
import infra.core.Ordered;
import infra.core.ResolvableType;
import infra.core.env.Environment;
import infra.core.env.StandardEnvironment;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.web.server.context.WebServerApplicationContext;

/**
 * Register a {@link InfraApplicationMXBean} implementation to the platform
 * {@link MBeanServer}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("NullAway")
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
    if (ClassUtils.isPresent("infra.web.server.context.WebServerApplicationContext", context.getClassLoader())) {
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

  private final class ApplicationMXBeanImpl implements InfraApplicationMXBean {

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
