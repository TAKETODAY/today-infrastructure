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
import javax.cache.annotation.CacheRemoveAll;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class CacheRemoveAllOperationTests extends AbstractCacheOperationTests<CacheRemoveAllOperation> {

  @Override
  protected CacheRemoveAllOperation createSimpleOperation() {
    CacheMethodDetails<CacheRemoveAll> methodDetails = create(CacheRemoveAll.class,
            SampleObject.class, "simpleRemoveAll");

    return new CacheRemoveAllOperation(methodDetails, defaultCacheResolver);
  }

  @Test
  public void simpleRemoveAll() {
    CacheRemoveAllOperation operation = createSimpleOperation();

    CacheInvocationParameter[] allParameters = operation.getAllParameters();
    assertThat(allParameters.length).isEqualTo(0);
  }

}
