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
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareParents;
import org.aspectj.lang.annotation.Pointcut;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.MethodBeforeAdvice;
import cn.taketoday.aop.aspectj.AbstractAspectJAdvice;
import cn.taketoday.aop.aspectj.AspectJAfterAdvice;
import cn.taketoday.aop.aspectj.AspectJAfterReturningAdvice;
import cn.taketoday.aop.aspectj.AspectJAfterThrowingAdvice;
import cn.taketoday.aop.aspectj.AspectJAroundAdvice;
import cn.taketoday.aop.aspectj.AspectJExpressionPointcut;
import cn.taketoday.aop.aspectj.AspectJMethodBeforeAdvice;
import cn.taketoday.aop.aspectj.DeclareParentsAdvisor;
import cn.taketoday.aop.framework.AopConfigException;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConvertingComparator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.ReflectionUtils.MethodFilter;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.comparator.InstanceComparator;

/**
 * Factory that can create Framework AOP Advisors given AspectJ classes from
 * classes honoring AspectJ's annotation syntax, using reflection to invoke the
 * corresponding advice methods.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ReflectiveAspectJAdvisorFactory extends AbstractAspectJAdvisorFactory implements Serializable {

  // Exclude @Pointcut methods
  private static final MethodFilter adviceMethodFilter =
          ReflectionUtils.USER_DECLARED_METHODS.and(
                  method -> AnnotationUtils.getAnnotation(method, Pointcut.class) == null);

  private static final Comparator<Method> adviceMethodComparator;

  static {
    // Note: although @After is ordered before @AfterReturning and @AfterThrowing,
    // an @After advice method will actually be invoked after @AfterReturning and
    // @AfterThrowing methods due to the fact that AspectJAfterAdvice.invoke(MethodInvocation)
    // invokes proceed() in a `try` block and only invokes the @After advice method
    // in a corresponding `finally` block.
    Comparator<Method> adviceKindComparator = new ConvertingComparator<>(
            new InstanceComparator<>(
                    Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class),
            (Converter<Method, Annotation>) method -> {
              AspectJAnnotation<?> ann = findAspectJAnnotationOnMethod(method);
              return ann != null ? ann.getAnnotation() : null;
            });
    Comparator<Method> methodNameComparator = new ConvertingComparator<>(Method::getName);
    adviceMethodComparator = adviceKindComparator.thenComparing(methodNameComparator);
  }

  @Nullable
  private final BeanFactory beanFactory;

  /**
   * Create a new {@code ReflectiveAspectJAdvisorFactory}.
   */
  public ReflectiveAspectJAdvisorFactory() {
    this(null);
  }

  /**
   * Create a new {@code ReflectiveAspectJAdvisorFactory}, propagating the given
   * {@link BeanFactory} to the created {@link AspectJExpressionPointcut} instances,
   * for bean pointcut handling as well as consistent {@link ClassLoader} resolution.
   *
   * @param beanFactory the BeanFactory to propagate (may be {@code null}}
   * @see AspectJExpressionPointcut#setBeanFactory
   * @see cn.taketoday.beans.factory.config.ConfigurableBeanFactory#getBeanClassLoader()
   */
  public ReflectiveAspectJAdvisorFactory(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
    Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
    String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
    validate(aspectClass);

    // We need to wrap the MetadataAwareAspectInstanceFactory with a decorator
    // so that it will only instantiate once.
    MetadataAwareAspectInstanceFactory factory =
            new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);

    ArrayList<Advisor> advisors = new ArrayList<>();
    for (Method method : getAdvisorMethods(aspectClass)) {
      // Prior to 4.0, advisors.size() was supplied as the declarationOrderInAspect
      // to getAdvisor(...) to represent the "current position" in the declared methods list.
      // However, since Java 7 the "current position" is not valid since the JDK no longer
      // returns declared methods in the order in which they are declared in the source code.
      // Thus, we now hard code the declarationOrderInAspect to 0 for all advice methods
      // discovered via reflection in order to support reliable advice ordering across JVM launches.
      // Specifically, a value of 0 aligns with the default value used in
      // AspectJPrecedenceComparator.getAspectDeclarationOrder(Advisor).
      Advisor advisor = getAdvisor(method, factory, 0, aspectName);
      if (advisor != null) {
        advisors.add(advisor);
      }
    }

    // If it's a per target aspect, emit the dummy instantiating aspect.
    if (!advisors.isEmpty() && factory.getAspectMetadata().isLazilyInstantiated()) {
      Advisor instantiationAdvisor = new SyntheticInstantiationAdvisor(factory);
      advisors.add(0, instantiationAdvisor);
    }

    // Find introduction fields.
    for (Field field : aspectClass.getDeclaredFields()) {
      Advisor advisor = getDeclareParentsAdvisor(field);
      if (advisor != null) {
        advisors.add(advisor);
      }
    }

    return advisors;
  }

  private List<Method> getAdvisorMethods(Class<?> aspectClass) {
    ArrayList<Method> methods = new ArrayList<>();
    ReflectionUtils.doWithMethods(aspectClass, methods::add, adviceMethodFilter);
    if (methods.size() > 1) {
      methods.sort(adviceMethodComparator);
    }
    return methods;
  }

  /**
   * Build a {@link DeclareParentsAdvisor}
   * for the given introduction field.
   * <p>Resulting Advisors will need to be evaluated for targets.
   *
   * @param introductionField the field to introspect
   * @return the Advisor instance, or {@code null} if not an Advisor
   */
  @Nullable
  private Advisor getDeclareParentsAdvisor(Field introductionField) {
    DeclareParents declareParents = introductionField.getAnnotation(DeclareParents.class);
    if (declareParents == null) {
      // Not an introduction field
      return null;
    }

    if (DeclareParents.class == declareParents.defaultImpl()) {
      throw new IllegalStateException("'defaultImpl' attribute must be set on DeclareParents");
    }

    return new DeclareParentsAdvisor(
            introductionField.getType(), declareParents.value(), declareParents.defaultImpl());
  }

  @Override
  @Nullable
  public Advisor getAdvisor(Method candidateAdviceMethod,
          MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrderInAspect, String aspectName) {

    validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());

    AspectJExpressionPointcut expressionPointcut = getPointcut(
            candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
    if (expressionPointcut == null) {
      return null;
    }

    return new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod,
            this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
  }

  @Nullable
  private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
    AspectJAnnotation<?> aspectJAnnotation =
            findAspectJAnnotationOnMethod(candidateAdviceMethod);
    if (aspectJAnnotation == null) {
      return null;
    }

    AspectJExpressionPointcut ajexp =
            new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class<?>[0]);
    ajexp.setExpression(aspectJAnnotation.getPointcutExpression());
    if (this.beanFactory != null) {
      ajexp.setBeanFactory(this.beanFactory);
    }
    return ajexp;
  }

  @Override
  @Nullable
  public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
          MetadataAwareAspectInstanceFactory aspectFactory, int declarationOrder, String aspectName) {

    Class<?> candidateAspectClass = aspectFactory.getAspectMetadata().getAspectClass();
    validate(candidateAspectClass);

    AspectJAnnotation<?> aspectJAnnotation =
            findAspectJAnnotationOnMethod(candidateAdviceMethod);
    if (aspectJAnnotation == null) {
      return null;
    }

    // If we get here, we know we have an AspectJ method.
    // Check that it's an AspectJ-annotated class
    if (!isAspect(candidateAspectClass)) {
      throw new AopConfigException("Advice must be declared inside an aspect type: " +
              "Offending method '" + candidateAdviceMethod + "' in class [" +
              candidateAspectClass.getName() + "]");
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Found AspectJ method: {}", candidateAdviceMethod);
    }

    AbstractAspectJAdvice advice;

    switch (aspectJAnnotation.getAnnotationType()) {
      case AtPointcut -> {
        if (logger.isDebugEnabled()) {
          logger.debug("Processing pointcut '{}'", candidateAdviceMethod.getName());
        }
        return null;
      }
      case AtAfter -> advice = new AspectJAfterAdvice(candidateAdviceMethod, expressionPointcut, aspectFactory);
      case AtAround -> advice = new AspectJAroundAdvice(candidateAdviceMethod, expressionPointcut, aspectFactory);
      case AtBefore -> advice = new AspectJMethodBeforeAdvice(candidateAdviceMethod, expressionPointcut, aspectFactory);
      case AtAfterReturning -> {
        advice = new AspectJAfterReturningAdvice(candidateAdviceMethod, expressionPointcut, aspectFactory);
        AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
        if (StringUtils.hasText(afterReturningAnnotation.returning())) {
          advice.setReturningName(afterReturningAnnotation.returning());
        }
      }
      case AtAfterThrowing -> {
        advice = new AspectJAfterThrowingAdvice(candidateAdviceMethod, expressionPointcut, aspectFactory);
        AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
        if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
          advice.setThrowingName(afterThrowingAnnotation.throwing());
        }
      }
      default -> throw new UnsupportedOperationException(
              "Unsupported advice type on method: " + candidateAdviceMethod);
    }

    // Now to configure the advice...
    advice.setAspectName(aspectName);
    advice.setDeclarationOrder(declarationOrder);

    String[] argNames = parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
    if (argNames != null) {
      advice.setArgumentNamesFromStringArray(argNames);
    }
    advice.calculateArgumentBindings();

    return advice;
  }

  /**
   * Synthetic advisor that instantiates the aspect.
   * Triggered by per-clause pointcut on non-singleton aspect.
   * The advice has no effect.
   */
  protected static class SyntheticInstantiationAdvisor extends DefaultPointcutAdvisor {

    public SyntheticInstantiationAdvisor(final MetadataAwareAspectInstanceFactory aif) {
      super(aif.getAspectMetadata().getPerClausePointcut(), (MethodBeforeAdvice)
              inv -> aif.getAspectInstance());
    }
  }

}
