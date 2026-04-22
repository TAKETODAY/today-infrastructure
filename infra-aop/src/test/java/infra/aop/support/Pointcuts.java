package infra.aop.support;

import java.lang.reflect.Method;

import infra.aop.Pointcut;
import infra.aop.framework.DefaultMethodInvocation;

import static infra.aop.InterceptorChainFactory.EMPTY_INTERCEPTOR;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/4/22 11:52
 */
abstract class Pointcuts {

  /**
   * Perform the least expensive check for a pointcut match.
   *
   * @param pointcut the pointcut to match
   * @param method the candidate method
   * @param targetClass the target class
   * @param args arguments to the method
   * @return whether there's a runtime match
   */
  static boolean matches(Pointcut pointcut, Method method, Class<?> targetClass, Object... args) {
    return Pointcut.matches(pointcut, new DefaultMethodInvocation(null, null, method, targetClass, args, EMPTY_INTERCEPTOR));
  }

}
