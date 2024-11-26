/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.cache.jcache.config;

import org.junit.jupiter.api.Test;

import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.jcache.interceptor.DefaultJCacheOperationSource;
import infra.cache.jcache.interceptor.JCacheInterceptor;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.GenericXmlApplicationContext;
import infra.contextsupport.testfixture.jcache.AbstractJCacheAnnotationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class JCacheNamespaceDrivenTests extends AbstractJCacheAnnotationTests {

  @Override
  protected ApplicationContext getApplicationContext() {
    return new GenericXmlApplicationContext(
            "infra/cache/jcache/config/jCacheNamespaceDriven.xml");
  }

  @Test
  public void cacheResolver() {
    ConfigurableApplicationContext context = new GenericXmlApplicationContext(
            "infra/cache/jcache/config/jCacheNamespaceDriven-resolver.xml");

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
