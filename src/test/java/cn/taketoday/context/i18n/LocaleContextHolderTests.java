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

package cn.taketoday.context.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.TimeZone;

import cn.taketoday.core.i18n.LocaleContext;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.core.i18n.SimpleLocaleContext;
import cn.taketoday.core.i18n.SimpleTimeZoneAwareLocaleContext;
import cn.taketoday.core.i18n.TimeZoneAwareLocaleContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 22:57
 */
class LocaleContextHolderTests {

  @Test
  public void testSetLocaleContext() {
    LocaleContext lc = new SimpleLocaleContext(Locale.GERMAN);
    LocaleContextHolder.setLocaleContext(lc);
    assertThat(LocaleContextHolder.getLocaleContext()).isSameAs(lc);
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());

    lc = new SimpleLocaleContext(Locale.GERMANY);
    LocaleContextHolder.setLocaleContext(lc);
    assertThat(LocaleContextHolder.getLocaleContext()).isSameAs(lc);
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());

    LocaleContextHolder.resetLocaleContext();
    assertThat(LocaleContextHolder.getLocaleContext()).isNull();
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
  }

  @Test
  public void testSetTimeZoneAwareLocaleContext() {
    LocaleContext lc = new SimpleTimeZoneAwareLocaleContext(Locale.GERMANY, TimeZone.getTimeZone("GMT+1"));
    LocaleContextHolder.setLocaleContext(lc);
    assertThat(LocaleContextHolder.getLocaleContext()).isSameAs(lc);
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));

    LocaleContextHolder.resetLocaleContext();
    assertThat(LocaleContextHolder.getLocaleContext()).isNull();
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
  }

  @Test
  public void testSetLocale() {
    LocaleContextHolder.setLocale(Locale.GERMAN);
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
    boolean condition1 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
    assertThat(condition1).isFalse();
    assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMAN);

    LocaleContextHolder.setLocale(Locale.GERMANY);
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
    boolean condition = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isFalse();
    assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMANY);

    LocaleContextHolder.setLocale(null);
    assertThat(LocaleContextHolder.getLocaleContext()).isNull();
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());

    LocaleContextHolder.setDefaultLocale(Locale.GERMAN);
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
    LocaleContextHolder.setDefaultLocale(null);
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
  }

  @Test
  public void testSetTimeZone() {
    LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+1"));
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
    boolean condition1 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
    assertThat(condition1).isTrue();
    assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isNull();
    assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));

    LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+2"));
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
    boolean condition = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isNull();
    assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));

    LocaleContextHolder.setTimeZone(null);
    assertThat(LocaleContextHolder.getLocaleContext()).isNull();
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());

    LocaleContextHolder.setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
    LocaleContextHolder.setDefaultTimeZone(null);
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
  }

  @Test
  public void testSetLocaleAndSetTimeZoneMixed() {
    LocaleContextHolder.setLocale(Locale.GERMANY);
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
    boolean condition5 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
    assertThat(condition5).isFalse();
    assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMANY);

    LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+1"));
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
    boolean condition3 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
    assertThat(condition3).isTrue();
    assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMANY);
    assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));

    LocaleContextHolder.setLocale(Locale.GERMAN);
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
    boolean condition2 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
    assertThat(condition2).isTrue();
    assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMAN);
    assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));

    LocaleContextHolder.setTimeZone(null);
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
    boolean condition4 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
    assertThat(condition4).isFalse();
    assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMAN);

    LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+2"));
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
    boolean condition1 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
    assertThat(condition1).isTrue();
    assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMAN);
    assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));

    LocaleContextHolder.setLocale(null);
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
    boolean condition = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isNull();
    assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));

    LocaleContextHolder.setTimeZone(null);
    assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
    assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
    assertThat(LocaleContextHolder.getLocaleContext()).isNull();
  }

}
