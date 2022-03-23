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

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.aop.IntroductionAdvisor;
import cn.taketoday.aop.support.DefaultIntroductionAdvisor;
import cn.taketoday.aop.support.DelegatingIntroductionInterceptor;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.core.NamedThreadLocal;

/**
 * Alternative to an object pool. This {@link cn.taketoday.aop.TargetSource}
 * uses a threading model in which every thread has its own copy of the target.
 * There's no contention for targets. Target object creation is kept to a minimum
 * on the running server.
 *
 * <p>Application code is written as to a normal pool; callers can't assume they
 * will be dealing with the same instance in invocations in different threads.
 * However, state can be relied on during the operations of a single thread:
 * for example, if one caller makes repeated calls on the AOP proxy.
 *
 * <p>Cleanup of thread-bound objects is performed on BeanFactory destruction,
 * calling their {@code DisposableBean.destroy()} method if available.
 * Be aware that many thread-bound objects can be around until the application
 * actually shuts down.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author TODAY 2021/2/1 20:41
 * @see ThreadLocalTargetSourceStats
 * @see DisposableBean#destroy()
 * @since 3.0
 */
public class ThreadLocalTargetSource
        extends AbstractPrototypeTargetSource implements ThreadLocalTargetSourceStats, DisposableBean {

  /**
   * ThreadLocal holding the target associated with the current
   * thread. Unlike most ThreadLocals, which are static, this variable
   * is meant to be per thread per instance of the ThreadLocalTargetSource class.
   */
  private final ThreadLocal<Object> targetInThread =
          new NamedThreadLocal<>("Thread-local instance of bean '" + getTargetBeanName() + "'");

  /**
   * Set of managed targets, enabling us to keep track of the targets we've created.
   */
  private final Set<Object> targetSet = new HashSet<>();

  private int invocationCount;

  private int hitCount;

  /**
   * Implementation of abstract getTarget() method.
   * We look for a target held in a ThreadLocal. If we don't find one,
   * we create one and bind it to the thread. No synchronization is required.
   */
  @Override
  public Object getTarget() {
    ++this.invocationCount;
    Object target = this.targetInThread.get();
    if (target == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("No target for prototype '{}' bound to thread: creating one and binding it to thread '{}'",
                getTargetBeanName(), Thread.currentThread().getName());
      }
      // Associate target with ThreadLocal.
      target = newPrototypeInstance();
      this.targetInThread.set(target);
      synchronized(this.targetSet) {
        this.targetSet.add(target);
      }
    }
    else {
      ++this.hitCount;
    }
    return target;
  }

  /**
   * Dispose of targets if necessary; clear ThreadLocal.
   *
   * @see #destroyPrototypeInstance
   */
  @Override
  public void destroy() {
    logger.debug("Destroying ThreadLocalTargetSource bindings");
    synchronized(this.targetSet) {
      for (Object target : this.targetSet) {
        destroyPrototypeInstance(target);
      }
      this.targetSet.clear();
    }
    // Clear ThreadLocal, just in case.
    this.targetInThread.remove();
  }

  @Override
  public int getInvocationCount() {
    return this.invocationCount;
  }

  @Override
  public int getHitCount() {
    return this.hitCount;
  }

  @Override
  public int getObjectCount() {
    synchronized(this.targetSet) {
      return this.targetSet.size();
    }
  }

  /**
   * Return an introduction advisor mixin that allows the AOP proxy to be
   * cast to ThreadLocalInvokerStats.
   */
  public IntroductionAdvisor getStatsMixin() {
    DelegatingIntroductionInterceptor dii = new DelegatingIntroductionInterceptor(this);
    return new DefaultIntroductionAdvisor(dii, ThreadLocalTargetSourceStats.class);
  }

}
