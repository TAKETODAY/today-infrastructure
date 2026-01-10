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

package infra.aop.framework.autoproxy.target;

import org.jspecify.annotations.Nullable;

import infra.aop.target.AbstractBeanFactoryTargetSource;
import infra.aop.target.CommonsPool2TargetSource;
import infra.aop.target.PrototypeTargetSource;
import infra.aop.target.ThreadLocalTargetSource;

/**
 * Convenient TargetSourceCreator using bean name prefixes to create one of three
 * well-known TargetSource types:
 * <ul>
 * <li>: CommonsPool2TargetSource</li>
 * <li>% ThreadLocalTargetSource</li>
 * <li>! PrototypeTargetSource</li>
 * </ul>
 *
 * @author Rod Johnson
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CommonsPool2TargetSource
 * @see ThreadLocalTargetSource
 * @see PrototypeTargetSource
 * @since 4.0 2021/12/13 22:32
 */
public class QuickTargetSourceCreator extends AbstractBeanFactoryTargetSourceCreator {

  /**
   * The CommonsPool2TargetSource prefix.
   */
  public static final String PREFIX_COMMONS_POOL = ":";

  /**
   * The ThreadLocalTargetSource prefix.
   */
  public static final String PREFIX_THREAD_LOCAL = "%";

  /**
   * The PrototypeTargetSource prefix.
   */
  public static final String PREFIX_PROTOTYPE = "!";

  @Override
  @Nullable
  protected final AbstractBeanFactoryTargetSource createBeanFactoryTargetSource(
          Class<?> beanClass, String beanName) {

    if (beanName.startsWith(PREFIX_COMMONS_POOL)) {
      CommonsPool2TargetSource cpts = new CommonsPool2TargetSource();
      cpts.setMaxSize(25);
      return cpts;
    }
    else if (beanName.startsWith(PREFIX_THREAD_LOCAL)) {
      return new ThreadLocalTargetSource();
    }
    else if (beanName.startsWith(PREFIX_PROTOTYPE)) {
      return new PrototypeTargetSource();
    }
    else {
      // No match. Don't create a custom target source.
      return null;
    }
  }

}
