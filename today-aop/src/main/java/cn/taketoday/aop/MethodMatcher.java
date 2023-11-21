/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.aop;

import org.aopalliance.intercept.MethodInvocation;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;

import cn.taketoday.lang.Assert;

/**
 * Part of a {@link Pointcut}: Checks whether the target method is eligible for advice.
 *
 * <p>A MethodMatcher may be evaluated <b>statically</b> or at <b>runtime</b> (dynamically).
 * Static matching involves method and (possibly) method attributes. Dynamic matching
 * also makes arguments for a particular call available, and any effects of running
 * previous advice applying to the join-point.
 *
 * <p>If an implementation returns {@code false} from its {@link #isRuntime()}
 * method, evaluation can be performed statically, and the result will be the same
 * for all invocations of this method, whatever their arguments. This means that
 * if the {@link #isRuntime()} method returns {@code false}, the 1-arg
 * {@link #matches(MethodInvocation)} method will never be invoked.
 *
 * <p>If an implementation returns {@code true} from its 2-arg
 * {@link #matches(java.lang.reflect.Method, Class)} method and its {@link #isRuntime()} method
 * returns {@code true}, the 1-arg {@link #matches(MethodInvocation)}
 * method will be invoked <i>immediately before each potential execution of the related advice</i>,
 * to decide whether the advice should run. All previous advice, such as earlier interceptors
 * in an interceptor chain, will have run, so any state changes they have produced in
 * parameters or ThreadLocal state will be available at the time of evaluation.
 *
 * <p><strong>WARNING</strong>: Concrete implementations of this interface must
 * provide proper implementations of {@link Object#equals(Object)},
 * {@link Object#hashCode()}, and {@link Object#toString()} in order to allow the
 * matcher to be used in caching scenarios &mdash; for example, in proxies generated
 * by CGLIB. As of 4.0, the {@code toString()} implementation
 * must generate a unique string representation that aligns with the logic used
 * to implement {@code equals()}. See concrete implementations of this interface
 * within the framework for examples.
 *
 * @author Rod Johnson
 * @author TODAY 2019-10-20 22:43
 * @see Pointcut
 * @see ClassFilter
 * @since 3.0
 */
public interface MethodMatcher {

  /**
   * Checking whether the given method matches.
   *
   * @param method the candidate method
   * @param targetClass the target class
   * @return whether or not this method matches on application startup.
   */
  boolean matches(Method method, Class<?> targetClass);

  /**
   * Is this MethodMatcher dynamic, that is, must a final call be made on the
   * {@link #matches(MethodInvocation)} method at runtime
   * even if the 2-arg matches method returns {@code true}?
   * <p>
   * Can be invoked when an AOP proxy is created, and need not be invoked again
   * before each method invocation,
   *
   * @return whether or not a runtime match via the 1-arg  {@link #matches(MethodInvocation)}
   * method is required if static matching passed
   */
  boolean isRuntime();

  /**
   * Check whether there a runtime (dynamic) match for this method, which must
   * have matched statically.
   * <p>
   * This method is invoked only if the 2-arg matches method returns {@code true}
   * for the given method and target class, and if the {@link #isRuntime()} method
   * returns {@code true}. Invoked immediately before potential running of the
   * advice, after any advice earlier in the advice chain has run.
   *
   * @param invocation runtime invocation contains the candidate method
   * and target class, arguments to the method
   * @return whether there's a runtime match
   * @see MethodMatcher#matches(Method, Class)
   */
  boolean matches(MethodInvocation invocation);

  /**
   * Canonical instance that matches all methods.
   */
  MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;

  //---------------------------------------------------------------------
  // Static factory methods
  //---------------------------------------------------------------------

  /**
   * Match all methods that <i>either</i> (or both) of the given MethodMatchers matches.
   *
   * @param mm1 the first MethodMatcher
   * @param mm2 the second MethodMatcher
   * @return a distinct MethodMatcher that matches all methods that either
   * of the given MethodMatchers matches
   */
  static MethodMatcher union(MethodMatcher mm1, MethodMatcher mm2) {
    return (mm1 instanceof IntroductionAwareMethodMatcher || mm2 instanceof IntroductionAwareMethodMatcher
            ? new UnionIntroductionAwareMethodMatcher(mm1, mm2)
            : new UnionMethodMatcher(mm1, mm2));
  }

  /**
   * Match all methods that <i>either</i> (or both) of the given MethodMatchers matches.
   *
   * @param mm1 the first MethodMatcher
   * @param cf1 the corresponding ClassFilter for the first MethodMatcher
   * @param mm2 the second MethodMatcher
   * @param cf2 the corresponding ClassFilter for the second MethodMatcher
   * @return a distinct MethodMatcher that matches all methods that either
   * of the given MethodMatchers matches
   */
  static MethodMatcher union(MethodMatcher mm1, ClassFilter cf1, MethodMatcher mm2, ClassFilter cf2) {
    return (mm1 instanceof IntroductionAwareMethodMatcher || mm2 instanceof IntroductionAwareMethodMatcher ?
            new ClassFilterAwareUnionIntroductionAwareMethodMatcher(mm1, cf1, mm2, cf2) :
            new ClassFilterAwareUnionMethodMatcher(mm1, cf1, mm2, cf2));
  }

  /**
   * Match all methods that <i>both</i> of the given MethodMatchers match.
   *
   * @param mm1 the first MethodMatcher
   * @param mm2 the second MethodMatcher
   * @return a distinct MethodMatcher that matches all methods that both
   * of the given MethodMatchers match
   */
  static MethodMatcher intersection(MethodMatcher mm1, MethodMatcher mm2) {
    return (mm1 instanceof IntroductionAwareMethodMatcher || mm2 instanceof IntroductionAwareMethodMatcher ?
            new IntersectionIntroductionAwareMethodMatcher(mm1, mm2) : new IntersectionMethodMatcher(mm1, mm2));
  }

  /**
   * Return a method matcher that represents the logical negation of the specified
   * matcher instance.
   *
   * @param methodMatcher the {@link MethodMatcher} to negate
   * @return a matcher that represents the logical negation of the specified matcher
   * @since 4.0
   */
  static MethodMatcher negate(MethodMatcher methodMatcher) {
    Assert.notNull(methodMatcher, "MethodMatcher is required");
    return new NegateMethodMatcher(methodMatcher);
  }

  /**
   * Apply the given MethodMatcher to the given Method, supporting an
   * {@link IntroductionAwareMethodMatcher}
   * (if applicable).
   *
   * @param mm the MethodMatcher to apply (may be an IntroductionAwareMethodMatcher)
   * @param method the candidate method
   * @param targetClass the target class
   * @param hasIntroductions {@code true} if the object on whose behalf we are
   * asking is the subject on one or more introductions; {@code false} otherwise
   * @return whether or not this method matches statically
   */
  static boolean matches(MethodMatcher mm, Method method, Class<?> targetClass, boolean hasIntroductions) {
    Assert.notNull(mm, "MethodMatcher is required");
    return (mm instanceof IntroductionAwareMethodMatcher ?
            ((IntroductionAwareMethodMatcher) mm).matches(method, targetClass, hasIntroductions)
                                                         : mm.matches(method, targetClass));
  }

  /**
   * MethodMatcher implementation for a union of two given MethodMatcher.
   */
  class UnionMethodMatcher implements MethodMatcher, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    protected final MethodMatcher mm1;
    protected final MethodMatcher mm2;

    private UnionMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
      Assert.notNull(mm1, "First MethodMatcher is required");
      Assert.notNull(mm2, "Second MethodMatcher is required");
      this.mm1 = mm1;
      this.mm2 = mm2;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
      return (matchesClass1(targetClass) && this.mm1.matches(method, targetClass)) ||
              (matchesClass2(targetClass) && this.mm2.matches(method, targetClass));
    }

    protected boolean matchesClass1(Class<?> targetClass) {
      return true;
    }

    protected boolean matchesClass2(Class<?> targetClass) {
      return true;
    }

    @Override
    public boolean isRuntime() {
      return this.mm1.isRuntime() || this.mm2.isRuntime();
    }

    @Override
    public boolean matches(MethodInvocation invocation) {
      return this.mm1.matches(invocation) || this.mm2.matches(invocation);
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof UnionMethodMatcher that)) {
        return false;
      }
      return (this.mm1.equals(that.mm1) && this.mm2.equals(that.mm2));
    }

    @Override
    public int hashCode() {
      return 37 * this.mm1.hashCode() + this.mm2.hashCode();
    }

    @Override
    public String toString() {
      return getClass().getName() + ": " + this.mm1 + ", " + this.mm2;
    }
  }

  /**
   * MethodMatcher implementation for a union of two given MethodMatchers
   * of which at least one is an IntroductionAwareMethodMatcher.
   */
  class UnionIntroductionAwareMethodMatcher
          extends UnionMethodMatcher implements IntroductionAwareMethodMatcher {
    @Serial
    private static final long serialVersionUID = 1L;

    private UnionIntroductionAwareMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
      super(mm1, mm2);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
      return (matchesClass1(targetClass) && MethodMatcher.matches(this.mm1, method, targetClass, hasIntroductions)) ||
              (matchesClass2(targetClass) && MethodMatcher.matches(this.mm2, method, targetClass, hasIntroductions));
    }
  }

  /**
   * MethodMatcher implementation for a union of two given MethodMatchers,
   * supporting an associated ClassFilter per MethodMatcher.
   */
  class ClassFilterAwareUnionMethodMatcher extends UnionMethodMatcher {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ClassFilter cf1;
    private final ClassFilter cf2;

    private ClassFilterAwareUnionMethodMatcher(MethodMatcher mm1, ClassFilter cf1, MethodMatcher mm2, ClassFilter cf2) {
      super(mm1, mm2);
      this.cf1 = cf1;
      this.cf2 = cf2;
    }

    @Override
    protected boolean matchesClass1(Class<?> targetClass) {
      return this.cf1.matches(targetClass);
    }

    @Override
    protected boolean matchesClass2(Class<?> targetClass) {
      return this.cf2.matches(targetClass);
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!super.equals(other)) {
        return false;
      }
      ClassFilter otherCf1 = ClassFilter.TRUE;
      ClassFilter otherCf2 = ClassFilter.TRUE;
      if (other instanceof ClassFilterAwareUnionMethodMatcher cfa) {
        otherCf1 = cfa.cf1;
        otherCf2 = cfa.cf2;
      }
      return (this.cf1.equals(otherCf1) && this.cf2.equals(otherCf2));
    }

    @Override
    public int hashCode() {
      // Allow for matching with regular UnionMethodMatcher by providing same hash...
      return super.hashCode();
    }

    @Override
    public String toString() {
      return getClass().getName() + ": " + this.cf1 + ", " + this.mm1 + ", " + this.cf2 + ", " + this.mm2;
    }
  }

  /**
   * MethodMatcher implementation for a union of two given MethodMatchers
   * of which at least one is an IntroductionAwareMethodMatcher,
   * supporting an associated ClassFilter per MethodMatcher.
   */
  class ClassFilterAwareUnionIntroductionAwareMethodMatcher
          extends ClassFilterAwareUnionMethodMatcher implements IntroductionAwareMethodMatcher {
    @Serial
    private static final long serialVersionUID = 1L;

    private ClassFilterAwareUnionIntroductionAwareMethodMatcher(
            MethodMatcher mm1, ClassFilter cf1, MethodMatcher mm2, ClassFilter cf2) {

      super(mm1, cf1, mm2, cf2);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
      return (matchesClass1(targetClass) && MethodMatcher.matches(this.mm1, method, targetClass, hasIntroductions)) ||
              (matchesClass2(targetClass) && MethodMatcher.matches(this.mm2, method, targetClass, hasIntroductions));
    }
  }

  /**
   * MethodMatcher implementation for an intersection of two given MethodMatcher.
   */
  class IntersectionMethodMatcher implements MethodMatcher, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    protected final MethodMatcher mm1;
    protected final MethodMatcher mm2;

    public IntersectionMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
      Assert.notNull(mm1, "First MethodMatcher is required");
      Assert.notNull(mm2, "Second MethodMatcher is required");
      this.mm1 = mm1;
      this.mm2 = mm2;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
      return (this.mm1.matches(method, targetClass) && this.mm2.matches(method, targetClass));
    }

    @Override
    public boolean isRuntime() {
      return (this.mm1.isRuntime() || this.mm2.isRuntime());
    }

    @Override
    public boolean matches(MethodInvocation invocation) {
      final Method method = invocation.getMethod();
      final Class<?> targetClass = invocation.getThis().getClass();
      boolean aMatches = (this.mm1.isRuntime() ? this.mm1.matches(invocation) : this.mm1.matches(method, targetClass));
      boolean bMatches = (this.mm2.isRuntime() ? this.mm2.matches(invocation) : this.mm2.matches(method, targetClass));
      return aMatches && bMatches;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof IntersectionMethodMatcher that)) {
        return false;
      }
      return (this.mm1.equals(that.mm1) && this.mm2.equals(that.mm2));
    }

    @Override
    public int hashCode() {
      return 37 * this.mm1.hashCode() + this.mm2.hashCode();
    }

    @Override
    public String toString() {
      return getClass().getName() + ": " + this.mm1 + ", " + this.mm2;
    }
  }

  /**
   * MethodMatcher implementation for an intersection of two given MethodMatchers
   * of which at least one is an IntroductionAwareMethodMatcher.
   */
  class IntersectionIntroductionAwareMethodMatcher
          extends IntersectionMethodMatcher implements IntroductionAwareMethodMatcher {

    @Serial
    private static final long serialVersionUID = 1L;

    public IntersectionIntroductionAwareMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
      super(mm1, mm2);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
      return (MethodMatcher.matches(this.mm1, method, targetClass, hasIntroductions) &&
              MethodMatcher.matches(this.mm2, method, targetClass, hasIntroductions));
    }

  }

  class NegateMethodMatcher implements MethodMatcher, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final MethodMatcher original;

    private NegateMethodMatcher(MethodMatcher original) {
      this.original = original;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
      return !this.original.matches(method, targetClass);
    }

    @Override
    public boolean isRuntime() {
      return this.original.isRuntime();
    }

    @Override
    public boolean matches(MethodInvocation invocation) {
      return !this.original.matches(invocation);
    }

    @Override
    public boolean equals(Object other) {
      return (this == other || (other instanceof NegateMethodMatcher that
              && this.original.equals(that.original)));
    }

    @Override
    public int hashCode() {
      return Objects.hash(getClass(), this.original);
    }

    @Override
    public String toString() {
      return "Negate " + this.original;
    }

  }

}
