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

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serial;

import infra.beans.BeansException;
import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.config.ConfigurableBeanFactory;

/**
 * Base class for dynamic {@link infra.aop.TargetSource} implementations
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

  @Serial
  private static final long serialVersionUID = 1L;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    super.setBeanFactory(beanFactory);

    // Check whether the target bean is defined as prototype.
    if (!beanFactory.isPrototype(getTargetBeanName())) {
      throw new BeanDefinitionStoreException(
              "Cannot use prototype-based TargetSource against non-prototype bean with name '" +
                      targetBeanName + "': instances would not be independent");
    }
  }

  /**
   * Subclasses should call this method to create a new prototype instance.
   */
  @SuppressWarnings("NullAway")
  protected Object newPrototypeInstance() {
    if (logger.isDebugEnabled()) {
      logger.debug("Creating new instance of bean '{}'", targetBeanName);
    }
    return getBeanFactory().getBean(getTargetBeanName());
  }

  /**
   * Subclasses should call this method to destroy an obsolete prototype instance.
   *
   * @param target the bean instance to destroy
   */
  protected void destroyPrototypeInstance(Object target) {
    if (logger.isDebugEnabled()) {
      logger.debug("Destroying instance of bean '{}'", targetBeanName);
    }
    if (getBeanFactory() instanceof ConfigurableBeanFactory factory) {
      factory.destroyBean(getTargetBeanName(), target);
    }
    else if (target instanceof DisposableBean) {
      try {
        ((DisposableBean) target).destroy();
      }
      catch (Throwable ex) {
        logger.warn("Destroy method on bean with name '{}' threw an exception", targetBeanName, ex);
      }
    }
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  @Serial
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
  @Serial
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
