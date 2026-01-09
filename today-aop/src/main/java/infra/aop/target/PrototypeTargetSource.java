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

package infra.aop.target;

import java.io.Serial;

import infra.beans.factory.BeanFactory;

/**
 * {@link infra.aop.TargetSource} implementation that
 * creates a new instance of the target bean for each request,
 * destroying each instance on release (after each request).
 *
 * <p>Obtains bean instances from its containing
 * {@link BeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 20:46
 * @see #setBeanFactory
 * @see #setTargetBeanName
 * @since 3.0
 */
public class PrototypeTargetSource extends AbstractPrototypeTargetSource {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Obtain a new prototype instance for every call.
   *
   * @see #newPrototypeInstance()
   */
  @Override
  public Object getTarget() {
    return newPrototypeInstance();
  }

  /**
   * Destroy the given independent instance.
   *
   * @see #destroyPrototypeInstance
   */
  @Override
  public void releaseTarget(Object target) {
    destroyPrototypeInstance(target);
  }

  @Override
  public String toString() {
    return "PrototypeTargetSource for target bean with name '" + targetBeanName + "'";
  }

}
