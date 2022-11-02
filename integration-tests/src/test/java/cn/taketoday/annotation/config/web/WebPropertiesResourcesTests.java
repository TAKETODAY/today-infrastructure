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

  private final Resources properties = new WebProperties().getResources();

  @Test
  void resourceChainNoCustomization() {
    assertThat(this.properties.getChain().getEnabled()).isNull();
  }

  @Test
  void resourceChainStrategyEnabled() {
    this.properties.getChain().getStrategy().getFixed().setEnabled(true);
    assertThat(this.properties.getChain().getEnabled()).isTrue();
  }

  @Test
  void resourceChainEnabled() {
    this.properties.getChain().setEnabled(true);
    assertThat(this.properties.getChain().getEnabled()).isTrue();
  }

  @Test
  void resourceChainDisabled() {
    this.properties.getChain().setEnabled(false);
    assertThat(this.properties.getChain().getEnabled()).isFalse();
  }

  @Test
  void defaultStaticLocationsAllEndWithTrailingSlash() {
    assertThat(this.properties.getStaticLocations()).allMatch((location) -> location.endsWith("/"));
  }

  @Test
  void customStaticLocationsAreNormalizedToEndWithTrailingSlash() {
    this.properties.setStaticLocations(new String[] { "/foo", "/bar", "/baz/" });
    String[] actual = this.properties.getStaticLocations();
    assertThat(actual).containsExactly("/foo/", "/bar/", "/baz/");
  }

  @Test
  void emptyCacheControl() {
    CacheControl cacheControl = this.properties.getCache().getHttpCacheControl();
    assertThat(cacheControl).isNull();
  }

  @Test
  void cacheControlAllPropertiesSet() {
    Resources.Cache.Cachecontrol properties = this.properties.getCache().getCachecontrol();
    properties.setMaxAge(Duration.ofSeconds(4));
    properties.setCachePrivate(true);
    properties.setCachePublic(true);
    properties.setMustRevalidate(true);
    properties.setNoTransform(true);
    properties.setProxyRevalidate(true);
    properties.setSMaxAge(Duration.ofSeconds(5));
    properties.setStaleIfError(Duration.ofSeconds(6));
    properties.setStaleWhileRevalidate(Duration.ofSeconds(7));
    CacheControl cacheControl = this.properties.getCache().getHttpCacheControl();
    assertThat(cacheControl.getHeaderValue())
            .isEqualTo("max-age=4, must-revalidate, no-transform, public, private, proxy-revalidate,"
                    + " s-maxage=5, stale-if-error=6, stale-while-revalidate=7");
  }

  @Test
  void invalidCacheControlCombination() {
    Resources.Cache.Cachecontrol properties = this.properties.getCache().getCachecontrol();
    properties.setMaxAge(Duration.ofSeconds(4));
    properties.setNoStore(true);
    CacheControl cacheControl = this.properties.getCache().getHttpCacheControl();
    assertThat(cacheControl.getHeaderValue()).isEqualTo("no-store");
  }

}
