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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import cn.taketoday.core.LinkedMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.FormHttpMessageConverter;
import cn.taketoday.mock.http.client.MockClientHttpRequest;
import cn.taketoday.mock.web.MockMultipartFile;
import cn.taketoday.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for
 * {@link ContentRequestMatchers#multipartData(MultiValueMap)} and.
 * {@link ContentRequestMatchers#multipartDataContains(Map)}.
 *
 * @author Valentin Spac
 * @author Rossen Stoyanchev
 */
public class MultipartRequestMatchersTests {

  private final MockClientHttpRequest request = new MockClientHttpRequest();

  private final MultiValueMap<String, Object> input = new LinkedMultiValueMap<>();

  private final MultiValueMap<String, Object> expected = new LinkedMultiValueMap<>();

  @BeforeEach
  public void setup() {
    this.request.getHeaders().setContentType(MediaType.MULTIPART_FORM_DATA);
  }

  @Test
  public void testContains() throws Exception {
    this.input.add("foo", "bar");
    this.input.add("foo", "baz");
    this.input.add("lorem", "ipsum");

    this.expected.add("foo", "bar");

    writeAndAssertContains();
  }

  @Test
  public void testDoesNotContain() {
    this.input.add("foo", "bar");
    this.input.add("foo", "baz");
    this.input.add("lorem", "ipsum");

    this.expected.add("foo", "wrongValue");

    assertThatExceptionOfType(AssertionError.class).isThrownBy(this::writeAndAssert);
  }

  @Test
  public void testParamsMatch() throws Exception {
    this.input.add("foo", "value 1");
    this.input.add("bar", "value A");
    this.input.add("baz", "value B");

    this.expected.addAll(this.input);

    writeAndAssert();
  }

  @Test
  public void testResourceMatch() throws Exception {
    MultipartFile f1 = new MockMultipartFile("f1", "foo.txt", "text/plain", "Foo Lorem ipsum".getBytes());
    MultipartFile f2 = new MockMultipartFile("f2", "bar.txt", "text/plain", "Bar Lorem ipsum".getBytes());
    MultipartFile f3 = new MockMultipartFile("f3", "foobar.txt", "text/plain", "Foobar Lorem ipsum".getBytes());

    this.input.add("fooParam", "foo value");
    this.input.add("barParam", "bar value");
    this.input.add(f1.getName(), f1.getResource());
    this.input.add(f2.getName(), f2.getResource());
    this.input.add(f3.getName(), f3.getResource());

    this.expected.addAll(this.input);

    writeAndAssert();
  }

  @Test
  public void testResourceNoMatch() {
    MockMultipartFile foo = new MockMultipartFile("f1", "foo.txt", "text/plain", "Foo Lorem ipsum".getBytes());
    MockMultipartFile bar = new MockMultipartFile("f2", "bar.txt", "text/plain", "Bar Lorem ipsum".getBytes());

    this.input.add("fooParam", "foo value");
    this.input.add("barParam", "bar value");
    this.input.add(foo.getName(), foo.getResource());
    this.input.add(bar.getName(), bar.getResource());

    this.expected.addAll(this.input);
    this.expected.set(foo.getName(), bar.getResource());

    assertThatExceptionOfType(AssertionError.class).isThrownBy(this::writeAndAssert);
  }

  @Test
  public void testByteArrayMatch() throws Exception {
    MultipartFile f1 = new MockMultipartFile("f1", "foo.txt", "text/plain", "Foo Lorem ipsum".getBytes());
    MultipartFile f2 = new MockMultipartFile("f2", "bar.txt", "text/plain", "Bar Lorem ipsum".getBytes());
    MultipartFile f3 = new MockMultipartFile("f3", "foobar.txt", "text/plain", "Foobar Lorem ipsum".getBytes());

    this.input.add("fooParam", "foo value");
    this.input.add("barParam", "bar value");
    this.input.add(f1.getName(), f1.getResource());
    this.input.add(f2.getName(), f2.getResource());
    this.input.add(f3.getName(), f3.getResource());

    this.expected.addAll(this.input);
    this.expected.set(f1.getName(), f1.getBytes());
    this.expected.set(f2.getName(), f2.getBytes());
    this.expected.set(f3.getName(), f3.getBytes());

    writeAndAssert();
  }

  @Test
  public void testByteArrayNoMatch() throws Exception {
    MultipartFile f1 = new MockMultipartFile("f1", "foo.txt", "text/plain", "Foo Lorem ipsum".getBytes());
    MultipartFile f2 = new MockMultipartFile("f2", "bar.txt", "text/plain", "Bar Lorem ipsum".getBytes());

    this.input.add("fooParam", "foo value");
    this.input.add("barParam", "bar value");
    this.input.add(f1.getName(), f1.getResource());
    this.input.add(f2.getName(), f2.getResource());

    this.expected.addAll(this.input);
    this.expected.set(f1.getName(), f2.getBytes());

    assertThatExceptionOfType(AssertionError.class).isThrownBy(this::writeAndAssert);
  }

  private void writeAndAssert() throws IOException {
    writeForm();
    new ContentRequestMatchers().multipartData(this.expected).match(request);
  }

  private void writeAndAssertContains() throws IOException {
    writeForm();
    Map<String, Object> expectedMap = this.expected.toSingleValueMap();
    new ContentRequestMatchers().multipartDataContains(expectedMap).match(request);
  }

  private void writeForm() throws IOException {
    new FormHttpMessageConverter().write(this.input, MediaType.MULTIPART_FORM_DATA,
            new HttpOutputMessage() {
              @Override
              public OutputStream getBody() throws IOException {
                return MultipartRequestMatchersTests.this.request.getBody();
              }

              @Override
              public HttpHeaders getHeaders() {
                return MultipartRequestMatchersTests.this.request.getHeaders();
              }
            });
  }

}
