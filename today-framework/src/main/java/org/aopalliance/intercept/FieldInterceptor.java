package org.aopalliance.intercept;

/**
 * Intercepts field access on a target object.
 *
 * <p>
 * The user should implement the {@link #set(FieldAccess)} and
 * {@link #get(FieldAccess)} methods to modify the original behavior. E.g. the
 * following class implements a tracing interceptor (traces the accesses to the
 * intercepted field(s)):
 *
 * <pre class=code>
 * class TracingInterceptor implements FieldInterceptor {
 *
 *     Object set(FieldAccess fa) throws Throwable {
 *         System.out.println("field " + fa.getField() + " is set with value " + fa.getValueToSet());
 *         Object ret = fa.proceed();
 *         System.out.println("field " + fa.getField() + " was set to value " + ret);
 *         return ret;
 *     }
 *
 *     Object get(FieldAccess fa) throws Throwable {
 *         System.out.println("field " + fa.getField() + " is about to be read");
 *         Object ret = fa.proceed();
 *         System.out.println("field " + fa.getField() + " was read; value is " + ret);
 *         return ret;
 *     }
 * }
 * </pre>
 */

public interface FieldInterceptor extends Interceptor {

  /**
   * Do the stuff you want to do before and after the field is getted.
   *
   * <p>
   * Polite implementations would certainly like to call
   * {@link Joinpoint#proceed()}.
   *
   * @param fieldRead
   *         the joinpoint that corresponds to the field read
   *
   * @return the result of the field read {@link Joinpoint#proceed()}, might be
   * intercepted by the interceptor.
   *
   * @throws Throwable
   *         if the interceptors or the target-object throws an exception.
   */
  Object get(FieldAccess fieldRead) throws Throwable;

  /**
   * Do the stuff you want to do before and after the field is setted.
   *
   * <p>
   * Polite implementations would certainly like to implement
   * {@link Joinpoint#proceed()}.
   *
   * @param fieldWrite
   *         the joinpoint that corresponds to the field write
   *
   * @return the result of the field set {@link Joinpoint#proceed()}, might be
   * intercepted by the interceptor.
   *
   * @throws Throwable
   *         if the interceptors or the target-object throws an exception.
   */
  Object set(FieldAccess fieldWrite) throws Throwable;

}
