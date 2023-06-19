/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.cache.config;

import org.junit.jupiter.api.Test;

import cn.taketoday.cache.interceptor.CacheInterceptor;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.GenericXmlApplicationContext;
import cn.taketoday.context.testfixture.cache.AbstractCacheAnnotationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Costin Leau
 * @author Chris Beams
 */
public class CacheAdviceNamespaceTests extends AbstractCacheAnnotationTests {

  @Override
  protected ConfigurableApplicationContext getApplicationContext() {
    return new GenericXmlApplicationContext(
            "cn/taketoday/cache/config/cache-advice.xml");
  }

  @Test
  public void testKeyStrategy() {
    CacheInterceptor bean = this.ctx.getBean("cacheAdviceClass", CacheInterceptor.class);
    assertThat(bean.getKeyGenerator()).isSameAs(this.ctx.getBean("keyGenerator"));
  }

}
