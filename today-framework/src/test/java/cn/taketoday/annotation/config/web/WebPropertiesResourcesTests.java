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

package cn.taketoday.annotation.config.web;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import cn.taketoday.annotation.config.web.WebProperties.Resources;
import cn.taketoday.http.CacheControl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Resources}.
 *
 * @author Stephane Nicoll
 * @author Kristine Jetzke
 */
class WebPropertiesResourcesTests {

  private final Resources properties = new WebProperties().resources;

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
