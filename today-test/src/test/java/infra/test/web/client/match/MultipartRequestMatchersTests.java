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

package infra.test.web.client.match;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import infra.http.HttpHeaders;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.converter.FormHttpMessageConverter;
import infra.mock.http.client.MockClientHttpRequest;
import infra.mock.web.MockMultipartFile;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.multipart.MultipartFile;

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
    this.expected.setOrRemove(foo.getName(), bar.getResource());

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
    this.expected.setOrRemove(f1.getName(), f1.getContentAsByteArray());
    this.expected.setOrRemove(f2.getName(), f2.getContentAsByteArray());
    this.expected.setOrRemove(f3.getName(), f3.getContentAsByteArray());

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
    this.expected.setOrRemove(f1.getName(), f2.getContentAsByteArray());

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
