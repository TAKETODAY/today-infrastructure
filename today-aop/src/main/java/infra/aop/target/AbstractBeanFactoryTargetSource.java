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

package infra.aop.target;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import infra.aop.TargetSource;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Base class for {@link infra.aop.TargetSource} implementations that are
 * based on a {@link BeanFactory}, delegating to
 * ioc-managed bean instances.
 *
 * <p>
 * Subclasses can create prototype instances or lazily access a singleton
 * target, for example. See {@link AbstractPrototypeTargetSource}'s subclasses
 * for concrete strategies.
 *
 * <p>
 * BeanFactory-based TargetSources are serializable. This involves disconnecting
 * the current target and turning into a {@link SingletonTargetSource}.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author TODAY 2021/2/1 20:38
 * @see BeanFactory#getBean
 * @see PrototypeTargetSource
 * @see ThreadLocalTargetSource
 * @since 3.0
 */
public abstract class AbstractBeanFactoryTargetSource implements TargetSource, BeanFactoryAware, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  protected final transient Logger logger = LoggerFactory.getLogger(getClass());

  /** Name of the target bean we will create on each invocation. */
  @Nullable
  protected String targetBeanName;

  /** Class of the target. */
  @Nullable
  private volatile Class<?> targetClass;

  /**
   * BeanFactory that owns this TargetSource. We need to hold onto this reference
   * so that we can create new prototype instances as necessary.
   */
  @Nullable
  private BeanFactory beanFactory;

  /**
   * Set the name of the target bean in the factory.
   * <p>
   * The target bean should not be a singleton, else the same instance will always
   * be obtained from the factory, resulting in the same behavior as provided by
   * {@link SingletonTargetSource}.
   *
   * @param targetBeanName name of the target bean in the BeanFactory that owns this
   * interceptor
   * @see SingletonTargetSource
   */
  public void setTargetBeanName(String targetBeanName) {
    this.targetBeanName = targetBeanName;
  }

  /**
   * Return the name of the target bean in the factory.
   */
  public String getTargetBeanName() {
    Assert.state(targetBeanName != null, "Target bean name not set");
    return targetBeanName;
  }

  /**
   * Specify the target class explicitly, to avoid any kind of access to the
   * target bean (for example, to avoid initialization of a FactoryBean instance).
   * <p>
   * Default is to detect the type automatically, through a {@code getType} call
   * on the BeanFactory (or even a full {@code getBean} call as fallback).
   */
  public void setTargetClass(Class<?> targetClass) {
    this.targetClass = targetClass;
  }

  /**
   * Set the owning BeanFactory. We need to save a reference so that we can use
   * the {@code getBean} method on every invocation.
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (this.targetBeanName == null) {
      throw new IllegalStateException("Property 'targetBeanName' is required");
    }
    this.beanFactory = beanFactory;
  }

  /**
   * Return the owning BeanFactory.
   */
  public BeanFactory getBeanFactory() {
    Assert.state(this.beanFactory != null, "BeanFactory not set");
    return this.beanFactory;
  }

  @Override
  public Class<?> getTargetClass() {
    Class<?> targetClass = this.targetClass;
    if (targetClass != null) {
      return targetClass;
    }
    synchronized(this) {
      // Full check within synchronization, entering the BeanFactory interaction algorithm only once...
      targetClass = this.targetClass;
      if (targetClass == null && beanFactory != null && targetBeanName != null) {
        // Determine type of the target bean.
        targetClass = beanFactory.getType(targetBeanName);
        if (targetClass == null) {
          if (logger.isTraceEnabled()) {
            logger.trace("Getting bean with name '{}' for type determination", targetBeanName);
          }
          Object beanInstance = this.beanFactory.getBean(targetBeanName);
          if (beanInstance == null) {
            throw new NoSuchBeanDefinitionException(targetBeanName);
          }
          targetClass = beanInstance.getClass();
        }
        this.targetClass = targetClass;
      }
      return targetClass;
    }
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  @Override
  public void releaseTarget(Object target) throws Exception {
    // Nothing to do here.
  }

  /**
   * Copy configuration from the other AbstractBeanFactoryBasedTargetSource
   * object. Subclasses should override this if they wish to expose it.
   *
   * @param other object to copy configuration from
   */
  protected void copyFrom(AbstractBeanFactoryTargetSource other) {
    this.targetClass = other.targetClass;
    this.beanFactory = other.beanFactory;
    this.targetBeanName = other.targetBeanName;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    AbstractBeanFactoryTargetSource otherTarget = (AbstractBeanFactoryTargetSource) other;
    return (Objects.equals(this.beanFactory, otherTarget.beanFactory) &&
            Objects.equals(this.targetBeanName, otherTarget.targetBeanName));
  }

  @Override
  public int hashCode() {
    int hashCode = getClass().hashCode();
    hashCode = 13 * hashCode + Objects.hashCode(this.beanFactory);
    hashCode = 13 * hashCode + Objects.hashCode(this.targetBeanName);
    return hashCode;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName());
    sb.append(" for target bean '").append(this.targetBeanName).append("'");
    if (this.targetClass != null) {
      sb.append(" of type [").append(this.targetClass.getName()).append("]");
    }
    return sb.toString();
  }

}
