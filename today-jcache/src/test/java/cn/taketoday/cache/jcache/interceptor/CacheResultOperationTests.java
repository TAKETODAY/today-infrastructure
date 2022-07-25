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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResult;

import cn.taketoday.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Stephane Nicoll
 */
public class CacheResultOperationTests extends AbstractCacheOperationTests<CacheResultOperation> {

  @Override
  protected CacheResultOperation createSimpleOperation() {
    CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
            SampleObject.class, "simpleGet", Long.class);

    return new CacheResultOperation(methodDetails, defaultCacheResolver, defaultKeyGenerator,
            defaultExceptionCacheResolver);
  }

  @Test
  public void simpleGet() {
    CacheResultOperation operation = createSimpleOperation();

    assertThat(operation.getKeyGenerator()).isNotNull();
    assertThat(operation.getExceptionCacheResolver()).isNotNull();

    assertThat(operation.getExceptionCacheName()).isNull();
    assertThat(operation.getExceptionCacheResolver()).isEqualTo(defaultExceptionCacheResolver);

    CacheInvocationParameter[] allParameters = operation.getAllParameters(2L);
    assertThat(allParameters.length).isEqualTo(1);
    assertCacheInvocationParameter(allParameters[0], Long.class, 2L, 0);

    CacheInvocationParameter[] keyParameters = operation.getKeyParameters(2L);
    assertThat(keyParameters.length).isEqualTo(1);
    assertCacheInvocationParameter(keyParameters[0], Long.class, 2L, 0);
  }

  @Test
  public void multiParameterKey() {
    CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
            SampleObject.class, "multiKeysGet", Long.class, Boolean.class, String.class);
    CacheResultOperation operation = createDefaultOperation(methodDetails);

    CacheInvocationParameter[] keyParameters = operation.getKeyParameters(3L, Boolean.TRUE, "Foo");
    assertThat(keyParameters.length).isEqualTo(2);
    assertCacheInvocationParameter(keyParameters[0], Long.class, 3L, 0);
    assertCacheInvocationParameter(keyParameters[1], String.class, "Foo", 2);
  }

  @Test
  public void invokeWithWrongParameters() {
    CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
            SampleObject.class, "anotherSimpleGet", String.class, Long.class);
    CacheResultOperation operation = createDefaultOperation(methodDetails);

    // missing one argument
    assertThatIllegalStateException().isThrownBy(() ->
            operation.getAllParameters("bar"));
  }

  @Test
  public void tooManyKeyValues() {
    CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
            SampleObject.class, "anotherSimpleGet", String.class, Long.class);
    CacheResultOperation operation = createDefaultOperation(methodDetails);

    // missing one argument
    assertThatIllegalStateException().isThrownBy(() ->
            operation.getKeyParameters("bar"));
  }

  @Test
  public void annotatedGet() {
    CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
            SampleObject.class, "annotatedGet", Long.class, String.class);
    CacheResultOperation operation = createDefaultOperation(methodDetails);
    CacheInvocationParameter[] parameters = operation.getAllParameters(2L, "foo");

    Set<Annotation> firstParameterAnnotations = parameters[0].getAnnotations();
    assertThat(firstParameterAnnotations.size()).isEqualTo(1);
    assertThat(firstParameterAnnotations.iterator().next().annotationType()).isEqualTo(CacheKey.class);

    Set<Annotation> secondParameterAnnotations = parameters[1].getAnnotations();
    assertThat(secondParameterAnnotations.size()).isEqualTo(1);
    assertThat(secondParameterAnnotations.iterator().next().annotationType()).isEqualTo(Value.class);
  }

  @Test
  public void fullGetConfig() {
    CacheMethodDetails<CacheResult> methodDetails = create(CacheResult.class,
            SampleObject.class, "fullGetConfig", Long.class);
    CacheResultOperation operation = createDefaultOperation(methodDetails);
    assertThat(operation.isAlwaysInvoked()).isTrue();
    assertThat(operation.getExceptionTypeFilter()).isNotNull();
    assertThat(operation.getExceptionTypeFilter().match(IOException.class)).isTrue();
    assertThat(operation.getExceptionTypeFilter().match(NullPointerException.class)).isFalse();
  }

  private CacheResultOperation createDefaultOperation(CacheMethodDetails<CacheResult> methodDetails) {
    return new CacheResultOperation(methodDetails,
            defaultCacheResolver, defaultKeyGenerator, defaultCacheResolver);
  }

}
