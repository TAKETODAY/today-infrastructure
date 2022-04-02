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

package cn.taketoday.test.web.client.match;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.mock.http.client.MockClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit tests for {@link JsonPathRequestMatchers}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class JsonPathRequestMatchersTests {

  private static final String REQUEST_CONTENT = "{" + //
          "'str':         'foo',           " + //
          "'num':         5,               " + //
          "'bool':        true,            " + //
          "'arr':         [42],            " + //
          "'colorMap':    {'red': 'rojo'}, " + //
          "'emptyString': '',              " + //
          "'emptyArray':  [],              " + //
          "'emptyMap':    {}               " + //
          "}";

  private static final MockClientHttpRequest request = new MockClientHttpRequest();

  static {
    try {
      request.getBody().write(REQUEST_CONTENT.getBytes());
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  public void valueWithMismatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.str").value("bogus").match(request));
  }

  @Test
  public void valueWithDirectMatch() throws Exception {
    new JsonPathRequestMatchers("$.str").value("foo").match(request);
  }

  @Test // SPR-14498
  public void valueWithNumberConversion() throws Exception {
    new JsonPathRequestMatchers("$.num").value(5.0f).match(request);
  }

  @Test
  public void valueWithMatcher() throws Exception {
    new JsonPathRequestMatchers("$.str").value(equalTo("foo")).match(request);
  }

  @Test // SPR-14498
  public void valueWithMatcherAndNumberConversion() throws Exception {
    new JsonPathRequestMatchers("$.num").value(equalTo(5.0f), Float.class).match(request);
  }

  @Test
  public void valueWithMatcherAndMismatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.str").value(equalTo("bogus")).match(request));
  }

  @Test
  public void exists() throws Exception {
    new JsonPathRequestMatchers("$.str").exists().match(request);
  }

  @Test
  public void existsForAnEmptyArray() throws Exception {
    new JsonPathRequestMatchers("$.emptyArray").exists().match(request);
  }

  @Test
  public void existsForAnEmptyMap() throws Exception {
    new JsonPathRequestMatchers("$.emptyMap").exists().match(request);
  }

  @Test
  public void existsNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.bogus").exists().match(request));
  }

  @Test
  public void doesNotExist() throws Exception {
    new JsonPathRequestMatchers("$.bogus").doesNotExist().match(request);
  }

  @Test
  public void doesNotExistNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.str").doesNotExist().match(request));
  }

  @Test
  public void doesNotExistForAnEmptyArray() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.emptyArray").doesNotExist().match(request));
  }

  @Test
  public void doesNotExistForAnEmptyMap() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.emptyMap").doesNotExist().match(request));
  }

  @Test
  public void isEmptyForAnEmptyString() throws Exception {
    new JsonPathRequestMatchers("$.emptyString").isEmpty().match(request);
  }

  @Test
  public void isEmptyForAnEmptyArray() throws Exception {
    new JsonPathRequestMatchers("$.emptyArray").isEmpty().match(request);
  }

  @Test
  public void isEmptyForAnEmptyMap() throws Exception {
    new JsonPathRequestMatchers("$.emptyMap").isEmpty().match(request);
  }

  @Test
  public void isNotEmptyForString() throws Exception {
    new JsonPathRequestMatchers("$.str").isNotEmpty().match(request);
  }

  @Test
  public void isNotEmptyForNumber() throws Exception {
    new JsonPathRequestMatchers("$.num").isNotEmpty().match(request);
  }

  @Test
  public void isNotEmptyForBoolean() throws Exception {
    new JsonPathRequestMatchers("$.bool").isNotEmpty().match(request);
  }

  @Test
  public void isNotEmptyForArray() throws Exception {
    new JsonPathRequestMatchers("$.arr").isNotEmpty().match(request);
  }

  @Test
  public void isNotEmptyForMap() throws Exception {
    new JsonPathRequestMatchers("$.colorMap").isNotEmpty().match(request);
  }

  @Test
  public void isNotEmptyForAnEmptyString() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.emptyString").isNotEmpty().match(request));
  }

  @Test
  public void isNotEmptyForAnEmptyArray() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.emptyArray").isNotEmpty().match(request));
  }

  @Test
  public void isNotEmptyForAnEmptyMap() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.emptyMap").isNotEmpty().match(request));
  }

  @Test
  public void isArray() throws Exception {
    new JsonPathRequestMatchers("$.arr").isArray().match(request);
  }

  @Test
  public void isArrayForAnEmptyArray() throws Exception {
    new JsonPathRequestMatchers("$.emptyArray").isArray().match(request);
  }

  @Test
  public void isArrayNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.str").isArray().match(request));
  }

  @Test
  public void isMap() throws Exception {
    new JsonPathRequestMatchers("$.colorMap").isMap().match(request);
  }

  @Test
  public void isMapForAnEmptyMap() throws Exception {
    new JsonPathRequestMatchers("$.emptyMap").isMap().match(request);
  }

  @Test
  public void isMapNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.str").isMap().match(request));
  }

  @Test
  public void isBoolean() throws Exception {
    new JsonPathRequestMatchers("$.bool").isBoolean().match(request);
  }

  @Test
  public void isBooleanNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.str").isBoolean().match(request));
  }

  @Test
  public void isNumber() throws Exception {
    new JsonPathRequestMatchers("$.num").isNumber().match(request);
  }

  @Test
  public void isNumberNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.str").isNumber().match(request));
  }

  @Test
  public void isString() throws Exception {
    new JsonPathRequestMatchers("$.str").isString().match(request);
  }

  @Test
  public void isStringNoMatch() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathRequestMatchers("$.arr").isString().match(request));
  }

}
