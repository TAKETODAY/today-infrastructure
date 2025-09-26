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
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

import infra.aop.Advisor;
import infra.aop.aspectj.AspectJAfterAdvice;
import infra.aop.aspectj.AspectJAfterReturningAdvice;
import infra.aop.aspectj.AspectJAfterThrowingAdvice;
import infra.aop.aspectj.AspectJAroundAdvice;
import infra.aop.aspectj.AspectJExpressionPointcut;
import infra.aop.aspectj.AspectJMethodBeforeAdvice;
import infra.aop.framework.AopConfigException;

/**
 * Interface for factories that can create Framework AOP Advisors from classes
 * annotated with AspectJ annotation syntax.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AspectMetadata
 * @see org.aspectj.lang.reflect.AjTypeSystem
 * @since 4.0
 */
public interface AspectJAdvisorFactory {

  /**
   * Determine whether or not the given class is an aspect, as reported
   * by AspectJ's {@link org.aspectj.lang.reflect.AjTypeSystem}.
   * <p>Will simply return {@code false} if the supposed aspect is
   * invalid (such as an extension of a concrete aspect class).
   * Will return true for some aspects that Framework AOP cannot process,
   * such as those with unsupported instantiation models.
   * Use the {@link #validate} method to handle these cases if necessary.
   *
   * @param clazz the supposed annotation-style AspectJ class
   * @return whether or not this class is recognized by AspectJ as an aspect class
   */
  boolean isAspect(Class<?> clazz);

  /**
   * Is the given class a valid AspectJ aspect class?
   *
   * @param aspectClass the supposed AspectJ annotation-style class to validate
   * @throws AopConfigException if the class is an invalid aspect
   * (which can never be legal)
   * @throws NotAnAtAspectException if the class is not an aspect at all
   * (which may or may not be legal, depending on the context)
   */
  void validate(Class<?> aspectClass) throws AopConfigException;

  /**
   * Build Framework AOP Advisors for all annotated At-AspectJ methods
   * on the specified aspect instance.
   *
   * @param aspectInstanceFactory the aspect instance factory
   * (not the aspect instance itself in order to avoid eager instantiation)
   * @return a list of advisors for this class
   */
  List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory);

  /**
   * Build a Framework AOP Advisor for the given AspectJ advice method.
   *
   * @param candidateAdviceMethod the candidate advice method
   * @param aspectInstanceFactory the aspect instance factory
   * @param declarationOrder the declaration order within the aspect
   * @param aspectName the name of the aspect
   * @return {@code null} if the method is not an AspectJ advice method
   * or if it is a pointcut that will be used by other advice but will not
   * create a Framework advice in its own right
   */
  @Nullable
  Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
          int declarationOrder, String aspectName);

  /**
   * Build a Framework AOP Advice for the given AspectJ advice method.
   *
   * @param candidateAdviceMethod the candidate advice method
   * @param expressionPointcut the AspectJ expression pointcut
   * @param aspectInstanceFactory the aspect instance factory
   * @param declarationOrder the declaration order within the aspect
   * @param aspectName the name of the aspect
   * @return {@code null} if the method is not an AspectJ advice method
   * or if it is a pointcut that will be used by other advice but will not
   * create a Framework advice in its own right
   * @see AspectJAroundAdvice
   * @see AspectJMethodBeforeAdvice
   * @see AspectJAfterAdvice
   * @see AspectJAfterReturningAdvice
   * @see AspectJAfterThrowingAdvice
   */
  @Nullable
  Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
          MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName);

}
