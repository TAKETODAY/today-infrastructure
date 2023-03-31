/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.servlet.htmlunit;

import com.gargoylesoftware.htmlunit.WebRequest;

import java.util.regex.Pattern;

/**
 * A {@link WebRequestMatcher} that allows matching on
 * {@code WebRequest#getUrl().toExternalForm()} using a regular expression.
 *
 * <p>For example, if you would like to match on the domain {@code code.jquery.com},
 * you might want to use the following.
 *
 * <pre class="code">
 * WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
 * </pre>
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @see cn.taketoday.test.web.servlet.htmlunit.DelegatingWebConnection
 * @since 4.0
 */
public final class UrlRegexRequestMatcher implements WebRequestMatcher {

  private final Pattern pattern;

  public UrlRegexRequestMatcher(String regex) {
    this.pattern = Pattern.compile(regex);
  }

  public UrlRegexRequestMatcher(Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  public boolean matches(WebRequest request) {
    String url = request.getUrl().toExternalForm();
    return this.pattern.matcher(url).matches();
  }

}
