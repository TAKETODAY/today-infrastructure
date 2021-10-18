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

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;

import cn.taketoday.beans.DisposableBean;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;

/**
 * Base class for dynamic {@link cn.taketoday.aop.TargetSource} implementations
 * that create new prototype bean instances to support a pooling or
 * new-instance-per-invocation strategy.
 *
 * <p>Such TargetSources must run in a {@link BeanFactory}, as it needs to
 * call the {@code getBean} method to create a new prototype instance.
 * Therefore, this base class extends {@link AbstractBeanFactoryTargetSource}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 20:40
 * @see BeanFactory#getBean
 * @see PrototypeTargetSource
 * @see ThreadLocalTargetSource
 * @since 3.0
 */
public abstract class AbstractPrototypeTargetSource extends AbstractBeanFactoryTargetSource {

  /**
   * Subclasses should call this method to create a new prototype instance.
   */
  protected Object newPrototypeInstance() {
    if (logger.isDebugEnabled()) {
      logger.debug("Creating new instance of bean '{}'", getTargetBeanName());
    }
    return getBeanFactory().getBean(getTargetBeanDefinition());
  }

  /**
   * Subclasses should call this method to destroy an obsolete prototype instance.
   *
   * @param target the bean instance to destroy
   */
  protected void destroyPrototypeInstance(Object target) {
    if (logger.isDebugEnabled()) {
      logger.debug("Destroying instance of bean '{}'", getTargetBeanName());
    }
    final BeanFactory factory = getBeanFactory();
    if (factory != null && factory.isFullLifecycle()) {
      if (factory instanceof ConfigurableBeanFactory) {
        ((ConfigurableBeanFactory) factory).destroyBean(target, getTargetBeanDefinition());
      }
      else if (target instanceof DisposableBean) {
        try {
          ((DisposableBean) target).destroy();
        }
        catch (Throwable ex) {
          logger.warn("Destroy method on bean with name '{}' threw an exception", getTargetBeanName(), ex);
        }
      }
    }
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    throw new NotSerializableException(
            "A prototype-based TargetSource itself is not deserializable - " +
                    "just a disconnected SingletonTargetSource or EmptyTargetSource is");
  }

  /**
   * Replaces this object with a SingletonTargetSource on serialization.
   * Protected as otherwise it won't be invoked for subclasses.
   * (The {@code writeReplace()} method must be visible to the class
   * being serialized.)
   * <p>With this implementation of this method, there is no need to mark
   * non-serializable fields in this class or subclasses as transient.
   */
  protected Object writeReplace() throws ObjectStreamException {
    if (logger.isDebugEnabled()) {
      logger.debug("Disconnecting TargetSource [{}]", this);
    }
    try {
      // Create disconnected SingletonTargetSource/EmptyTargetSource.
      Object target = getTarget();
      return target != null
             ? new SingletonTargetSource(target)
             : EmptyTargetSource.forClass(getTargetClass());
    }
    catch (Exception ex) {
      String msg = "Cannot get target for disconnecting TargetSource [" + this + "]";
      logger.error(msg, ex);
      throw new NotSerializableException(msg + ": " + ex);
    }
  }

}
