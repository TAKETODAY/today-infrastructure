/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.weaving;

import org.jspecify.annotations.Nullable;

import java.lang.instrument.ClassFileTransformer;

import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.DisposableBean;
import infra.instrument.InstrumentationSavingAgent;
import infra.instrument.classloading.InstrumentationLoadTimeWeaver;
import infra.instrument.classloading.LoadTimeWeaver;
import infra.instrument.classloading.ReflectiveLoadTimeWeaver;
import infra.instrument.classloading.TomcatLoadTimeWeaver;
import infra.lang.Assert;
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
 * {@link InstrumentationSavingAgent Infra VM agent} and any {@link ClassLoader}
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
