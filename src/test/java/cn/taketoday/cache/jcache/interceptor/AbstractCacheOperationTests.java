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

package cn.taketoday.cache.jcache.interceptor;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;

import cn.taketoday.cache.jcache.AbstractJCacheTests;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractCacheOperationTests<O extends JCacheOperation<?>> extends AbstractJCacheTests {

  protected final SampleObject sampleInstance = new SampleObject();

  protected abstract O createSimpleOperation();

  @Test
  public void simple() {
    O operation = createSimpleOperation();
    assertThat(operation.getCacheName()).as("Wrong cache name").isEqualTo("simpleCache");
    assertThat(operation.getAnnotations().size()).as("Unexpected number of annotation on " + operation.getMethod()).isEqualTo(1);
    assertThat(operation.getAnnotations().iterator().next()).as("Wrong method annotation").isEqualTo(operation.getCacheAnnotation());

    assertThat(operation.getCacheResolver()).as("cache resolver should be set").isNotNull();
  }

  protected void assertCacheInvocationParameter(CacheInvocationParameter actual, Class<?> targetType,
          Object value, int position) {
    assertThat(actual.getRawType()).as("wrong parameter type for " + actual).isEqualTo(targetType);
    assertThat(actual.getValue()).as("wrong parameter value for " + actual).isEqualTo(value);
    assertThat(actual.getParameterPosition()).as("wrong parameter position for " + actual).isEqualTo(position);
  }

  protected <A extends Annotation> CacheMethodDetails<A> create(Class<A> annotationType,
          Class<?> targetType, String methodName,
          Class<?>... parameterTypes) {
    Method method = ReflectionUtils.findMethod(targetType, methodName, parameterTypes);
    Assert.notNull(method, "requested method '" + methodName + "'does not exist");
    A cacheAnnotation = method.getAnnotation(annotationType);
    return new DefaultCacheMethodDetails<>(method, cacheAnnotation, getCacheName(cacheAnnotation));
  }

  private static String getCacheName(Annotation annotation) {
    Object cacheName = AnnotationUtils.getValue(annotation, "cacheName");
    return cacheName != null ? cacheName.toString() : "test";
  }

}
