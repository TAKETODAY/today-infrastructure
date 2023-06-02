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

package cn.taketoday.test.web.servlet.result;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.test.web.servlet.StubMvcResult;

import static cn.taketoday.http.MediaType.APPLICATION_JSON_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class ContentResultMatchersTests {

  @Test
  void typeMatches() throws Exception {
    new ContentResultMatchers().contentType(APPLICATION_JSON_VALUE).match(getStubMvcResult(CONTENT));
  }

  @Test
  void typeNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new ContentResultMatchers().contentType("text/plain").match(getStubMvcResult(CONTENT)));
  }

  @Test
  void string() throws Exception {
    new ContentResultMatchers().string(new String(CONTENT.getBytes(UTF_8))).match(getStubMvcResult(CONTENT));
  }

  @Test
  void stringNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new ContentResultMatchers().encoding("bogus").match(getStubMvcResult(CONTENT)));
  }

  @Test
  void stringMatcher() throws Exception {
    String content = new String(CONTENT.getBytes(UTF_8));
    new ContentResultMatchers().string(Matchers.equalTo(content)).match(getStubMvcResult(CONTENT));
  }

  @Test
  void stringMatcherNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new ContentResultMatchers().string(Matchers.equalTo("bogus")).match(getStubMvcResult(CONTENT)));
  }

  @Test
  void bytes() throws Exception {
    new ContentResultMatchers().bytes(CONTENT.getBytes(UTF_8)).match(getStubMvcResult(CONTENT));
  }

  @Test
  void bytesNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new ContentResultMatchers().bytes("bogus".getBytes()).match(getStubMvcResult(CONTENT)));
  }

  @Test
  void jsonLenientMatch() throws Exception {
    new ContentResultMatchers().json("{\n \"foo\" : \"bar\"  \n}").match(getStubMvcResult(CONTENT));
    new ContentResultMatchers().json("{\n \"foo\" : \"bar\"  \n}", false).match(getStubMvcResult(CONTENT));
  }

  @Test
  void jsonStrictMatch() throws Exception {
    new ContentResultMatchers().json("{\n \"foo\":\"bar\",   \"foo array\":[\"foo\",\"bar\"] \n}", true).match(getStubMvcResult(CONTENT));
    new ContentResultMatchers().json("{\n \"foo array\":[\"foo\",\"bar\"], \"foo\":\"bar\" \n}", true).match(getStubMvcResult(CONTENT));
  }

  @Test
  void jsonLenientNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new ContentResultMatchers().json("{\n\"fooo\":\"bar\"\n}").match(getStubMvcResult(CONTENT)));
  }

  @Test
  void jsonStrictNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new ContentResultMatchers().json("{\"foo\":\"bar\",   \"foo array\":[\"bar\",\"foo\"]}", true).match(getStubMvcResult(CONTENT)));
  }

  @Test
    // gh-23622
  void jsonUtf8Match() throws Exception {
    new ContentResultMatchers().json("{\"name\":\"Jürgen\"}").match(getStubMvcResult(UTF8_CONTENT));
  }

  private static final String CONTENT = "{\"foo\":\"bar\",\"foo array\":[\"foo\",\"bar\"]}";

  private static final String UTF8_CONTENT = "{\"name\":\"Jürgen\"}";

  private StubMvcResult getStubMvcResult(String content) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.addHeader("Content-Type", APPLICATION_JSON_VALUE);
    response.getOutputStream().write(content.getBytes(UTF_8));
    return new StubMvcResult(null, null, null, null, null, null, response);
  }

}
