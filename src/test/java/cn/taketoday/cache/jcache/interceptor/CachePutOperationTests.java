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

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CachePut;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Stephane Nicoll
 */
public class CachePutOperationTests extends AbstractCacheOperationTests<CachePutOperation> {

  @Override
  protected CachePutOperation createSimpleOperation() {
    CacheMethodDetails<CachePut> methodDetails = create(CachePut.class,
            SampleObject.class, "simplePut", Long.class, SampleObject.class);
    return createDefaultOperation(methodDetails);
  }

  @Test
  public void simplePut() {
    CachePutOperation operation = createSimpleOperation();

    CacheInvocationParameter[] allParameters = operation.getAllParameters(2L, sampleInstance);
    assertThat(allParameters.length).isEqualTo(2);
    assertCacheInvocationParameter(allParameters[0], Long.class, 2L, 0);
    assertCacheInvocationParameter(allParameters[1], SampleObject.class, sampleInstance, 1);

    CacheInvocationParameter valueParameter = operation.getValueParameter(2L, sampleInstance);
    assertThat(valueParameter).isNotNull();
    assertCacheInvocationParameter(valueParameter, SampleObject.class, sampleInstance, 1);
  }

  @Test
  public void noCacheValue() {
    CacheMethodDetails<CachePut> methodDetails = create(CachePut.class,
            SampleObject.class, "noCacheValue", Long.class);

    assertThatIllegalArgumentException().isThrownBy(() ->
            createDefaultOperation(methodDetails));
  }

  @Test
  public void multiCacheValues() {
    CacheMethodDetails<CachePut> methodDetails = create(CachePut.class,
            SampleObject.class, "multiCacheValues", Long.class, SampleObject.class, SampleObject.class);

    assertThatIllegalArgumentException().isThrownBy(() ->
            createDefaultOperation(methodDetails));
  }

  @Test
  public void invokeWithWrongParameters() {
    CachePutOperation operation = createSimpleOperation();

    assertThatIllegalStateException().isThrownBy(() ->
            operation.getValueParameter(2L));
  }

  @Test
  public void fullPutConfig() {
    CacheMethodDetails<CachePut> methodDetails = create(CachePut.class,
            SampleObject.class, "fullPutConfig", Long.class, SampleObject.class);
    CachePutOperation operation = createDefaultOperation(methodDetails);
    assertThat(operation.isEarlyPut()).isTrue();
    assertThat(operation.getExceptionTypeFilter()).isNotNull();
    assertThat(operation.getExceptionTypeFilter().match(IOException.class)).isTrue();
    assertThat(operation.getExceptionTypeFilter().match(NullPointerException.class)).isFalse();
  }

  private CachePutOperation createDefaultOperation(CacheMethodDetails<CachePut> methodDetails) {
    return new CachePutOperation(methodDetails, defaultCacheResolver, defaultKeyGenerator);
  }

}
