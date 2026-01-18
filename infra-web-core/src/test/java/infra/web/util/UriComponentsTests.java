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

package infra.web.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.web.util.UriComponentsBuilder.ParserType;

import static infra.web.util.UriComponentsBuilder.forURIString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link UriComponents}.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 */
class UriComponentsTests {

  @Test
  void expandAndEncode() {
    UriComponents uri = UriComponentsBuilder
            .forPath("/hotel list/{city} specials").queryParam("q", "{value}").build()
            .expand("Z\u00fcrich", "a+b").encode();

    assertThat(uri.toString()).isEqualTo("/hotel%20list/Z%C3%BCrich%20specials?q=a+b");
  }

  @Test
  void encodeAndExpand() {
    UriComponents uri = UriComponentsBuilder
            .forPath("/hotel list/{city} specials").queryParam("q", "{value}").encode().build()
            .expand("Z\u00fcrich", "a+b");

    assertThat(uri.toString()).isEqualTo("/hotel%20list/Z%C3%BCrich%20specials?q=a%2Bb");
  }

  @Test
  void encodeAndExpandPartially() {
    UriComponents uri = UriComponentsBuilder
            .forPath("/hotel list/{city} specials").queryParam("q", "{value}").encode()
            .uriVariables(Collections.singletonMap("city", "Z\u00fcrich")).build();

    assertThat(uri.expand("a+b").toString()).isEqualTo("/hotel%20list/Z%C3%BCrich%20specials?q=a%2Bb");
  }

  @Test
    // SPR-17168
  void encodeAndExpandWithDollarSign() {
    UriComponents uri = UriComponentsBuilder.forPath("/path").queryParam("q", "{value}").encode().build();
    assertThat(uri.expand("JavaClass$1.class").toString()).isEqualTo("/path?q=JavaClass%241.class");
  }

  @ParameterizedTest
  @EnumSource
  void toURIEncoded(ParserType parserType) {
    UriComponents uri = forURIString("https://example.com/hotel list/Z\u00fcrich", parserType).build();
    assertThat(uri.encode().toURI()).isEqualTo(URI.create("https://example.com/hotel%20list/Z%C3%BCrich"));
  }

  @ParameterizedTest
  @EnumSource
  void toURINotEncoded(ParserType parserType) {
    UriComponents uri = forURIString("https://example.com/hotel list/Z\u00fcrich", parserType).build();
    assertThat(uri.toURI()).isEqualTo(URI.create("https://example.com/hotel%20list/Z\u00fcrich"));
  }

  @ParameterizedTest
  @EnumSource
  void toURIAlreadyEncoded(ParserType parserType) {
    UriComponents uri = forURIString("https://example.com/hotel%20list/Z%C3%BCrich", parserType).build(true);
    assertThat(uri.encode().toURI()).isEqualTo(URI.create("https://example.com/hotel%20list/Z%C3%BCrich"));
  }

  @ParameterizedTest
  @EnumSource
  void toURIWithIpv6HostAlreadyEncoded(ParserType parserType) {
    UriComponents uri = forURIString(
            "http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich", parserType).build(true);

    assertThat(uri.encode().toURI()).isEqualTo(
            URI.create("http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich"));
  }

  @ParameterizedTest
  @EnumSource
  void toURIStringWithPortVariable(ParserType parserType) {
    String url = "http://localhost:{port}/first";
    assertThat(forURIString(url, parserType).build().toUriString()).isEqualTo(url);
  }

  @ParameterizedTest
  @EnumSource
  void expand(ParserType parserType) {
    UriComponents uri = forURIString("https://example.com", parserType).path("/{foo} {bar}").build();
    uri = uri.expand("1 2", "3 4");

    assertThat(uri.getPath()).isEqualTo("/1 2 3 4");
    assertThat(uri.toUriString()).isEqualTo("https://example.com/1 2 3 4");
  }

  @ParameterizedTest // SPR-13311
  @EnumSource
  void expandWithRegexVar(ParserType parserType) {
    String template = "/myurl/{name:[a-z]{1,5}}/show";
    UriComponents uri = forURIString(template, parserType).build();
    uri = uri.expand(Collections.singletonMap("name", "test"));

    assertThat(uri.getPath()).isEqualTo("/myurl/test/show");
  }

  @ParameterizedTest // SPR-17630
  @EnumSource
  void uirTemplateExpandWithMismatchedCurlyBraces(ParserType parserType) {
    UriComponents uri = forURIString("/myurl/?q={{{{", parserType).encode().build();
    assertThat(uri.toUriString()).isEqualTo("/myurl/?q=%7B%7B%7B%7B");
  }

  @ParameterizedTest // gh-22447
  @EnumSource
  void expandWithFragmentOrder(ParserType parserType) {
    UriComponents uri = forURIString("https://{host}/{path}#{fragment}", parserType).build()
            .expand("example.com", "foo", "bar");

    assertThat(uri.toUriString()).isEqualTo("https://example.com/foo#bar");
  }

  @Test
  void expandQueryParamWithArray() {
    String uri = UriComponentsBuilder.forPath("/hello")
            .queryParam("name", "{name}")
            .buildAndExpand(Map.of("name", new String[] { "foo", "bar" }))
            .toString();

    assertThat(uri).isEqualTo("/hello?name=foo,bar");
  }

  @Test
  void expandQueryParamWithList() {
    String uri = UriComponentsBuilder.forPath("/hello")
            .queryParam("name", "{name}")
            .buildAndExpand(Map.of("name", List.of("foo", "bar")))
            .toString();

    assertThat(uri).isEqualTo("/hello?name=foo,bar");
  }

  @ParameterizedTest // SPR-12123
  @EnumSource
  void port(ParserType parserType) {
    UriComponents uri1 = forURIString("https://example.com:8080/bar", parserType).build();
    UriComponents uri2 = forURIString("https://example.com/bar", parserType).port(8080).build();
    UriComponents uri3 = forURIString("https://example.com/bar", parserType).port("{port}").build().expand(8080);
    UriComponents uri4 = forURIString("https://example.com/bar", parserType).port("808{digit}").build().expand(0);

    assertThat(uri1.getPort()).isEqualTo(8080);
    assertThat(uri1.toUriString()).isEqualTo("https://example.com:8080/bar");
    assertThat(uri2.getPort()).isEqualTo(8080);
    assertThat(uri2.toUriString()).isEqualTo("https://example.com:8080/bar");
    assertThat(uri3.getPort()).isEqualTo(8080);
    assertThat(uri3.toUriString()).isEqualTo("https://example.com:8080/bar");
    assertThat(uri4.getPort()).isEqualTo(8080);
    assertThat(uri4.toUriString()).isEqualTo("https://example.com:8080/bar");
  }

  @ParameterizedTest // gh-28521
  @EnumSource
  void invalidPort(ParserType parserType) {
    assertThatExceptionOfType(InvalidUrlException.class)
            .isThrownBy(() -> forURIString("https://example.com:XXX/bar", parserType));
    assertExceptionsForInvalidPort(forURIString("https://example.com/bar", parserType).port("XXX").build());
  }

  private void assertExceptionsForInvalidPort(UriComponents uriComponents) {
    assertThatIllegalStateException()
            .isThrownBy(uriComponents::getPort)
            .withMessage("The port must be an integer: XXX");
    assertThatIllegalStateException()
            .isThrownBy(uriComponents::toURI)
            .withMessage("The port must be an integer: XXX");
  }

  @Test
  void expandEncoded() {
    assertThatIllegalStateException().isThrownBy(() ->
            UriComponentsBuilder.forPath("/{foo}").build().encode().expand("bar"));
  }

  @Test
  void invalidCharacters() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            UriComponentsBuilder.forPath("/{foo}").build(true));
  }

  @Test
  void invalidEncodedSequence() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            UriComponentsBuilder.forPath("/fo%2o").build(true));
  }

  @ParameterizedTest
  @EnumSource
  void normalize(ParserType parserType) {
    UriComponents uri = forURIString("https://example.com/foo/../bar", parserType).build();
    assertThat(uri.normalize().toString()).isEqualTo("https://example.com/bar");
  }

  @ParameterizedTest
  @EnumSource
  void serializable(ParserType parserType) throws Exception {
    UriComponents uri = forURIString(
            "https://example.com", parserType).path("/{foo}").query("bar={baz}").build();

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(uri);
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
    UriComponents readObject = (UriComponents) ois.readObject();

    assertThat(uri.toString()).isEqualTo(readObject.toString());
  }

  @Test
  void copytoURIComponentsBuilder() {
    UriComponents source = UriComponentsBuilder.forPath("/foo/bar").pathSegment("ba/z").build();
    UriComponentsBuilder targetBuilder = UriComponentsBuilder.create();
    source.copyToUriComponentsBuilder(targetBuilder);
    UriComponents result = targetBuilder.build().encode();

    assertThat(result.getPath()).isEqualTo("/foo/bar/ba%2Fz");
    assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar", "ba%2Fz"));
  }

  @ParameterizedTest
  @EnumSource
  void equalsHierarchicalUriComponents(ParserType parserType) {
    String url = "https://example.com";
    UriComponents uric1 = forURIString(url, parserType).path("/{foo}").query("bar={baz}").build();
    UriComponents uric2 = forURIString(url, parserType).path("/{foo}").query("bar={baz}").build();
    UriComponents uric3 = forURIString(url, parserType).path("/{foo}").query("bin={baz}").build();

    assertThat(uric1).isInstanceOf(HierarchicalUriComponents.class);
    assertThat(uric1).isEqualTo(uric1);
    assertThat(uric1).isEqualTo(uric2);
    assertThat(uric1).isNotEqualTo(uric3);
  }

  @ParameterizedTest
  @EnumSource
  void equalsOpaqueUriComponents(ParserType parserType) {
    String baseUrl = "http:example.com";
    UriComponents uric1 = forURIString(baseUrl + "/foo/bar", parserType).build();
    UriComponents uric2 = forURIString(baseUrl + "/foo/bar", parserType).build();
    UriComponents uric3 = forURIString(baseUrl + "/foo/bin", parserType).build();

    assertThat(uric1).isEqualTo(uric1);
    assertThat(uric1).isEqualTo(uric2);
    assertThat(uric1).isNotEqualTo(uric3);
  }

}
