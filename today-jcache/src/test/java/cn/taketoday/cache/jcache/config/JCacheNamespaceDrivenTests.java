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

package cn.taketoday.cache.jcache.config;

import org.junit.jupiter.api.Test;

import cn.taketoday.cache.interceptor.CacheErrorHandler;
import cn.taketoday.cache.jcache.interceptor.DefaultJCacheOperationSource;
import cn.taketoday.cache.jcache.interceptor.JCacheInterceptor;
import cn.taketoday.cache.jcache.testfixture.jcache.AbstractJCacheAnnotationTests;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.GenericXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class JCacheNamespaceDrivenTests extends AbstractJCacheAnnotationTests {

  @Override
  protected ApplicationContext getApplicationContext() {
    return new GenericXmlApplicationContext(
            "cn/taketoday/cache/jcache/config/jCacheNamespaceDriven.xml");
  }

  @Test
  public void cacheResolver() {
    ConfigurableApplicationContext context = new GenericXmlApplicationContext(
            "cn/taketoday/cache/jcache/config/jCacheNamespaceDriven-resolver.xml");

    DefaultJCacheOperationSource ci = context.getBean(DefaultJCacheOperationSource.class);
    assertThat(ci.getCacheResolver()).isSameAs(context.getBean("cacheResolver"));
    context.close();
  }

  @Test
  public void testCacheErrorHandler() {
    JCacheInterceptor ci = ctx.getBean(JCacheInterceptor.class);
    assertThat(ci.getErrorHandler()).isSameAs(ctx.getBean("errorHandler", CacheErrorHandler.class));
  }

}
