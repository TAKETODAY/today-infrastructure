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
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.expression.ExpressionProcessor;
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

  private final CacheOperationExpressionEvaluator eval = new CacheOperationExpressionEvaluator();

  private final AnnotationCacheOperationSource source = new AnnotationCacheOperationSource();

  private Collection<CacheOperation> getOps(String name) {
    Method method = ReflectionUtils.findMethod(AnnotatedClass.class, name, Object.class, Object.class);
    return this.source.getCacheOperations(method, AnnotatedClass.class);
  }

  @Test
  public void testMultipleCachingSource() {
    Collection<CacheOperation> ops = getOps("multipleCaching");
    assertThat(ops.size()).isEqualTo(2);
    Iterator<CacheOperation> it = ops.iterator();
    CacheOperation next = it.next();
    assertThat(next instanceof CacheableOperation).isTrue();
    assertThat(next.getCacheNames().contains("test")).isTrue();
    assertThat(next.getKey()).isEqualTo("#a");
    next = it.next();
    assertThat(next instanceof CacheableOperation).isTrue();
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

    CacheEvaluationContext evalCtx = this.eval.createEvaluationContext(caches, method, args,
            target, target.getClass(), method, CacheOperationExpressionEvaluator.NO_RESULT, null);
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
    CacheEvaluationContext context = createEvaluationContext("theResult");
    Object value = ExpressionProcessor.getSharedInstance().getValue("#{result}", context, Object.class);
    assertThat(value).isEqualTo("theResult");
  }

  @Test
  public void withNullReturn() {
    CacheEvaluationContext context = createEvaluationContext(null);
    Object value = ExpressionProcessor.getSharedInstance().getValue("#{result}", context, Object.class);
    assertThat(value).isNull();
  }

  @Test
  public void withoutReturnValue() {
    CacheEvaluationContext context = createEvaluationContext(CacheOperationExpressionEvaluator.NO_RESULT);
    context.setReturnEmptyWhenPropertyNotResolved(true);
    Object value = ExpressionProcessor.getSharedInstance().getValue("#{result}", context, Object.class);
    assertThat(value).isNull();
  }

  @Test
  public void unavailableReturnValue() {
    CacheEvaluationContext context = createEvaluationContext(CacheOperationExpressionEvaluator.RESULT_UNAVAILABLE);
    assertThatExceptionOfType(VariableNotAvailableException.class).isThrownBy(() ->
                    ExpressionProcessor.getSharedInstance().getValue("#{result}", context, Object.class)
            )
            .satisfies(ex -> assertThat(ex.getName()).isEqualTo("result"));
  }

  @Test
  public void resolveBeanReference() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    BeanDefinition beanDefinition = new RootBeanDefinition(String.class);
    applicationContext.registerBeanDefinition("myBean", beanDefinition);
    applicationContext.refresh();

    CacheEvaluationContext context = createEvaluationContext(CacheOperationExpressionEvaluator.NO_RESULT, applicationContext);
    Object value = ExpressionProcessor.getSharedInstance().getValue("#{myBean.class.getName()}", context, Object.class);
    assertThat(value).isEqualTo(String.class.getName());
  }

  private CacheEvaluationContext createEvaluationContext(Object result) {
    return createEvaluationContext(result, null);
  }

  private CacheEvaluationContext createEvaluationContext(Object result, BeanFactory beanFactory) {
    AnnotatedClass target = new AnnotatedClass();
    Method method = ReflectionUtils.findMethod(
            AnnotatedClass.class, "multipleCaching", Object.class, Object.class);
    Object[] args = new Object[] { new Object(), new Object() };
    Collection<ConcurrentMapCache> caches = Collections.singleton(new ConcurrentMapCache("test"));
    return this.eval.createEvaluationContext(
            caches, method, args, target, target.getClass(), method, result, beanFactory);
  }

  private static class AnnotatedClass {

    @Caching(cacheable = { @Cacheable(value = "test", key = "a"), @Cacheable(value = "test", key = "b") })
    public void multipleCaching(Object a, Object b) {
    }
  }

}
