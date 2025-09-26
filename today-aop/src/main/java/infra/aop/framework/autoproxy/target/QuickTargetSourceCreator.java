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
