package infra.aop.support;

import java.lang.reflect.Method;

import infra.aop.MethodMatcher;
import infra.aop.Pointcut;
import infra.aop.framework.DefaultMethodInvocation;
import infra.lang.Assert;

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
    Assert.notNull(pointcut, "Pointcut is required");
    if (pointcut == Pointcut.TRUE) {
      return true;
    }
    if (pointcut.getClassFilter().matches(targetClass)) {
      // Only check if it gets past first hurdle.
      MethodMatcher mm = pointcut.getMethodMatcher();
      if (mm.matches(method, targetClass)) {
        // We may need additional runtime (argument) check.
        return !mm.isRuntime() || mm.matches(
                new DefaultMethodInvocation(null, null, method, targetClass, args, EMPTY_INTERCEPTOR));
      }
    }
    return false;
  }

}
