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

package infra.cache.aspectj;

import org.junit.jupiter.api.Test;

import infra.cache.Cache;
import infra.cache.config.CacheableService;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.GenericXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Costin Leau
 */
public class AspectJCacheAnnotationTests extends AbstractCacheAnnotationTests {

  @Override
  protected ConfigurableApplicationContext getApplicationContext() {
    return new GenericXmlApplicationContext(
            "/infra/cache/config/annotation-cache-aspectj.xml");
  }

  @Test
  public void testKeyStrategy() {
    AnnotationCacheAspect aspect = ctx.getBean(
            "infra.cache.config.internalCacheAspect", AnnotationCacheAspect.class);
    assertThat(aspect.getKeyGenerator()).isSameAs(ctx.getBean("keyGenerator"));
  }

  @Override
  protected void testMultiEvict(CacheableService<?> service) {
    Object o1 = new Object();

    Object r1 = service.multiCache(o1);
    Object r2 = service.multiCache(o1);

    Cache primary = cm.getCache("primary");
    Cache secondary = cm.getCache("secondary");

    assertThat(r2).isSameAs(r1);
    assertThat(primary.get(o1).get()).isSameAs(r1);
    assertThat(secondary.get(o1).get()).isSameAs(r1);

    service.multiEvict(o1);
    assertThat(primary.get(o1)).isNull();
    assertThat(secondary.get(o1)).isNull();

    Object r3 = service.multiCache(o1);
    Object r4 = service.multiCache(o1);
    assertThat(r3).isNotSameAs(r1);
    assertThat(r4).isSameAs(r3);

    assertThat(primary.get(o1).get()).isSameAs(r3);
    assertThat(secondary.get(o1).get()).isSameAs(r4);
  }

}
