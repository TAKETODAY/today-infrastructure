/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.client.reactive;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import infra.http.ResponseCookie;

/**
 * Parser that delegates to {@link java.net.HttpCookie#parse(String)} for parsing,
 * but also extracts and sets {@code sameSite}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class JdkResponseCookieParser implements ResponseCookie.Parser {

  private static final Pattern SAME_SITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");

  /**
   * Parse the given headers.
   */
  @Override
  public List<ResponseCookie> parse(String header) {
    Matcher matcher = SAME_SITE_PATTERN.matcher(header);
    String sameSite = matcher.matches() ? matcher.group(1) : null;
    List<HttpCookie> cookies = java.net.HttpCookie.parse(header);

    var result = new ArrayList<ResponseCookie>(cookies.size());
    for (HttpCookie cookie : cookies) {
      result.add(ResponseCookie.from(cookie).sameSite(sameSite).build());
    }
    return result;
  }

}
