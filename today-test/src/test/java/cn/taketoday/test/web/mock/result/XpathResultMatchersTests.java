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

package cn.taketoday.test.web.mock.result;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.test.web.mock.StubMvcResult;
import cn.taketoday.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link XpathResultMatchers}.
 *
 * @author Rossen Stoyanchev
 */
public class XpathResultMatchersTests {

  private static final String RESPONSE_CONTENT = "<foo><bar>111</bar><bar>true</bar></foo>";

  @Test
  public void node() throws Exception {
    new XpathResultMatchers("/foo/bar", null).node(Matchers.notNullValue()).match(getStubMvcResult());
  }

  @Test
  public void nodeNoMatch() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new XpathResultMatchers("/foo/bar", null).node(Matchers.nullValue()).match(getStubMvcResult()));
  }

  @Test
  public void nodeList() throws Exception {
    new XpathResultMatchers("/foo/bar", null).nodeList(Matchers.notNullValue()).match(getStubMvcResult());
  }

  @Test
  public void nodeListNoMatch() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new XpathResultMatchers("/foo/bar", null).nodeList(Matchers.nullValue()).match(getStubMvcResult()));
  }

  @Test
  public void exists() throws Exception {
    new XpathResultMatchers("/foo/bar", null).exists().match(getStubMvcResult());
  }

  @Test
  public void existsNoMatch() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new XpathResultMatchers("/foo/Bar", null).exists().match(getStubMvcResult()));
  }

  @Test
  public void doesNotExist() throws Exception {
    new XpathResultMatchers("/foo/Bar", null).doesNotExist().match(getStubMvcResult());
  }

  @Test
  public void doesNotExistNoMatch() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new XpathResultMatchers("/foo/bar", null).doesNotExist().match(getStubMvcResult()));
  }

  @Test
  public void nodeCount() throws Exception {
    new XpathResultMatchers("/foo/bar", null).nodeCount(2).match(getStubMvcResult());
  }

  @Test
  public void nodeCountNoMatch() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new XpathResultMatchers("/foo/bar", null).nodeCount(1).match(getStubMvcResult()));
  }

  @Test
  public void string() throws Exception {
    new XpathResultMatchers("/foo/bar[1]", null).string("111").match(getStubMvcResult());
  }

  @Test
  public void stringNoMatch() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new XpathResultMatchers("/foo/bar[1]", null).string("112").match(getStubMvcResult()));
  }

  @Test
  public void number() throws Exception {
    new XpathResultMatchers("/foo/bar[1]", null).number(111.0).match(getStubMvcResult());
  }

  @Test
  public void numberNoMatch() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new XpathResultMatchers("/foo/bar[1]", null).number(111.1).match(getStubMvcResult()));
  }

  @Test
  public void booleanValue() throws Exception {
    new XpathResultMatchers("/foo/bar[2]", null).booleanValue(true).match(getStubMvcResult());
  }

  @Test
  public void booleanValueNoMatch() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new XpathResultMatchers("/foo/bar[2]", null).booleanValue(false).match(getStubMvcResult()));
  }

  @Test
  public void stringEncodingDetection() throws Exception {
    String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<person><name>Jürgen</name></person>";
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    response.addHeader("Content-Type", "application/xml");
    StreamUtils.copy(bytes, response.getOutputStream());
    StubMvcResult result = new StubMvcResult(null, null, null, null, null, null, response);

    new XpathResultMatchers("/person/name", null).string("Jürgen").match(result);
  }

  private StubMvcResult getStubMvcResult() throws Exception {
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    response.addHeader("Content-Type", "application/xml");
    response.getWriter().print(new String(RESPONSE_CONTENT.getBytes(StandardCharsets.ISO_8859_1)));
    return new StubMvcResult(null, null, null, null, null, null, response);
  }

}
