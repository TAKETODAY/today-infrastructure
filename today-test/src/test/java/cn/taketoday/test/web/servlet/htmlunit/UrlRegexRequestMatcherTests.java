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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UrlRegexRequestMatcher}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.0
 */
public class UrlRegexRequestMatcherTests extends AbstractWebRequestMatcherTests {

  @Test
  public void verifyExampleInClassLevelJavadoc() throws Exception {
    WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
    assertMatches(cdnMatcher, "https://code.jquery.com/jquery-1.11.0.min.js");
    assertDoesNotMatch(cdnMatcher, "http://localhost/jquery-1.11.0.min.js");
  }

}
