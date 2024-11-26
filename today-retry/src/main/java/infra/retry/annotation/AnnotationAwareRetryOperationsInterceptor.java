/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.retry.annotation;

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

import infra.aop.IntroductionInterceptor;
import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.expression.BeanFactoryResolver;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.core.annotation.AnnotationUtils;
import infra.expression.Expression;
import infra.expression.ParserContext;
import infra.expression.spel.standard.SpelExpressionParser;
import infra.expression.spel.support.StandardEvaluationContext;
import infra.retry.RetryContext;
import infra.retry.RetryListener;
import infra.retry.RetryPolicy;
import infra.retry.backoff.BackOffPolicy;
import infra.retry.backoff.BackOffPolicyBuilder;
import infra.retry.backoff.NoBackOffPolicy;
import infra.retry.backoff.Sleeper;
import infra.retry.interceptor.FixedKeyGenerator;
import infra.retry.interceptor.MethodArgumentsKeyGenerator;
import infra.retry.interceptor.MethodInvocationRecoverer;
import infra.retry.interceptor.NewMethodArgumentsIdentifier;
import infra.retry.interceptor.RetryInterceptorBuilder;
import infra.retry.policy.CircuitBreakerRetryPolicy;
import infra.retry.policy.ExpressionRetryPolicy;
import infra.retry.policy.MapRetryContextCache;
import infra.retry.policy.RetryContextCache;
import infra.retry.policy.SimpleRetryPolicy;
import infra.retry.support.Args;
import infra.retry.support.RetrySynchronizationManager;
import infra.retry.support.RetryTemplate;
import infra.util.ConcurrentReferenceHashMap;
import infra.util.ObjectUtils;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

/**
 * Interceptor that parses the retry metadata on the method it is invoking and delegates
 * to an appropriate RetryOperationsInterceptor.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class AnnotationAwareRetryOperationsInterceptor implements IntroductionInterceptor, BeanFactoryAware {

  private static final ParserContext PARSER_CONTEXT = ParserContext.TEMPLATE_EXPRESSION;

  private static final SpelExpressionParser PARSER = SpelExpressionParser.INSTANCE;

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
    return infra.retry.interceptor.Retryable.class.isAssignableFrom(intf);
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
      if (StringUtils.isBlank(label)) {
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
        breaker.openTimeoutSupplier(() -> evaluate(parsed, Long.class, false));
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
        breaker.resetTimeoutSupplier(() -> evaluate(parsed, Long.class, false));
      }
    }
    breaker.setResetTimeout(circuit.resetTimeout());
  }

  private RetryTemplate createTemplate(String[] listenersBeanNames) {
    RetryTemplate template = new RetryTemplate();
    if (listenersBeanNames.length > 0) {
      if (listenersBeanNames.length > 1 || StringUtils.hasText(listenersBeanNames[0])) {
        template.setListeners(getListenersBeans(listenersBeanNames));
      }
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

  @SuppressWarnings("unchecked")
  private RetryPolicy getRetryPolicy(Annotation retryable, boolean stateless) {
    Map<String, Object> attrs = AnnotationUtils.getAnnotationAttributes(retryable);
    @SuppressWarnings("unchecked")
    Class<? extends Throwable>[] retryFor = (Class<? extends Throwable>[]) attrs.get("value");
    String exceptionExpression = (String) attrs.get("exceptionExpression");
    boolean hasExceptionExpression = StringUtils.hasText(exceptionExpression);
    if (ObjectUtils.isEmpty(retryFor)) {
      retryFor = (Class<? extends Throwable>[]) attrs.get("retryFor");
    }
    @SuppressWarnings("unchecked")
    Class<? extends Throwable>[] noRetryFor = (Class<? extends Throwable>[]) attrs.get("noRetryFor");
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
    final Expression maxAttExpression = parsedExpression;
    SimpleRetryPolicy simple = null;
    if (retryFor.length == 0 && noRetryFor.length == 0) {
      simple = hasExceptionExpression
               ? new ExpressionRetryPolicy(resolve(exceptionExpression)).withBeanFactory(this.beanFactory)
               : new SimpleRetryPolicy();
      if (maxAttExpression != null) {
        simple.maxAttemptsSupplier(() -> evaluate(maxAttExpression, Integer.class, stateless));
      }
      else {
        simple.setMaxAttempts(maxAttempts);
      }
    }
    Map<Class<? extends Throwable>, Boolean> policyMap = new HashMap<>();
    for (Class<? extends Throwable> type : retryFor) {
      policyMap.put(type, true);
    }
    for (Class<? extends Throwable> type : noRetryFor) {
      policyMap.put(type, false);
    }
    boolean retryNotExcluded = retryFor.length == 0;
    if (simple == null) {
      if (hasExceptionExpression) {
        simple = new ExpressionRetryPolicy(maxAttempts, policyMap,
                true, resolve(exceptionExpression), retryNotExcluded)
                .withBeanFactory(this.beanFactory);
      }
      else {
        simple = new SimpleRetryPolicy(maxAttempts, policyMap, true, retryNotExcluded);
      }

      if (maxAttExpression != null) {
        simple.maxAttemptsSupplier(() -> evaluate(maxAttExpression, Integer.class, stateless));
      }
    }
    @SuppressWarnings("unchecked")
    Class<? extends Throwable>[] noRecovery = (Class<? extends Throwable>[]) attrs.get("notRecoverable");
    if (ObjectUtils.isNotEmpty(noRecovery)) {
      simple.setNotRecoverable(noRecovery);
    }
    return simple;
  }

  private BackOffPolicy getBackoffPolicy(Backoff backoff, boolean stateless) {
    long min = backoff.delay() == 0 ? backoff.value() : backoff.delay();
    String delayExpression = backoff.delayExpression();
    Expression parsedMinExp = null;
    if (StringUtils.hasText(delayExpression)) {
      parsedMinExp = parse(delayExpression);
      if (isTemplate(delayExpression)) {
        min = parsedMinExp.getValue(this.evaluationContext, Long.class);
        parsedMinExp = null;
      }
    }
    long max = backoff.maxDelay();
    String maxDelayExpression = backoff.maxDelayExpression();
    Expression parsedMaxExp = null;
    if (StringUtils.hasText(maxDelayExpression)) {
      parsedMaxExp = parse(maxDelayExpression);
      if (isTemplate(maxDelayExpression)) {
        max = parsedMaxExp.getValue(this.evaluationContext, Long.class);
        parsedMaxExp = null;
      }
    }
    double multiplier = backoff.multiplier();
    String multiplierExpression = backoff.multiplierExpression();
    Expression parsedMultExp = null;
    if (StringUtils.hasText(multiplierExpression)) {
      parsedMultExp = parse(multiplierExpression);
      if (isTemplate(multiplierExpression)) {
        multiplier = parsedMultExp.getValue(this.evaluationContext, Double.class);
        parsedMultExp = null;
      }
    }
    boolean isRandom = false;
    String randomExpression = backoff.randomExpression();
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
    return buildBackOff(min, parsedMinExp, max, parsedMaxExp, multiplier,
            parsedMultExp, isRandom, parsedRandomExp, stateless);
  }

  private BackOffPolicy buildBackOff(long min, Expression minExp, long max,
          Expression maxExp, double multiplier,
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
