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
    //
  void encodeAndExpandWithDollarSign() {
    UriComponents uri = UriComponentsBuilder.forPath("/path").queryParam("q", "{value}").encode().build();
    assertThat(uri.expand("JavaClass$1.class").toString()).isEqualTo("/path?q=JavaClass%241.class");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void toURIEncoded(ParserType parserType) {
    UriComponents uri = UriComponentsBuilder.forURIString("https://example.com/hotel list/Z\u00fcrich", parserType).build();
    assertThat(uri.encode().toURI()).isEqualTo(URI.create("https://example.com/hotel%20list/Z%C3%BCrich"));
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void toURINotEncoded(ParserType parserType) {
    UriComponents uri = UriComponentsBuilder.forURIString("https://example.com/hotel list/Z\u00fcrich", parserType).build();
    assertThat(uri.toURI()).isEqualTo(URI.create("https://example.com/hotel%20list/Z\u00fcrich"));
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void toURIAlreadyEncoded(ParserType parserType) {
    UriComponents uri = UriComponentsBuilder.forURIString("https://example.com/hotel%20list/Z%C3%BCrich", parserType).build(true);
    assertThat(uri.encode().toURI()).isEqualTo(URI.create("https://example.com/hotel%20list/Z%C3%BCrich"));
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void toURIWithIpv6HostAlreadyEncoded(ParserType parserType) {
    UriComponents uri = UriComponentsBuilder.forURIString(
            "http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich", parserType).build(true);

    assertThat(uri.encode().toURI()).isEqualTo(
            URI.create("http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich"));
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void toURIStringWithPortVariable(ParserType parserType) {
    String url = "http://localhost:{port}/first";
    assertThat(UriComponentsBuilder.forURIString(url, parserType).build().toUriString()).isEqualTo(url);
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void expand(ParserType parserType) {
    UriComponents uri = UriComponentsBuilder.forURIString("https://example.com", parserType).path("/{foo} {bar}").build();
    uri = uri.expand("1 2", "3 4");

    assertThat(uri.getPath()).isEqualTo("/1 2 3 4");
    assertThat(uri.toUriString()).isEqualTo("https://example.com/1 2 3 4");
  }

  @ParameterizedTest //
  @EnumSource(value = ParserType.class)
  void expandWithRegexVar(ParserType parserType) {
    String template = "/myurl/{name:[a-z]{1,5}}/show";
    UriComponents uri = UriComponentsBuilder.forURIString(template, parserType).build();
    uri = uri.expand(Collections.singletonMap("name", "test"));

    assertThat(uri.getPath()).isEqualTo("/myurl/test/show");
  }

  @ParameterizedTest //
  @EnumSource(value = ParserType.class)
  void uirTemplateExpandWithMismatchedCurlyBraces(ParserType parserType) {
    UriComponents uri = UriComponentsBuilder.forURIString("/myurl/?q={{{{", parserType).encode().build();
    assertThat(uri.toUriString()).isEqualTo("/myurl/?q=%7B%7B%7B%7B");
  }

  @ParameterizedTest // gh-22447
  @EnumSource(value = ParserType.class)
  void expandWithFragmentOrder(ParserType parserType) {
    UriComponents uri = UriComponentsBuilder
            .forURIString("https://{host}/{path}#{fragment}", parserType).build()
            .expand("example.com", "foo", "bar");

    assertThat(uri.toUriString()).isEqualTo("https://example.com/foo#bar");
  }

  @ParameterizedTest //
  @EnumSource(value = ParserType.class)
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
  @EnumSource(value = ParserType.class)
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
  @EnumSource(value = ParserType.class)
  void normalize(ParserType parserType) {
    UriComponents uri = UriComponentsBuilder.forURIString("https://example.com/foo/../bar", parserType).build();
    assertThat(uri.normalize().toString()).isEqualTo("https://example.com/bar");
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void serializable(ParserType parserType) throws Exception {
    UriComponents uri = UriComponentsBuilder.forURIString(
            "https://example.com", parserType).path("/{foo}").query("bar={baz}").build();

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(uri);
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
    UriComponents readObject = (UriComponents) ois.readObject();

    assertThat(uri.toString()).isEqualTo(readObject.toString());
  }

  @Test
  void copyToURIComponentsBuilder() {
    UriComponents source = UriComponentsBuilder.forPath("/foo/bar").pathSegment("ba/z").build();
    UriComponentsBuilder targetBuilder = UriBuilder.forUriComponents();
    source.copyToUriComponentsBuilder(targetBuilder);
    UriComponents result = targetBuilder.build().encode();

    assertThat(result.getPath()).isEqualTo("/foo/bar/ba%2Fz");
    assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar", "ba%2Fz"));
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void equalsHierarchicalUriComponents(ParserType parserType) {
    String url = "https://example.com";
    UriComponents uric1 = UriComponentsBuilder.forURIString(url, parserType).path("/{foo}").query("bar={baz}").build();
    UriComponents uric2 = UriComponentsBuilder.forURIString(url, parserType).path("/{foo}").query("bar={baz}").build();
    UriComponents uric3 = UriComponentsBuilder.forURIString(url, parserType).path("/{foo}").query("bin={baz}").build();

    assertThat(uric1).isInstanceOf(HierarchicalUriComponents.class);
    assertThat(uric1).isEqualTo(uric1);
    assertThat(uric1).isEqualTo(uric2);
    assertThat(uric1).isNotEqualTo(uric3);
  }

  @ParameterizedTest
  @EnumSource(value = ParserType.class)
  void equalsOpaqueUriComponents(ParserType parserType) {
    String baseUrl = "http:example.com";
    UriComponents uric1 = UriComponentsBuilder.forURIString(baseUrl + "/foo/bar", parserType).build();
    UriComponents uric2 = UriComponentsBuilder.forURIString(baseUrl + "/foo/bar", parserType).build();
    UriComponents uric3 = UriComponentsBuilder.forURIString(baseUrl + "/foo/bin", parserType).build();

    assertThat(uric1).isEqualTo(uric1);
    assertThat(uric1).isEqualTo(uric2);
    assertThat(uric1).isNotEqualTo(uric3);
  }

}
