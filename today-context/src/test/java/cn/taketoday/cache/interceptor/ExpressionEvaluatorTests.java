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

package cn.taketoday.cache.interceptor;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.cache.annotation.AnnotationCacheOperationSource;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.cache.annotation.Caching;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.context.expression.AnnotatedElementKey;
import cn.taketoday.context.expression.BeanFactoryResolver;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 */
public class ExpressionEvaluatorTests {

  private final StandardEvaluationContext originalEvaluationContext = new StandardEvaluationContext();

  private final CacheOperationExpressionEvaluator eval = new CacheOperationExpressionEvaluator(
          new CacheEvaluationContextFactory(this.originalEvaluationContext));

  private final AnnotationCacheOperationSource source = new AnnotationCacheOperationSource();

  private Collection<CacheOperation> getOps(String name) {
    Method method = ReflectionUtils.findMethod(AnnotatedClass.class, name, Object.class, Object.class);
    return this.source.getCacheOperations(method, AnnotatedClass.class);
  }

  @Test
  public void testMultipleCachingSource() {
    Collection<CacheOperation> ops = getOps("multipleCaching");
    assertThat(ops).hasSize(2);
    Iterator<CacheOperation> it = ops.iterator();
    CacheOperation next = it.next();
    assertThat(next).isInstanceOf(CacheableOperation.class);
    assertThat(next.getCacheNames().contains("test")).isTrue();
    assertThat(next.getKey()).isEqualTo("#a");
    next = it.next();
    assertThat(next).isInstanceOf(CacheableOperation.class);
    assertThat(next.getCacheNames().contains("test")).isTrue();
    assertThat(next.getKey()).isEqualTo("#b");
  }

  @Test
  public void testMultipleCachingEval() {
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
  public void withReturnValue() {
    EvaluationContext context = createEvaluationContext("theResult");
    Object value = new SpelExpressionParser().parseExpression("#result").getValue(context);
    assertThat(value).isEqualTo("theResult");
  }

  @Test
  public void withNullReturn() {
    EvaluationContext context = createEvaluationContext(null);
    Object value = new SpelExpressionParser().parseExpression("#result").getValue(context);
    assertThat(value).isNull();
  }

  @Test
  public void withoutReturnValue() {
    EvaluationContext context = createEvaluationContext(CacheOperationExpressionEvaluator.NO_RESULT);
    Object value = new SpelExpressionParser().parseExpression("#result").getValue(context);
    assertThat(value).isNull();
  }

  @Test
  public void unavailableReturnValue() {
    EvaluationContext context = createEvaluationContext(CacheOperationExpressionEvaluator.RESULT_UNAVAILABLE);
    assertThatExceptionOfType(VariableNotAvailableException.class).isThrownBy(() ->
                    new SpelExpressionParser().parseExpression("#result").getValue(context))
            .satisfies(ex -> assertThat(ex.getName()).isEqualTo("result"));
  }

  @Test
  public void resolveBeanReference() {
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
