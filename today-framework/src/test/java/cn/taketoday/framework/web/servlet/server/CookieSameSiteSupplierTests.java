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

package cn.taketoday.framework.web.servlet.server;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import cn.taketoday.session.config.SameSite;
import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link CookieSameSiteSupplier}.
 *
 * @author Phillip Webb
 */
class CookieSameSiteSupplierTests {

  @Test
  void whenHasNameWhenNameIsNullThrowsException() {
    CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
    assertThatIllegalArgumentException().isThrownBy(() -> supplier.whenHasName((String) null))
            .withMessage("Name must not be empty");
  }

  @Test
  void whenHasNameWhenNameIsEmptyThrowsException() {
    CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
    assertThatIllegalArgumentException().isThrownBy(() -> supplier.whenHasName(""))
            .withMessage("Name must not be empty");
  }

  @Test
  void whenHasNameWhenNameMatchesCallsGetSameSite() {
    CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
    assertThat(supplier.whenHasName("test").getSameSite(new Cookie("test", "x"))).isEqualTo(SameSite.LAX);
  }

  @Test
  void whenHasNameWhenNameDoesNotMatchDoesNotCallGetSameSite() {
    CookieSameSiteSupplier supplier = (cookie) -> fail("Supplier Called");
    assertThat(supplier.whenHasName("test").getSameSite(new Cookie("tset", "x"))).isNull();
  }

  @Test
  void whenHasSuppliedNameWhenNameIsNullThrowsException() {
    CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
    assertThatIllegalArgumentException().isThrownBy(() -> supplier.whenHasName((Supplier<String>) null))
            .withMessage("NameSupplier must not be empty");
  }

  @Test
  void whenHasSuppliedNameWhenNameMatchesCallsGetSameSite() {
    CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
    assertThat(supplier.whenHasName(() -> "test").getSameSite(new Cookie("test", "x"))).isEqualTo(SameSite.LAX);
  }

  @Test
  void whenHasSuppliedNameWhenNameDoesNotMatchDoesNotCallGetSameSite() {
    CookieSameSiteSupplier supplier = (cookie) -> fail("Supplier Called");
    assertThat(supplier.whenHasName(() -> "test").getSameSite(new Cookie("tset", "x"))).isNull();
  }

  @Test
  void whenHasNameMatchingRegexWhenRegexIsNullThrowsException() {
    CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
    assertThatIllegalArgumentException().isThrownBy(() -> supplier.whenHasNameMatching((String) null))
            .withMessage("Regex must not be empty");
  }

  @Test
  void whenHasNameMatchingRegexWhenRegexIsEmptyThrowsException() {
    CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
    assertThatIllegalArgumentException().isThrownBy(() -> supplier.whenHasNameMatching(""))
            .withMessage("Regex must not be empty");
  }

  @Test
  void whenHasNameMatchingRegexWhenNameMatchesCallsGetSameSite() {
    CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
    assertThat(supplier.whenHasNameMatching("te.*").getSameSite(new Cookie("test", "x"))).isEqualTo(SameSite.LAX);
  }

  @Test
  void whenHasNameMatchingRegexWhenNameDoesNotMatchDoesNotCallGetSameSite() {
    CookieSameSiteSupplier supplier = (cookie) -> fail("Supplier Called");
    assertThat(supplier.whenHasNameMatching("te.*").getSameSite(new Cookie("tset", "x"))).isNull();
  }

  @Test
  void whenHasNameMatchingPatternWhenPatternIsNullThrowsException() {
    CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
    assertThatIllegalArgumentException().isThrownBy(() -> supplier.whenHasNameMatching((Pattern) null))
            .withMessage("Pattern must not be null");
  }

  @Test
  void whenHasNameMatchingPatternWhenNameMatchesCallsGetSameSite() {
    CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
    assertThat(supplier.whenHasNameMatching(Pattern.compile("te.*")).getSameSite(new Cookie("test", "x")))
            .isEqualTo(SameSite.LAX);
  }

  @Test
  void whenHasNameMatchingPatternWhenNameDoesNotMatchDoesNotCallGetSameSite() {
    CookieSameSiteSupplier supplier = (cookie) -> fail("Supplier Called");
    assertThat(supplier.whenHasNameMatching(Pattern.compile("te.*")).getSameSite(new Cookie("tset", "x"))).isNull();
  }

  @Test
  void whenWhenPredicateIsNullThrowsException() {
    CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
    assertThatIllegalArgumentException().isThrownBy(() -> supplier.when(null))
            .withMessage("Predicate must not be null");
  }

  @Test
  void whenWhenPredicateMatchesCallsGetSameSite() {
    CookieSameSiteSupplier supplier = (cookie) -> SameSite.LAX;
    assertThat(supplier.when((cookie) -> cookie.getName().equals("test")).getSameSite(new Cookie("test", "x")))
            .isEqualTo(SameSite.LAX);
  }

  @Test
  void whenWhenPredicateDoesNotMatchDoesNotCallGetSameSite() {
    CookieSameSiteSupplier supplier = (cookie) -> fail("Supplier Called");
    assertThat(supplier.when((cookie) -> cookie.getName().equals("test")).getSameSite(new Cookie("tset", "x")))
            .isNull();
  }

  @Test
  void ofNoneSuppliesNone() {
    assertThat(CookieSameSiteSupplier.ofNone().getSameSite(new Cookie("test", "x"))).isEqualTo(SameSite.NONE);
  }

  @Test
  void ofLaxSuppliesLax() {
    assertThat(CookieSameSiteSupplier.ofLax().getSameSite(new Cookie("test", "x"))).isEqualTo(SameSite.LAX);
  }

  @Test
  void ofStrictSuppliesStrict() {
    assertThat(CookieSameSiteSupplier.ofStrict().getSameSite(new Cookie("test", "x"))).isEqualTo(SameSite.STRICT);
  }

  @Test
  void ofWhenNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> CookieSameSiteSupplier.of(null))
            .withMessage("SameSite must not be null");
  }

  @Test
  void ofSuppliesValue() {
    assertThat(CookieSameSiteSupplier.of(SameSite.STRICT).getSameSite(new Cookie("test", "x")))
            .isEqualTo(SameSite.STRICT);
  }

}
