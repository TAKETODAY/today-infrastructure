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

import infra.beans.factory.Aware;
import infra.beans.factory.InitializingBean;
import infra.context.ApplicationContextAware;
import infra.instrument.classloading.LoadTimeWeaver;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the application context's default {@link LoadTimeWeaver}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see infra.context.ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 * @since 4.0
 */
public interface LoadTimeWeaverAware extends Aware {

  /**
   * Set the {@link LoadTimeWeaver} of this object's containing
   * {@link infra.context.ApplicationContext ApplicationContext}.
   * <p>Invoked after the population of normal bean properties but before an
   * initialization callback like
   * {@link InitializingBean InitializingBean's}
   * {@link InitializingBean#afterPropertiesSet() afterPropertiesSet()}
   * or a custom init-method. Invoked after
   * {@link ApplicationContextAware ApplicationContextAware's}
   * {@link ApplicationContextAware#setApplicationContext setApplicationContext(..)}.
   * <p><b>NOTE:</b> This method will only be called if there actually is a
   * {@code LoadTimeWeaver} available in the application context. If
   * there is none, the method will simply not get invoked, assuming that the
   * implementing object is able to activate its weaving dependency accordingly.
   *
   * @param loadTimeWeaver the {@code LoadTimeWeaver} instance (never {@code null})
   * @see InitializingBean#afterPropertiesSet
   * @see ApplicationContextAware#setApplicationContext
   */
  void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver);

}
