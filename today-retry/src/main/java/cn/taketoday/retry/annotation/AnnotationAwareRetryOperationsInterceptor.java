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

package cn.taketoday.retry.annotation;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.OperationNotSupportedException;

import cn.taketoday.aop.IntroductionInterceptor;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.expression.BeanFactoryResolver;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.common.TemplateParserContext;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.retry.backoff.BackOffPolicy;
import cn.taketoday.retry.backoff.BackOffPolicyBuilder;
import cn.taketoday.retry.backoff.NoBackOffPolicy;
import cn.taketoday.retry.backoff.Sleeper;
import cn.taketoday.retry.interceptor.FixedKeyGenerator;
import cn.taketoday.retry.interceptor.MethodArgumentsKeyGenerator;
import cn.taketoday.retry.interceptor.MethodInvocationRecoverer;
import cn.taketoday.retry.interceptor.NewMethodArgumentsIdentifier;
import cn.taketoday.retry.interceptor.RetryInterceptorBuilder;
import cn.taketoday.retry.policy.CircuitBreakerRetryPolicy;
import cn.taketoday.retry.policy.ExpressionRetryPolicy;
import cn.taketoday.retry.policy.MapRetryContextCache;
import cn.taketoday.retry.policy.RetryContextCache;
import cn.taketoday.retry.policy.SimpleRetryPolicy;
import cn.taketoday.retry.support.Args;
import cn.taketoday.retry.support.RetrySynchronizationManager;
import cn.taketoday.retry.support.RetryTemplate;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.ReflectionUtils.MethodCallback;
import cn.taketoday.util.StringUtils;

import static cn.taketoday.retry.policy.ExpressionRetryPolicy.isTemplate;

/**
 * Interceptor that parses the retry metadata on the method it is invoking and delegates
 * to an appropriate RetryOperationsInterceptor.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 * @since 4.0
 */
public class AnnotationAwareRetryOperationsInterceptor implements IntroductionInterceptor, BeanFactoryAware {

  private static final TemplateParserContext PARSER_CONTEXT = new TemplateParserContext();

  private static final SpelExpressionParser PARSER = new SpelExpressionParser();

  private static final MethodInterceptor NULL_INTERCEPTOR = methodInvocation -> {
    throw new OperationNotSupportedException("Not supported");
  };

  private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

  private final ConcurrentReferenceHashMap<Object, ConcurrentMap<Method, MethodInterceptor>> delegates = new ConcurrentReferenceHashMap<>();

  private RetryContextCache retryContextCache = new MapRetryContextCache();

  private MethodArgumentsKeyGenerator methodArgumentsKeyGenerator;

  private NewMethodArgumentsIdentifier newMethodArgumentsIdentifier;

  private Sleeper sleeper;

  private BeanFactory beanFactory;

  private RetryListener[] globalListeners;

  /**
   * @param sleeper the sleeper to set
   */
  public void setSleeper(Sleeper sleeper) {
    this.sleeper = sleeper;
  }

  /**
   * Public setter for the {@link RetryContextCache}.
   *
   * @param retryContextCache the {@link RetryContextCache} to set.
   */
  public void setRetryContextCache(RetryContextCache retryContextCache) {
    this.retryContextCache = retryContextCache;
  }

  /**
   * @param methodArgumentsKeyGenerator the {@link MethodArgumentsKeyGenerator}
   */
  public void setKeyGenerator(MethodArgumentsKeyGenerator methodArgumentsKeyGenerator) {
    this.methodArgumentsKeyGenerator = methodArgumentsKeyGenerator;
  }

  /**
   * @param newMethodArgumentsIdentifier the {@link NewMethodArgumentsIdentifier}
   */
  public void setNewItemIdentifier(NewMethodArgumentsIdentifier newMethodArgumentsIdentifier) {
    this.newMethodArgumentsIdentifier = newMethodArgumentsIdentifier;
  }

  /**
   * Default retry listeners to apply to all operations.
   *
   * @param globalListeners the default listeners
   */
  public void setListeners(Collection<RetryListener> globalListeners) {
    ArrayList<RetryListener> retryListeners = new ArrayList<>(globalListeners);
    AnnotationAwareOrderComparator.sort(retryListeners);
    this.globalListeners = retryListeners.toArray(new RetryListener[0]);
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
    this.evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
  }

  @Override
  public boolean implementsInterface(Class<?> intf) {
    return cn.taketoday.retry.interceptor.Retryable.class.isAssignableFrom(intf);
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    MethodInterceptor delegate = getDelegate(invocation.getThis(), invocation.getMethod());
    if (delegate != null) {
      return delegate.invoke(invocation);
    }
    else {
      return invocation.proceed();
    }
  }

  private MethodInterceptor getDelegate(Object target, Method method) {
    ConcurrentMap<Method, MethodInterceptor> cachedMethods = this.delegates.get(target);
    if (cachedMethods == null) {
      cachedMethods = new ConcurrentHashMap<>();
    }
    MethodInterceptor delegate = cachedMethods.get(method);
    if (delegate == null) {
      MethodInterceptor interceptor = NULL_INTERCEPTOR;
      Retryable retryable = AnnotatedElementUtils.findMergedAnnotation(method, Retryable.class);
      if (retryable == null) {
        retryable = classLevelAnnotation(method, Retryable.class);
      }
      if (retryable == null) {
        retryable = findAnnotationOnTarget(target, method, Retryable.class);
      }
      if (retryable != null) {
        if (StringUtils.hasText(retryable.interceptor())) {
          interceptor = this.beanFactory.getBean(retryable.interceptor(), MethodInterceptor.class);
        }
        else if (retryable.stateful()) {
          interceptor = getStatefulInterceptor(target, method, retryable);
        }
        else {
          interceptor = getStatelessInterceptor(target, method, retryable);
        }
      }
      cachedMethods.putIfAbsent(method, interceptor);
      delegate = cachedMethods.get(method);
    }
    this.delegates.putIfAbsent(target, cachedMethods);
    return delegate == NULL_INTERCEPTOR ? null : delegate;
  }

  private <A extends Annotation> A findAnnotationOnTarget(Object target, Method method, Class<A> annotation) {

    try {
      Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
      A retryable = AnnotatedElementUtils.findMergedAnnotation(targetMethod, annotation);
      if (retryable == null) {
        retryable = classLevelAnnotation(targetMethod, annotation);
      }

      return retryable;
    }
    catch (Exception e) {
      return null;
    }
  }

  /**
   * With a class level annotation, exclude @Recover methods.
   */
  private <A extends Annotation> A classLevelAnnotation(Method method, Class<A> annotation) {
    A ann = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), annotation);
    if (ann != null && AnnotatedElementUtils.findMergedAnnotation(method, Recover.class) != null) {
      ann = null;
    }
    return ann;
  }

  private MethodInterceptor getStatelessInterceptor(Object target, Method method, Retryable retryable) {
    RetryTemplate template = createTemplate(retryable.listeners());
    template.setRetryPolicy(getRetryPolicy(retryable, true));
    template.setBackOffPolicy(getBackoffPolicy(retryable.backoff(), true));
    return RetryInterceptorBuilder.stateless()
            .retryOperations(template)
            .label(retryable.label())
            .recoverer(getRecoverer(target, method))
            .build();
  }

  private MethodInterceptor getStatefulInterceptor(Object target, Method method, Retryable retryable) {
    RetryTemplate template = createTemplate(retryable.listeners());
    template.setRetryContextCache(this.retryContextCache);

    CircuitBreaker circuit = AnnotatedElementUtils.findMergedAnnotation(method, CircuitBreaker.class);
    if (circuit == null) {
      circuit = findAnnotationOnTarget(target, method, CircuitBreaker.class);
    }
    if (circuit != null) {
      RetryPolicy policy = getRetryPolicy(circuit, false);
      CircuitBreakerRetryPolicy breaker = new CircuitBreakerRetryPolicy(policy);
      openTimeout(breaker, circuit);
      resetTimeout(breaker, circuit);
      template.setRetryPolicy(breaker);
      template.setBackOffPolicy(new NoBackOffPolicy());
      String label = circuit.label();
      if (!StringUtils.hasText(label)) {
        label = method.toGenericString();
      }
      return RetryInterceptorBuilder.circuitBreaker()
              .keyGenerator(new FixedKeyGenerator("circuit"))
              .retryOperations(template)
              .recoverer(getRecoverer(target, method))
              .label(label)
              .build();
    }
    RetryPolicy policy = getRetryPolicy(retryable, false);
    template.setRetryPolicy(policy);
    template.setBackOffPolicy(getBackoffPolicy(retryable.backoff(), false));
    String label = retryable.label();
    return RetryInterceptorBuilder.stateful()
            .keyGenerator(this.methodArgumentsKeyGenerator)
            .newMethodArgumentsIdentifier(this.newMethodArgumentsIdentifier)
            .retryOperations(template)
            .label(label)
            .recoverer(getRecoverer(target, method))
            .build();
  }

  private void openTimeout(CircuitBreakerRetryPolicy breaker, CircuitBreaker circuit) {
    String expression = circuit.openTimeoutExpression();
    if (StringUtils.hasText(expression)) {
      Expression parsed = parse(expression);
      if (isTemplate(expression)) {
        Long value = parsed.getValue(this.evaluationContext, Long.class);
        if (value != null) {
          breaker.setOpenTimeout(value);
          return;
        }
      }
      else {
        breaker.setOpenTimeout(() -> evaluate(parsed, Long.class, false));
        return;
      }
    }
    breaker.setOpenTimeout(circuit.openTimeout());
  }

  private void resetTimeout(CircuitBreakerRetryPolicy breaker, CircuitBreaker circuit) {
    String expression = circuit.resetTimeoutExpression();
    if (StringUtils.hasText(expression)) {
      Expression parsed = parse(expression);
      if (isTemplate(expression)) {
        Long value = parsed.getValue(this.evaluationContext, Long.class);
        if (value != null) {
          breaker.setResetTimeout(value);
          return;
        }
      }
      else {
        breaker.setResetTimeout(() -> evaluate(parsed, Long.class, false));
      }
    }
    breaker.setResetTimeout(circuit.resetTimeout());
  }

  private RetryTemplate createTemplate(String[] listenersBeanNames) {
    RetryTemplate template = new RetryTemplate();
    if (listenersBeanNames.length > 0) {
      template.setListeners(getListenersBeans(listenersBeanNames));
    }
    else if (this.globalListeners != null) {
      template.setListeners(this.globalListeners);
    }
    return template;
  }

  private RetryListener[] getListenersBeans(String[] listenersBeanNames) {
    RetryListener[] listeners = new RetryListener[listenersBeanNames.length];
    for (int i = 0; i < listeners.length; i++) {
      listeners[i] = this.beanFactory.getBean(listenersBeanNames[i], RetryListener.class);
    }
    return listeners;
  }

  private MethodInvocationRecoverer<?> getRecoverer(Object target, Method method) {
    if (target instanceof MethodInvocationRecoverer) {
      return (MethodInvocationRecoverer<?>) target;
    }
    final AtomicBoolean foundRecoverable = new AtomicBoolean(false);
    ReflectionUtils.doWithMethods(target.getClass(), candidate -> {
      if (AnnotatedElementUtils.findMergedAnnotation(candidate, Recover.class) != null) {
        foundRecoverable.set(true);
      }
    });

    if (!foundRecoverable.get()) {
      return null;
    }
    return new RecoverAnnotationRecoveryHandler<>(target, method);
  }

  private RetryPolicy getRetryPolicy(Annotation retryable, boolean stateless) {
    Map<String, Object> attrs = AnnotationUtils.getAnnotationAttributes(retryable);
    @SuppressWarnings("unchecked")
    Class<? extends Throwable>[] includes = (Class<? extends Throwable>[]) attrs.get("value");
    String exceptionExpression = (String) attrs.get("exceptionExpression");
    boolean hasExpression = StringUtils.hasText(exceptionExpression);
    if (includes.length == 0) {
      @SuppressWarnings("unchecked")
      Class<? extends Throwable>[] value = (Class<? extends Throwable>[]) attrs.get("include");
      includes = value;
    }
    @SuppressWarnings("unchecked")
    Class<? extends Throwable>[] excludes = (Class<? extends Throwable>[]) attrs.get("exclude");
    Integer maxAttempts = (Integer) attrs.get("maxAttempts");
    String maxAttemptsExpression = (String) attrs.get("maxAttemptsExpression");
    Expression parsedExpression = null;
    if (StringUtils.hasText(maxAttemptsExpression)) {
      parsedExpression = parse(maxAttemptsExpression);
      if (isTemplate(maxAttemptsExpression)) {
        maxAttempts = parsedExpression.getValue(this.evaluationContext, Integer.class);
        parsedExpression = null;
      }
    }
    final Expression expression = parsedExpression;
    if (includes.length == 0 && excludes.length == 0) {
      SimpleRetryPolicy simple = hasExpression
                                 ? new ExpressionRetryPolicy(resolve(exceptionExpression)).withBeanFactory(this.beanFactory)
                                 : new SimpleRetryPolicy();
      if (expression != null) {
        simple.setMaxAttempts(() -> evaluate(expression, Integer.class, stateless));
      }
      else {
        simple.setMaxAttempts(maxAttempts);
      }
      return simple;
    }
    Map<Class<? extends Throwable>, Boolean> policyMap = new HashMap<>();
    for (Class<? extends Throwable> type : includes) {
      policyMap.put(type, true);
    }
    for (Class<? extends Throwable> type : excludes) {
      policyMap.put(type, false);
    }
    boolean retryNotExcluded = includes.length == 0;
    if (hasExpression) {
      return new ExpressionRetryPolicy(maxAttempts, policyMap, true, exceptionExpression, retryNotExcluded)
              .withBeanFactory(this.beanFactory);
    }
    else {
      SimpleRetryPolicy policy = new SimpleRetryPolicy(maxAttempts, policyMap, true, retryNotExcluded);
      if (expression != null) {
        policy.setMaxAttempts(() -> evaluate(expression, Integer.class, stateless));
      }
      return policy;
    }
  }

  private BackOffPolicy getBackoffPolicy(Backoff backoff, boolean stateless) {
    Map<String, Object> attrs = AnnotationUtils.getAnnotationAttributes(backoff);
    long min = backoff.delay() == 0 ? backoff.value() : backoff.delay();
    String delayExpression = (String) attrs.get("delayExpression");
    Expression parsedMinExp = null;
    if (StringUtils.hasText(delayExpression)) {
      parsedMinExp = parse(delayExpression);
      if (isTemplate(delayExpression)) {
        min = parsedMinExp.getValue(this.evaluationContext, Long.class);
        parsedMinExp = null;
      }
    }
    long max = backoff.maxDelay();
    String maxDelayExpression = (String) attrs.get("maxDelayExpression");
    Expression parsedMaxExp = null;
    if (StringUtils.hasText(maxDelayExpression)) {
      parsedMaxExp = parse(maxDelayExpression);
      if (isTemplate(maxDelayExpression)) {
        max = parsedMaxExp.getValue(this.evaluationContext, Long.class);
        parsedMaxExp = null;
      }
    }
    double multiplier = backoff.multiplier();
    String multiplierExpression = (String) attrs.get("multiplierExpression");
    Expression parsedMultExp = null;
    if (StringUtils.hasText(multiplierExpression)) {
      parsedMultExp = parse(multiplierExpression);
      if (isTemplate(multiplierExpression)) {
        multiplier = parsedMultExp.getValue(this.evaluationContext, Double.class);
        parsedMultExp = null;
      }
    }
    boolean isRandom = false;
    String randomExpression = (String) attrs.get("randomExpression");
    Expression parsedRandomExp = null;
    if (multiplier > 0) {
      isRandom = backoff.random();
      if (StringUtils.hasText(randomExpression)) {
        parsedRandomExp = parse(randomExpression);
        if (isTemplate(randomExpression)) {
          isRandom = parsedRandomExp.getValue(this.evaluationContext, Boolean.class);
          parsedRandomExp = null;
        }
      }
    }
    return buildBackOff(min, parsedMinExp, max, parsedMaxExp, multiplier, parsedMultExp, isRandom, parsedRandomExp,
            stateless);
  }

  private BackOffPolicy buildBackOff(long min, Expression minExp, long max, Expression maxExp, double multiplier,
          Expression multExp, boolean isRandom, Expression randomExp, boolean stateless) {

    BackOffPolicyBuilder builder = BackOffPolicyBuilder.newBuilder();
    if (minExp != null) {
      builder.delaySupplier(() -> evaluate(minExp, Long.class, stateless));
    }
    else {
      builder.delay(min);
    }
    if (maxExp != null) {
      builder.maxDelaySupplier(() -> evaluate(maxExp, Long.class, stateless));
    }
    else {
      builder.maxDelay(max);
    }
    if (multExp != null) {
      builder.multiplierSupplier(() -> evaluate(multExp, Double.class, stateless));
    }
    else {
      builder.multiplier(multiplier);
    }
    if (randomExp != null) {
      builder.randomSupplier(() -> evaluate(randomExp, Boolean.class, stateless));
    }
    else {
      builder.random(isRandom);
    }
    builder.sleeper(this.sleeper);
    return builder.build();
  }

  private Expression parse(String expression) {
    if (isTemplate(expression)) {
      return PARSER.parseExpression(resolve(expression), PARSER_CONTEXT);
    }
    else {
      return PARSER.parseExpression(resolve(expression));
    }
  }

  private boolean isTemplate(String expression) {
    return expression.contains(PARSER_CONTEXT.getExpressionPrefix())
            && expression.contains(PARSER_CONTEXT.getExpressionSuffix());
  }

  private <T> T evaluate(Expression expression, Class<T> type, boolean stateless) {
    Args args = null;
    if (stateless) {
      RetryContext context = RetrySynchronizationManager.getContext();
      if (context != null) {
        args = (Args) context.getAttribute("ARGS");
      }
      if (args == null) {
        args = Args.NO_ARGS;
      }
    }
    return expression.getValue(this.evaluationContext, args, type);
  }

  /**
   * Resolve the specified value if possible.
   *
   * @see ConfigurableBeanFactory#resolveEmbeddedValue
   */
  private String resolve(String value) {
    if (this.beanFactory != null && this.beanFactory instanceof ConfigurableBeanFactory) {
      return ((ConfigurableBeanFactory) this.beanFactory).resolveEmbeddedValue(value);
    }
    return value;
  }

}
