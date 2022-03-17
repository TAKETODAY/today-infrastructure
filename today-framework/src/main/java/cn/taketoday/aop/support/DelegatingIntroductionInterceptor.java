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

package cn.taketoday.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import java.io.Serial;

import cn.taketoday.aop.DynamicIntroductionAdvice;
import cn.taketoday.aop.IntroductionInterceptor;
import cn.taketoday.aop.framework.AbstractMethodInvocation;
import cn.taketoday.lang.Assert;

/**
 * Convenient implementation of the {@link IntroductionInterceptor} interface.
 *
 * <p>Subclasses merely need to extend this class and implement the interfaces
 * to be introduced themselves. In this case the delegate is the subclass
 * instance itself. Alternatively a separate delegate may implement the
 * interface, and be set via the delegate bean property.
 *
 * <p>Delegates or subclasses may implement any number of interfaces.
 * All interfaces except IntroductionInterceptor are picked up from
 * the subclass or delegate by default.
 *
 * <p>The {@code suppressInterface} method can be used to suppress interfaces
 * implemented by the delegate but which should not be introduced to the owning
 * AOP proxy.
 *
 * <p>An instance of this class is serializable if the delegate is.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/3/8 22:20
 * @see #suppressInterface
 * @see DelegatePerTargetObjectIntroductionInterceptor
 * @since 3.0
 */
public class DelegatingIntroductionInterceptor
        extends IntroductionInfoSupport implements IntroductionInterceptor {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Object that actually implements the interfaces.
   * May be "this" if a subclass implements the introduced interfaces.
   */
  private Object delegate;

  /**
   * Construct a new DelegatingIntroductionInterceptor, providing
   * a delegate that implements the interfaces to be introduced.
   *
   * @param delegate the delegate that implements the introduced interfaces
   */
  public DelegatingIntroductionInterceptor(Object delegate) {
    init(delegate);
  }

  /**
   * Construct a new DelegatingIntroductionInterceptor.
   * The delegate will be the subclass, which must implement
   * additional interfaces.
   */
  protected DelegatingIntroductionInterceptor() {
    init(this);
  }

  /**
   * Both constructors use this init method, as it is impossible to pass
   * a "this" reference from one constructor to another.
   *
   * @param delegate the delegate object
   */
  private void init(Object delegate) {
    Assert.notNull(delegate, "Delegate must not be null");
    this.delegate = delegate;
    implementInterfacesOnObject(delegate);

    // We don't want to expose the control interface
    suppressInterface(IntroductionInterceptor.class);
    suppressInterface(DynamicIntroductionAdvice.class);
  }

  /**
   * Subclasses may need to override this if they want to perform custom
   * behaviour in around advice. However, subclasses should invoke this
   * method, which handles introduced interfaces and forwarding to the target.
   */
  @Override
  public Object invoke(MethodInvocation mi) throws Throwable {
    if (isMethodOnIntroducedInterface(mi)) {
      // Using the following method rather than direct reflection, we
      // get correct handling of InvocationTargetException
      // if the introduced method throws an exception.

      // Massage return value if possible: if the delegate returned itself,
      // we really want to return the proxy.
      Object retVal = AopUtils.invokeJoinpointUsingReflection(this.delegate, mi.getMethod(), mi.getArguments());
      if (retVal == this.delegate && mi instanceof AbstractMethodInvocation invocation) {
        Object proxy = invocation.getProxy();
        if (mi.getMethod().getReturnType().isInstance(proxy)) {
          retVal = proxy;
        }
      }

      return retVal;
    }

    return doProceed(mi);
  }

  /**
   * Proceed with the supplied {@link org.aopalliance.intercept.MethodInterceptor}.
   * Subclasses can override this method to intercept method invocations on the
   * target object which is useful when an introduction needs to monitor the object
   * that it is introduced into. This method is <strong>never</strong> called for
   * {@link MethodInvocation MethodInvocations} on the introduced interfaces.
   */
  protected Object doProceed(MethodInvocation mi) throws Throwable {
    // If we get here, just pass the invocation on.
    return mi.proceed();
  }

}

