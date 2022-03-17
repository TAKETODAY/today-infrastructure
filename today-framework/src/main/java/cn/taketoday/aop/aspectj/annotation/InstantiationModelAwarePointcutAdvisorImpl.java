/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.aop.aspectj.annotation;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.reflect.PerClauseKind;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;

import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.aspectj.AspectJExpressionPointcut;
import cn.taketoday.aop.aspectj.AspectJPrecedenceInformation;
import cn.taketoday.aop.aspectj.InstantiationModelAwarePointcutAdvisor;
import cn.taketoday.aop.aspectj.annotation.AbstractAspectJAdvisorFactory.AspectJAnnotation;
import cn.taketoday.aop.support.DynamicMethodMatcherPointcut;
import cn.taketoday.aop.support.Pointcuts;
import cn.taketoday.lang.Nullable;

/**
 * Internal implementation of AspectJPointcutAdvisor.
 * Note that there will be one instance of this advisor for each target method.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
final class InstantiationModelAwarePointcutAdvisorImpl
        implements InstantiationModelAwarePointcutAdvisor, AspectJPrecedenceInformation, Serializable {

  private static final Advice EMPTY_ADVICE = new Advice() { };

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
      Pointcut preInstantiationPointcut = Pointcuts.union(
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
    if (this.instantiatedAdvice == null) {
      this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
    }
    return this.instantiatedAdvice;
  }

  private Advice instantiateAdvice(AspectJExpressionPointcut pointcut) {
    Advice advice = this.aspectJAdvisorFactory.getAdvice(this.aspectJAdviceMethod, pointcut,
            this.aspectInstanceFactory, this.declarationOrder, this.aspectName);
    return (advice != null ? advice : EMPTY_ADVICE);
  }

  /**
   * This is only of interest for Framework AOP: AspectJ instantiation semantics
   * are much richer. In AspectJ terminology, all a return of {@code true}
   * means here is that the aspect is not a SINGLETON.
   */
  @Override
  public boolean isPerInstance() {
    return (getAspectMetadata().getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON);
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
  public boolean isBeforeAdvice() {
    if (this.isBeforeAdvice == null) {
      determineAdviceType();
    }
    return this.isBeforeAdvice;
  }

  @Override
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
    AspectJAnnotation<?> aspectJAnnotation =
            AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(this.aspectJAdviceMethod);
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
    return "InstantiationModelAwarePointcutAdvisor: expression [" + getDeclaredPointcut().getExpression() +
            "]; advice method [" + this.aspectJAdviceMethod + "]; perClauseKind=" +
            this.aspectInstanceFactory.getAspectMetadata().getAjType().getPerClause().getKind();
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
      if (aspectInstanceFactory instanceof LazySingletonAspectInstanceFactoryDecorator) {
        this.aspectInstanceFactory = (LazySingletonAspectInstanceFactoryDecorator) aspectInstanceFactory;
      }
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
      // We're either instantiated and matching on declared pointcut,
      // or uninstantiated matching on either pointcut...
      return (isAspectMaterialized() && this.declaredPointcut.matches(method, targetClass)) ||
              this.preInstantiationPointcut.getMethodMatcher().matches(method, targetClass);
    }

    @Override
    public boolean matches(MethodInvocation invocation) {
      // This can match only on declared pointcut.
      return (isAspectMaterialized() && this.declaredPointcut.matches(invocation.getMethod(), invocation.getThis().getClass()));
    }

    private boolean isAspectMaterialized() {
      return (this.aspectInstanceFactory == null || this.aspectInstanceFactory.isMaterialized());
    }
  }

}
