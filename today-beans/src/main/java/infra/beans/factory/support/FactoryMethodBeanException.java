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

package infra.beans.factory.support;

import infra.beans.FatalBeanException;
import infra.beans.factory.InjectionPoint;
import infra.beans.factory.config.BeanDefinition;

/**
 * factory method error
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/31 22:35
 */
public class FactoryMethodBeanException extends FatalBeanException {

  private final String beanName;

  private final BeanDefinition merged;

  private final InjectionPoint injectionPoint;

  public FactoryMethodBeanException(BeanDefinition merged,
          InjectionPoint injectionPoint, String beanName, String message) {
    super(message);
    this.merged = merged;
    this.injectionPoint = injectionPoint;
    this.beanName = beanName;
  }

  public BeanDefinition getBeanDefinition() {
    return merged;
  }

  public InjectionPoint getInjectionPoint() {
    return injectionPoint;
  }

  public String getBeanName() {
    return beanName;
  }

}
