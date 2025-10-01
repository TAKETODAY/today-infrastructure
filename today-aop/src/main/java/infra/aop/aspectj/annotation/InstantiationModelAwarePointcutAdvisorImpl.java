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

package infra.aop.aspectj.annotation;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.reflect.PerClauseKind;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;

import infra.aop.Pointcut;
import infra.aop.aspectj.AspectJExpressionPointcut;
import infra.aop.aspectj.AspectJPrecedenceInformation;
import infra.aop.aspectj.InstantiationModelAwarePointcutAdvisor;
import infra.aop.support.DynamicMethodMatcherPointcut;
import infra.util.ObjectUtils;

/**
 * Internal implementation of AspectJPointcutAdvisor.
 * Note that there will be one instance of this advisor for each target method.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
final class InstantiationModelAwarePointcutAdvisorImpl
        implements InstantiationModelAwarePointcutAdvisor, AspectJPrecedenceInformation, Serializable {

  private final AspectJExpressionPointcut declaredPointcut;

  private final Class<?> declaringClass;

  private final String methodName;

  private final Class<?>[] parameterTypes;

  private transient Method aspectJAdviceMethod;

  private final AspectJAdvisorFactory aspectJAdvisorFactory;

  private final MetadataAwareAspectInstanceFactory aspectInstanceFactory;

  private final int declarationOrder;

  private final String aspectName;

  private final Pointcut pointcut;

  private final boolean lazy;

  @Nullable
  private Advice instantiatedAdvice;

  @Nullable
  private Boolean isBeforeAdvice;

  @Nullable
  private Boolean isAfterAdvice;

  public InstantiationModelAwarePointcutAdvisorImpl(AspectJExpressionPointcut declaredPointcut,
          Method aspectJAdviceMethod, AspectJAdvisorFactory aspectJAdvisorFactory,
          MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {

    this.declaredPointcut = declaredPointcut;
    this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
    this.methodName = aspectJAdviceMethod.getName();
    this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
    this.aspectJAdviceMethod = aspectJAdviceMethod;
    this.aspectJAdvisorFactory = aspectJAdvisorFactory;
    this.aspectInstanceFactory = aspectInstanceFactory;
    this.declarationOrder = declarationOrder;
    this.aspectName = aspectName;

    if (aspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
      // Static part of the pointcut is a lazy type.
      Pointcut preInstantiationPointcut = Pointcut.union(
              aspectInstanceFactory.getAspectMetadata().getPerClausePointcut(), this.declaredPointcut);

      // Make it dynamic: must mutate from pre-instantiation to post-instantiation state.
      // If it's not a dynamic pointcut, it may be optimized out
      // by the Framework AOP infrastructure after the first evaluation.
      this.pointcut = new PerTargetInstantiationModelPointcut(
              this.declaredPointcut, preInstantiationPointcut, aspectInstanceFactory);
      this.lazy = true;
    }
    else {
      // A singleton aspect.
      this.pointcut = this.declaredPointcut;
      this.lazy = false;
      this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
    }
  }

  /**
   * The pointcut for Framework AOP to use.
   * Actual behaviour of the pointcut will change depending on the state of the advice.
   */
  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

  @Override
  public boolean isLazy() {
    return this.lazy;
  }

  @Override
  public synchronized boolean isAdviceInstantiated() {
    return (this.instantiatedAdvice != null);
  }

  /**
   * Lazily instantiate advice if necessary.
   */
  @Override
  public synchronized Advice getAdvice() {
    if (instantiatedAdvice == null) {
      this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
    }
    return instantiatedAdvice;
  }

  private Advice instantiateAdvice(AspectJExpressionPointcut pointcut) {
    Advice advice = aspectJAdvisorFactory.getAdvice(aspectJAdviceMethod, pointcut,
            aspectInstanceFactory, declarationOrder, aspectName);
    return advice != null ? advice : EMPTY_ADVICE;
  }

  /**
   * This is only of interest for Framework AOP: AspectJ instantiation semantics
   * are much richer. In AspectJ terminology, all a return of {@code true}
   * means here is that the aspect is not a SINGLETON.
   */
  @Override
  public boolean isPerInstance() {
    return getAspectMetadata().getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON;
  }

  /**
   * Return the AspectJ AspectMetadata for this advisor.
   */
  public AspectMetadata getAspectMetadata() {
    return this.aspectInstanceFactory.getAspectMetadata();
  }

  public MetadataAwareAspectInstanceFactory getAspectInstanceFactory() {
    return this.aspectInstanceFactory;
  }

  public AspectJExpressionPointcut getDeclaredPointcut() {
    return this.declaredPointcut;
  }

  @Override
  public int getOrder() {
    return this.aspectInstanceFactory.getOrder();
  }

  @Override
  public String getAspectName() {
    return this.aspectName;
  }

  @Override
  public int getDeclarationOrder() {
    return this.declarationOrder;
  }

  @Override
  @SuppressWarnings("NullAway")
  public boolean isBeforeAdvice() {
    if (this.isBeforeAdvice == null) {
      determineAdviceType();
    }
    return this.isBeforeAdvice;
  }

  @Override
  @SuppressWarnings("NullAway")
  public boolean isAfterAdvice() {
    if (this.isAfterAdvice == null) {
      determineAdviceType();
    }
    return this.isAfterAdvice;
  }

  /**
   * Duplicates some logic from getAdvice, but importantly does not force
   * creation of the advice.
   */
  private void determineAdviceType() {
    AbstractAspectJAdvisorFactory.AspectJAnnotation aspectJAnnotation = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(this.aspectJAdviceMethod);
    if (aspectJAnnotation == null) {
      this.isBeforeAdvice = false;
      this.isAfterAdvice = false;
    }
    else {
      switch (aspectJAnnotation.getAnnotationType()) {
        case AtPointcut, AtAround -> {
          this.isBeforeAdvice = false;
          this.isAfterAdvice = false;
        }
        case AtBefore -> {
          this.isBeforeAdvice = true;
          this.isAfterAdvice = false;
        }
        case AtAfter, AtAfterReturning, AtAfterThrowing -> {
          this.isBeforeAdvice = false;
          this.isAfterAdvice = true;
        }
      }
    }
  }

  @Serial
  private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
    inputStream.defaultReadObject();
    try {
      this.aspectJAdviceMethod = this.declaringClass.getMethod(this.methodName, this.parameterTypes);
    }
    catch (NoSuchMethodException ex) {
      throw new IllegalStateException("Failed to find advice method on deserialization", ex);
    }
  }

  @Override
  public String toString() {
    return "InstantiationModelAwarePointcutAdvisor: expression [" +
            getDeclaredPointcut().getExpression() + "]; advice method [" +
            aspectJAdviceMethod + "]; perClauseKind=" +
            aspectInstanceFactory.getAspectMetadata().getAjType().getPerClause().getKind();
  }

  /**
   * Pointcut implementation that changes its behaviour when the advice is instantiated.
   * Note that this is a <i>dynamic</i> pointcut; otherwise it might be optimized out
   * if it does not at first match statically.
   */
  private static final class PerTargetInstantiationModelPointcut extends DynamicMethodMatcherPointcut {

    private final AspectJExpressionPointcut declaredPointcut;

    private final Pointcut preInstantiationPointcut;

    @Nullable
    private LazySingletonAspectInstanceFactoryDecorator aspectInstanceFactory;

    public PerTargetInstantiationModelPointcut(AspectJExpressionPointcut declaredPointcut,
            Pointcut preInstantiationPointcut, MetadataAwareAspectInstanceFactory aspectInstanceFactory) {

      this.declaredPointcut = declaredPointcut;
      this.preInstantiationPointcut = preInstantiationPointcut;
      if (aspectInstanceFactory instanceof LazySingletonAspectInstanceFactoryDecorator lazyFactory) {
        this.aspectInstanceFactory = lazyFactory;
      }
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
      // We're either instantiated and matching on declared pointcut,
      // or uninstantiated matching on either pointcut...
      return (isAspectMaterialized() && declaredPointcut.matches(method, targetClass))
              || preInstantiationPointcut.getMethodMatcher().matches(method, targetClass);
    }

    @Override
    public boolean matches(MethodInvocation invocation) {
      // This can match only on declared pointcut.
      return isAspectMaterialized() && declaredPointcut.matches(invocation);
    }

    private boolean isAspectMaterialized() {
      return (this.aspectInstanceFactory == null || this.aspectInstanceFactory.isMaterialized());
    }

    @Override
    public boolean equals(@Nullable Object other) {
      // For equivalence, we only need to compare the preInstantiationPointcut fields since
      // they include the declaredPointcut fields. In addition, we should not compare the
      // aspectInstanceFactory fields since LazySingletonAspectInstanceFactoryDecorator does
      // not implement equals().
      return (this == other || (other instanceof PerTargetInstantiationModelPointcut that &&
              ObjectUtils.nullSafeEquals(this.preInstantiationPointcut, that.preInstantiationPointcut)));
    }

    @Override
    public int hashCode() {
      return ObjectUtils.nullSafeHashCode(this.declaredPointcut.getExpression());
    }

    @Override
    public String toString() {
      return PerTargetInstantiationModelPointcut.class.getName() + ": " + this.declaredPointcut.getExpression();
    }

  }

}
