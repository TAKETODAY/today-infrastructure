/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.config.web;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import infra.app.config.web.WebProperties.Resources;
import infra.http.CacheControl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Resources}.
 *
 * @author Stephane Nicoll
 * @author Kristine Jetzke
 */
class WebPropertiesResourcesTests {

  private final Resources properties = new infra.app.config.web.WebProperties().resources;

  @Test
  void resourceChainNoCustomization() {
    assertThat(this.properties.chain.getEnabled()).isNull();
  }

  @Test
  void resourceChainStrategyEnabled() {
    this.properties.chain.strategy.fixed.enabled = (true);
    assertThat(this.properties.chain.getEnabled()).isTrue();
  }

  @Test
  void resourceChainEnabled() {
    this.properties.chain.setEnabled(true);
    assertThat(this.properties.chain.getEnabled()).isTrue();
  }

  @Test
  void resourceChainDisabled() {
    this.properties.chain.setEnabled(false);
    assertThat(this.properties.chain.getEnabled()).isFalse();
  }

  @Test
  void defaultStaticLocationsAllEndWithTrailingSlash() {
    assertThat(this.properties.staticLocations).allMatch((location) -> location.endsWith("/"));
  }

  @Test
  void customStaticLocationsAreNormalizedToEndWithTrailingSlash() {
    this.properties.setStaticLocations(new String[] { "/foo", "/bar", "/baz/" });
    String[] actual = this.properties.staticLocations;
    assertThat(actual).containsExactly("/foo/", "/bar/", "/baz/");
  }

  @Test
  void emptyCacheControl() {
    CacheControl cacheControl = this.properties.cache.getHttpCacheControl();
    assertThat(cacheControl).isNull();
  }

  @Test
  void cacheControlAllPropertiesSet() {
    Resources.Cache.Cachecontrol properties = this.properties.cache.cachecontrol;
    properties.maxAge = (Duration.ofSeconds(4));
    properties.cachePrivate = (true);
    properties.cachePublic = (true);
    properties.mustRevalidate = (true);
    properties.noTransform = (true);
    properties.proxyRevalidate = (true);
    properties.sMaxAge = (Duration.ofSeconds(5));
    properties.staleIfError = (Duration.ofSeconds(6));
    properties.staleWhileRevalidate = (Duration.ofSeconds(7));
    CacheControl cacheControl = this.properties.cache.getHttpCacheControl();
    assertThat(cacheControl.getHeaderValue())
            .isEqualTo("max-age=4, must-revalidate, no-transform, public, private, proxy-revalidate,"
                    + " s-maxage=5, stale-if-error=6, stale-while-revalidate=7");
  }

  @Test
  void invalidCacheControlCombination() {
    Resources.Cache.Cachecontrol properties = this.properties.cache.cachecontrol;
    properties.maxAge = (Duration.ofSeconds(4));
    properties.noStore = (true);
    CacheControl cacheControl = this.properties.cache.getHttpCacheControl();
    assertThat(cacheControl.getHeaderValue()).isEqualTo("no-store");
  }

}
