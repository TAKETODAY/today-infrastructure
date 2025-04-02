/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.weaving;

import java.lang.instrument.ClassFileTransformer;

import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.DisposableBean;
import infra.instrument.InstrumentationSavingAgent;
import infra.instrument.classloading.InstrumentationLoadTimeWeaver;
import infra.instrument.classloading.LoadTimeWeaver;
import infra.instrument.classloading.ReflectiveLoadTimeWeaver;
import infra.instrument.classloading.TomcatLoadTimeWeaver;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Default {@link LoadTimeWeaver} bean for use in an application context,
 * decorating an automatically detected internal {@code LoadTimeWeaver}.
 *
 * <p>Typically registered for the default bean name "{@code loadTimeWeaver}";
 * the most convenient way to achieve this is Framework's
 * {@code <context:load-time-weaver>} XML tag or {@code @EnableLoadTimeWeaving}
 * on a {@code @Configuration} class.
 *
 * <p>This class implements a runtime environment check for obtaining the
 * appropriate weaver implementation, including
 * {@link InstrumentationSavingAgent Framework's VM agent} and any {@link ClassLoader}
 * supported by Framework's {@link ReflectiveLoadTimeWeaver}.
 *
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Costin Leau
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.context.ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 * @since 4.0
 */
public class DefaultContextLoadTimeWeaver implements LoadTimeWeaver, BeanClassLoaderAware, DisposableBean {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private LoadTimeWeaver loadTimeWeaver;

  public DefaultContextLoadTimeWeaver() {
  }

  public DefaultContextLoadTimeWeaver(ClassLoader beanClassLoader) {
    setBeanClassLoader(beanClassLoader);
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    LoadTimeWeaver serverSpecificLoadTimeWeaver = createServerSpecificLoadTimeWeaver(classLoader);
    if (serverSpecificLoadTimeWeaver != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Determined server-specific load-time weaver: {}",
                serverSpecificLoadTimeWeaver.getClass().getName());
      }
      this.loadTimeWeaver = serverSpecificLoadTimeWeaver;
    }
    else if (InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
      logger.debug("Found Infra JVM agent for instrumentation");
      this.loadTimeWeaver = new InstrumentationLoadTimeWeaver(classLoader);
    }
    else {
      try {
        this.loadTimeWeaver = new ReflectiveLoadTimeWeaver(classLoader);
        if (logger.isDebugEnabled()) {
          logger.debug("Using reflective load-time weaver for class loader: " +
                  this.loadTimeWeaver.getInstrumentableClassLoader().getClass().getName());
        }
      }
      catch (IllegalStateException ex) {
        throw new IllegalStateException(ex.getMessage() + " Specify a custom LoadTimeWeaver or start your " +
                "Java virtual machine with Infra agent: -javaagent:today-instrument-{version}.jar");
      }
    }
  }

  /*
   * This method never fails, allowing to try other possible ways to use an
   * server-agnostic weaver. This non-failure logic is required since
   * determining a load-time weaver based on the ClassLoader name alone may
   * legitimately fail due to other mismatches.
   */
  @Nullable
  protected LoadTimeWeaver createServerSpecificLoadTimeWeaver(ClassLoader classLoader) {
    String name = classLoader.getClass().getName();
    try {
      if (name.startsWith("org.apache.catalina")) {
        return new TomcatLoadTimeWeaver(classLoader);
      }
    }
    catch (Exception ex) {
      if (logger.isInfoEnabled()) {
        logger.info("Could not obtain server-specific LoadTimeWeaver: {}", ex.getMessage());
      }
    }
    return null;
  }

  @Override
  public void destroy() {
    if (this.loadTimeWeaver instanceof InstrumentationLoadTimeWeaver) {
      if (logger.isDebugEnabled()) {
        logger.debug("Removing all registered transformers for class loader: {}",
                this.loadTimeWeaver.getInstrumentableClassLoader().getClass().getName());
      }
      ((InstrumentationLoadTimeWeaver) this.loadTimeWeaver).removeTransformers();
    }
  }

  @Override
  public void addTransformer(ClassFileTransformer transformer) {
    Assert.state(this.loadTimeWeaver != null, "Not initialized");
    this.loadTimeWeaver.addTransformer(transformer);
  }

  @Override
  public ClassLoader getInstrumentableClassLoader() {
    Assert.state(this.loadTimeWeaver != null, "Not initialized");
    return this.loadTimeWeaver.getInstrumentableClassLoader();
  }

  @Override
  public ClassLoader getThrowawayClassLoader() {
    Assert.state(this.loadTimeWeaver != null, "Not initialized");
    return this.loadTimeWeaver.getThrowawayClassLoader();
  }

}
