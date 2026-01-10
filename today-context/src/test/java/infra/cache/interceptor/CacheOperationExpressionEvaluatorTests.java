/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.cache.interceptor;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.cache.annotation.AnnotationCacheOperationSource;
import infra.cache.annotation.Cacheable;
import infra.cache.annotation.Caching;
import infra.cache.concurrent.ConcurrentMapCache;
import infra.context.expression.AnnotatedElementKey;
import infra.context.expression.BeanFactoryResolver;
import infra.context.support.StaticApplicationContext;
import infra.expression.EvaluationContext;
import infra.expression.spel.standard.SpelExpressionParser;
import infra.expression.spel.support.StandardEvaluationContext;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link CacheOperationExpressionEvaluator}.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 */
class CacheOperationExpressionEvaluatorTests {

  private final StandardEvaluationContext originalEvaluationContext = new StandardEvaluationContext();

  private final CacheOperationExpressionEvaluator eval = new CacheOperationExpressionEvaluator(
          originalEvaluationContext);

  private final AnnotationCacheOperationSource source = new AnnotationCacheOperationSource();

  private Collection<CacheOperation> getOps(String name) {
    Method method = ReflectionUtils.findMethod(AnnotatedClass.class, name, Object.class, Object.class);
    return this.source.getCacheOperations(method, AnnotatedClass.class);
  }

  @Test
  void testMultipleCachingSource() {
    Collection<CacheOperation> ops = getOps("multipleCaching");
    assertThat(ops).hasSize(2);
    Iterator<CacheOperation> it = ops.iterator();
    CacheOperation next = it.next();
    assertThat(next).isInstanceOf(CacheableOperation.class);
    assertThat(next.getCacheNames()).contains("test");
    assertThat(next.getKey()).isEqualTo("#a");
    next = it.next();
    assertThat(next).isInstanceOf(CacheableOperation.class);
    assertThat(next.getCacheNames()).contains("test");
    assertThat(next.getKey()).isEqualTo("#b");
  }

  @Test
  void testMultipleCachingEval() {
    AnnotatedClass target = new AnnotatedClass();
    Method method = ReflectionUtils.findMethod(
            AnnotatedClass.class, "multipleCaching", Object.class, Object.class);
    Object[] args = new Object[] { new Object(), new Object() };
    Collection<ConcurrentMapCache> caches = Collections.singleton(new ConcurrentMapCache("test"));

    EvaluationContext evalCtx = this.eval.createEvaluationContext(caches, method, args,
            target, target.getClass(), method, CacheOperationExpressionEvaluator.NO_RESULT);
    Collection<CacheOperation> ops = getOps("multipleCaching");

    Iterator<CacheOperation> it = ops.iterator();
    AnnotatedElementKey key = new AnnotatedElementKey(method, AnnotatedClass.class);

    Object keyA = this.eval.key(it.next().getKey(), key, evalCtx);
    Object keyB = this.eval.key(it.next().getKey(), key, evalCtx);

    assertThat(keyA).isEqualTo(args[0]);
    assertThat(keyB).isEqualTo(args[1]);
  }

  @Test
  void withReturnValue() {
    EvaluationContext context = createEvaluationContext("theResult");
    Object value = new SpelExpressionParser().parseExpression("#result").getValue(context);
    assertThat(value).isEqualTo("theResult");
  }

  @Test
  void withNullReturn() {
    EvaluationContext context = createEvaluationContext(null);
    Object value = new SpelExpressionParser().parseExpression("#result").getValue(context);
    assertThat(value).isNull();
  }

  @Test
  void withoutReturnValue() {
    EvaluationContext context = createEvaluationContext(CacheOperationExpressionEvaluator.NO_RESULT);
    Object value = new SpelExpressionParser().parseExpression("#result").getValue(context);
    assertThat(value).isNull();
  }

  @Test
  void unavailableReturnValue() {
    EvaluationContext context = createEvaluationContext(CacheOperationExpressionEvaluator.RESULT_UNAVAILABLE);
    assertThatExceptionOfType(VariableNotAvailableException.class)
            .isThrownBy(() -> new SpelExpressionParser().parseExpression("#result").getValue(context))
            .withMessage("Variable 'result' not available");
  }

  @Test
  void resolveBeanReference() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    BeanDefinition beanDefinition = new RootBeanDefinition(String.class);
    applicationContext.registerBeanDefinition("myBean", beanDefinition);
    applicationContext.refresh();

    EvaluationContext context = createEvaluationContext(CacheOperationExpressionEvaluator.NO_RESULT, applicationContext);
    Object value = new SpelExpressionParser().parseExpression("@myBean.class.getName()").getValue(context);
    assertThat(value).isEqualTo(String.class.getName());
  }

  private EvaluationContext createEvaluationContext(Object result) {
    return createEvaluationContext(result, null);
  }

  private EvaluationContext createEvaluationContext(Object result, @Nullable BeanFactory beanFactory) {
    if (beanFactory != null) {
      this.originalEvaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
    }
    AnnotatedClass target = new AnnotatedClass();
    Method method = ReflectionUtils.findMethod(
            AnnotatedClass.class, "multipleCaching", Object.class, Object.class);
    Object[] args = new Object[] { new Object(), new Object() };
    Collection<ConcurrentMapCache> caches = Collections.singleton(new ConcurrentMapCache("test"));
    return this.eval.createEvaluationContext(
            caches, method, args, target, target.getClass(), method, result);
  }

  private static class AnnotatedClass {

    @Caching(cacheable = { @Cacheable(value = "test", key = "#a"), @Cacheable(value = "test", key = "#b") })
    public void multipleCaching(Object a, Object b) {
    }
  }

}
