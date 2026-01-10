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
