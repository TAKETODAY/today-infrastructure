/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
