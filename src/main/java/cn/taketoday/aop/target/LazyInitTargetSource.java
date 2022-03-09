/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.aop.target;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.lang.Nullable;

/**
 * {@link TargetSource} that lazily accesses a
 * singleton bean from a {@link BeanFactory}.
 *
 * <p>Useful when a proxy reference is needed on initialization but
 * the actual target object should not be initialized until first use.
 * When the target bean is defined in an
 * {@link cn.taketoday.context.ApplicationContext} (or a
 * {@code BeanFactory} that is eagerly pre-instantiating singleton beans)
 * it must be marked as "lazy-init" too, else it will be instantiated by said
 * {@code ApplicationContext} (or {@code BeanFactory}) on startup.
 * <p>For example:
 *
 * <pre class="code">
 * &lt;bean id="serviceTarget" class="example.MyService" lazy-init="true"&gt;
 *   ...
 * &lt;/bean&gt;
 *
 * &lt;bean id="service" class="cn.taketoday.aop.proxy.ProxyFactoryBean"&gt;
 *   &lt;property name="targetSource"&gt;
 *     &lt;bean class="cn.taketoday.aop.target.LazyInitTargetSource"&gt;
 *       &lt;property name="targetBeanName"&gt;&lt;idref local="serviceTarget"/&gt;&lt;/property&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * The "serviceTarget" bean will not get initialized until a method on the
 * "service" proxy gets invoked.
 *
 * <p>Subclasses can extend this class and override the {@link #postProcessTargetObject(Object)} to
 * perform some additional processing with the target object when it is first loaded.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see BeanFactory#getBean
 * @see #postProcessTargetObject
 * @since 4.0
 */
@SuppressWarnings("serial")
public class LazyInitTargetSource extends AbstractBeanFactoryTargetSource {

  @Nullable
  private Object target;

  @Override
  @Nullable
  public synchronized Object getTarget() throws BeansException {
    if (this.target == null) {
      this.target = getBeanFactory().getBean(getTargetBeanName());
      postProcessTargetObject(this.target);
    }
    return this.target;
  }

  /**
   * Subclasses may override this method to perform additional processing on
   * the target object when it is first loaded.
   *
   * @param targetObject the target object that has just been instantiated (and configured)
   */
  protected void postProcessTargetObject(Object targetObject) { }

}
