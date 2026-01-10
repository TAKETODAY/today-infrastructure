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

package infra.beans.factory;

import infra.beans.BeansException;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * @author Today <br>
 *
 * 2018-09-09 20:14
 */
//@Singleton
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProxyBeanPostProcessor implements InitializationBeanPostProcessor {
  private static final Logger log = LoggerFactory.getLogger(ProxyBeanPostProcessor.class);

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) {
    log.debug("ProxyBeanPostProcessor Before named :[{}]", beanName);
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    log.debug("ProxyBeanPostProcessor After :[{}]", beanName);
    return bean;
  }

}
