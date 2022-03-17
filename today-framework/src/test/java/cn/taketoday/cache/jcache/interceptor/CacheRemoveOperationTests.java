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

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemove;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class CacheRemoveOperationTests extends AbstractCacheOperationTests<CacheRemoveOperation> {

  @Override
  protected CacheRemoveOperation createSimpleOperation() {
    CacheMethodDetails<CacheRemove> methodDetails = create(CacheRemove.class,
            SampleObject.class, "simpleRemove", Long.class);

    return new CacheRemoveOperation(methodDetails, defaultCacheResolver, defaultKeyGenerator);
  }

  @Test
  public void simpleRemove() {
    CacheRemoveOperation operation = createSimpleOperation();

    CacheInvocationParameter[] allParameters = operation.getAllParameters(2L);
    assertThat(allParameters.length).isEqualTo(1);
    assertCacheInvocationParameter(allParameters[0], Long.class, 2L, 0);
  }

}
