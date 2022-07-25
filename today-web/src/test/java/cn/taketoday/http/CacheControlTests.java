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

package cn.taketoday.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Brian Clozel
 * @author TODAY 2021/4/15 13:11
 * @since 3.0
 */
public class CacheControlTests {

  @Test
  public void emptyCacheControl() throws Exception {
    CacheControl cc = CacheControl.empty();
    assertThat(cc.getHeaderValue()).isNull();
  }

  @Test
  public void maxAge() throws Exception {
    CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS);
    assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600");
  }

  @Test
  public void maxAge_duration() throws Exception {
    CacheControl cc = CacheControl.maxAge(Duration.ofHours(1));
    assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600");
  }

  @Test
  public void maxAgeAndDirectives() throws Exception {
    CacheControl cc = CacheControl.maxAge(3600, TimeUnit.SECONDS).cachePublic().noTransform();
    assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, no-transform, public");
  }

  @Test
  public void maxAgeAndSMaxAge() throws Exception {
    CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).sMaxAge(30, TimeUnit.MINUTES);
    assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, s-maxage=1800");
  }

  @Test
  public void maxAgeAndSMaxAge_duration() throws Exception {
    CacheControl cc = CacheControl.maxAge(Duration.ofHours(1)).sMaxAge(Duration.ofMinutes(30));
    assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, s-maxage=1800");
  }

  @Test
  public void noCachePrivate() throws Exception {
    CacheControl cc = CacheControl.noCache().cachePrivate();
    assertThat(cc.getHeaderValue()).isEqualTo("no-cache, private");
  }

  @Test
  public void noStore() throws Exception {
    CacheControl cc = CacheControl.noStore();
    assertThat(cc.getHeaderValue()).isEqualTo("no-store");
  }

  @Test
  public void staleIfError() throws Exception {
    CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).staleIfError(2, TimeUnit.HOURS);
    assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, stale-if-error=7200");
  }

  @Test
  public void staleIfError_duration() throws Exception {
    CacheControl cc = CacheControl.maxAge(Duration.ofHours(1)).staleIfError(2, TimeUnit.HOURS);
    assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, stale-if-error=7200");
  }

  @Test
  public void staleWhileRevalidate() throws Exception {
    CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).staleWhileRevalidate(2, TimeUnit.HOURS);
    assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, stale-while-revalidate=7200");
  }

  @Test
  public void staleWhileRevalidate_duration() throws Exception {
    CacheControl cc = CacheControl.maxAge(Duration.ofHours(1)).staleWhileRevalidate(2, TimeUnit.HOURS);
    assertThat(cc.getHeaderValue()).isEqualTo("max-age=3600, stale-while-revalidate=7200");
  }

}
