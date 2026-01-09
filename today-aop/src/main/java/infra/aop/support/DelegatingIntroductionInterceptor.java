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

package infra.aop.support;

import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

import infra.aop.DynamicIntroductionAdvice;
import infra.aop.IntroductionInterceptor;
import infra.aop.ProxyMethodInvocation;
import infra.lang.Assert;

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
  @Nullable
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
    Assert.notNull(delegate, "Delegate is required");
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
  @Nullable
  public Object invoke(MethodInvocation mi) throws Throwable {
    if (isMethodOnIntroducedInterface(mi)) {
      // Using the following method rather than direct reflection, we
      // get correct handling of InvocationTargetException
      // if the introduced method throws an exception.
      Object retVal = AopUtils.invokeJoinpointUsingReflection(this.delegate, mi.getMethod(), mi.getArguments());

      // Massage return value if possible: if the delegate returned itself,
      // we really want to return the proxy.
      if (retVal == this.delegate && mi instanceof ProxyMethodInvocation) {
        Object proxy = ((ProxyMethodInvocation) mi).getProxy();
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
  @Nullable
  protected Object doProceed(MethodInvocation mi) throws Throwable {
    // If we get here, just pass the invocation on.
    return mi.proceed();
  }

}

