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

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import cn.taketoday.session.config.SameSite;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import jakarta.servlet.http.Cookie;

/**
 * Strategy interface that can be used with {@link ConfigurableServletWebServerFactory}
 * implementations in order to supply custom {@link SameSite} values for specific
 * {@link Cookie cookies}.
 * <p>
 * Basic CookieSameSiteSupplier implementations can be constructed using the {@code of...}
 * factory methods, typically combined with name matching. For example: <pre class="code">
 * CookieSameSiteSupplier.ofLax().whenHasName("mycookie");
 * </pre>
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurableServletWebServerFactory#addCookieSameSiteSuppliers(CookieSameSiteSupplier...)
 * @since 4.0
 */
@FunctionalInterface
public interface CookieSameSiteSupplier {

  /**
   * Get the {@link SameSite} values that should be used for the given {@link Cookie}.
   *
   * @param cookie the cookie to check
   * @return the {@link SameSite} value to use or {@code null} if the next supplier
   * should be checked
   */
  @Nullable
  SameSite getSameSite(Cookie cookie);

  /**
   * Limit this supplier so that it's only called if the Cookie has the given name.
   *
   * @param name the name to check
   * @return a new {@link CookieSameSiteSupplier} that only calls this supplier when the
   * name matches
   */
  default CookieSameSiteSupplier whenHasName(String name) {
    Assert.hasText(name, "Name must not be empty");
    return when(cookie -> ObjectUtils.nullSafeEquals(cookie.getName(), name));
  }

  /**
   * Limit this supplier so that it's only called if the Cookie has the given name.
   *
   * @param nameSupplier a supplier providing the name to check
   * @return a new {@link CookieSameSiteSupplier} that only calls this supplier when the
   * name matches
   */
  default CookieSameSiteSupplier whenHasName(Supplier<String> nameSupplier) {
    Assert.notNull(nameSupplier, "NameSupplier must not be empty");
    return when(cookie -> ObjectUtils.nullSafeEquals(cookie.getName(), nameSupplier.get()));
  }

  /**
   * Limit this supplier so that it's only called if the Cookie name matches the given
   * regex.
   *
   * @param regex the regex pattern that must match
   * @return a new {@link CookieSameSiteSupplier} that only calls this supplier when the
   * name matches the regex
   */
  default CookieSameSiteSupplier whenHasNameMatching(String regex) {
    Assert.hasText(regex, "Regex must not be empty");
    return whenHasNameMatching(Pattern.compile(regex));
  }

  /**
   * Limit this supplier so that it's only called if the Cookie name matches the given
   * {@link Pattern}.
   *
   * @param pattern the regex pattern that must match
   * @return a new {@link CookieSameSiteSupplier} that only calls this supplier when the
   * name matches the pattern
   */
  default CookieSameSiteSupplier whenHasNameMatching(Pattern pattern) {
    Assert.notNull(pattern, "Pattern is required");
    return when(cookie -> pattern.matcher(cookie.getName()).matches());
  }

  /**
   * Limit this supplier so that it's only called if the predicate accepts the Cookie.
   *
   * @param predicate the predicate used to match the cookie
   * @return a new {@link CookieSameSiteSupplier} that only calls this supplier when the
   * cookie matches the predicate
   */
  default CookieSameSiteSupplier when(Predicate<Cookie> predicate) {
    Assert.notNull(predicate, "Predicate is required");
    return cookie -> predicate.test(cookie) ? getSameSite(cookie) : null;
  }

  /**
   * Return a new {@link CookieSameSiteSupplier} that always returns
   * {@link SameSite#NONE}.
   *
   * @return the supplier instance
   */
  static CookieSameSiteSupplier ofNone() {
    return of(SameSite.NONE);
  }

  /**
   * Return a new {@link CookieSameSiteSupplier} that always returns
   * {@link SameSite#LAX}.
   *
   * @return the supplier instance
   */
  static CookieSameSiteSupplier ofLax() {
    return of(SameSite.LAX);
  }

  /**
   * Return a new {@link CookieSameSiteSupplier} that always returns
   * {@link SameSite#STRICT}.
   *
   * @return the supplier instance
   */
  static CookieSameSiteSupplier ofStrict() {
    return of(SameSite.STRICT);
  }

  /**
   * Return a new {@link CookieSameSiteSupplier} that always returns the given
   * {@link SameSite} value.
   *
   * @param sameSite the value to return
   * @return the supplier instance
   */
  static CookieSameSiteSupplier of(SameSite sameSite) {
    Assert.notNull(sameSite, "SameSite is required");
    return cookie -> sameSite;
  }

}
