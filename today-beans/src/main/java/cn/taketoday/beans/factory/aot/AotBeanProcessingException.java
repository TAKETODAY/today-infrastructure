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

package cn.taketoday.beans.factory.aot;

import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.lang.Nullable;

/**
 * Thrown when AOT fails to process a bean.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
@SuppressWarnings("serial")
public class AotBeanProcessingException extends AotProcessingException {

  private final RootBeanDefinition beanDefinition;

  /**
   * Create an instance with the {@link RegisteredBean} that fails to be
   * processed, a detail message, and an optional root cause.
   *
   * @param registeredBean the registered bean that fails to be processed
   * @param msg the detail message
   * @param cause the root cause, if any
   */
  public AotBeanProcessingException(RegisteredBean registeredBean, String msg, @Nullable Throwable cause) {
    super(createErrorMessage(registeredBean, msg), cause);
    this.beanDefinition = registeredBean.getMergedBeanDefinition();
  }

  /**
   * Shortcut to create an instance with the {@link RegisteredBean} that fails
   * to be processed with only a detail message.
   *
   * @param registeredBean the registered bean that fails to be processed
   * @param msg the detail message
   */
  public AotBeanProcessingException(RegisteredBean registeredBean, String msg) {
    this(registeredBean, msg, null);
  }

  private static String createErrorMessage(RegisteredBean registeredBean, String msg) {
    StringBuilder sb = new StringBuilder("Error processing bean with name '");
    sb.append(registeredBean.getBeanName()).append("'");
    String resourceDescription = registeredBean.getMergedBeanDefinition().getResourceDescription();
    if (resourceDescription != null) {
      sb.append(" defined in ").append(resourceDescription);
    }
    sb.append(": ").append(msg);
    return sb.toString();
  }

  /**
   * Return the bean definition of the bean that failed to be processed.
   */
  public RootBeanDefinition getBeanDefinition() {
    return this.beanDefinition;
  }

}
