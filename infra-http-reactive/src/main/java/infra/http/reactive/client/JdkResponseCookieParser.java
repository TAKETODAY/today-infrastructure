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

package infra.http.reactive.client;

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
