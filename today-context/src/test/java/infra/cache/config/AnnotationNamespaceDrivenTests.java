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

package infra.cache.config;

import org.junit.jupiter.api.Test;

import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.CacheInterceptor;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.GenericXmlApplicationContext;
import infra.context.testfixture.cache.AbstractCacheAnnotationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Costin Leau
 * @author Chris Beams
 * @author Stephane Nicoll
 */
public class AnnotationNamespaceDrivenTests extends AbstractCacheAnnotationTests {

  @Override
  protected ConfigurableApplicationContext getApplicationContext() {
    return new GenericXmlApplicationContext(
            "infra/cache/config/annotationDrivenCacheNamespace.xml");
  }

  @Test
  public void testKeyStrategy() {
    CacheInterceptor ci = this.ctx.getBean(
            "infra.cache.interceptor.CacheInterceptor#0", CacheInterceptor.class);
    assertThat(ci.getKeyGenerator()).isSameAs(this.ctx.getBean("keyGenerator"));
  }

  @Test
  public void cacheResolver() {
    try {
      ConfigurableApplicationContext context = new GenericXmlApplicationContext(
              "infra/cache/config/annotationDrivenCacheNamespace-resolver.xml");

      CacheInterceptor ci = context.getBean(CacheInterceptor.class);
      assertThat(ci.getCacheResolver()).isSameAs(context.getBean("cacheResolver"));
      context.close();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }

  @Test
  public void bothSetOnlyResolverIsUsed() {
    ConfigurableApplicationContext context = new GenericXmlApplicationContext(
            "infra/cache/config/annotationDrivenCacheNamespace-manager-resolver.xml");

    CacheInterceptor ci = context.getBean(CacheInterceptor.class);
    assertThat(ci.getCacheResolver()).isSameAs(context.getBean("cacheResolver"));
    context.close();
  }

  @Test
  public void testCacheErrorHandler() {
    CacheInterceptor ci = this.ctx.getBean(
            "infra.cache.interceptor.CacheInterceptor#0", CacheInterceptor.class);
    assertThat(ci.getErrorHandler()).isSameAs(this.ctx.getBean("errorHandler", CacheErrorHandler.class));
  }

}
