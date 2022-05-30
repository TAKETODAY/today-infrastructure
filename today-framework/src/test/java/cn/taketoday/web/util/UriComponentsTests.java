/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import static cn.taketoday.web.util.UriComponentsBuilder.fromHttpUrl;
import static cn.taketoday.web.util.UriComponentsBuilder.fromUriString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link UriComponents}.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 */
public class UriComponentsTests {

  @Test
  public void expandAndEncode() {
    UriComponents uri = UriComponentsBuilder
            .fromPath("/hotel list/{city} specials").queryParam("q", "{value}").build()
            .expand("Z\u00fcrich", "a+b").encode();

    assertThat(uri.toString()).isEqualTo("/hotel%20list/Z%C3%BCrich%20specials?q=a+b");
  }

  @Test
  public void encodeAndExpand() {
    UriComponents uri = UriComponentsBuilder
            .fromPath("/hotel list/{city} specials").queryParam("q", "{value}").encode().build()
            .expand("Z\u00fcrich", "a+b");

    assertThat(uri.toString()).isEqualTo("/hotel%20list/Z%C3%BCrich%20specials?q=a%2Bb");
  }

  @Test
  public void encodeAndExpandPartially() {
    UriComponents uri = UriComponentsBuilder
            .fromPath("/hotel list/{city} specials").queryParam("q", "{value}").encode()
            .uriVariables(Collections.singletonMap("city", "Z\u00fcrich")).build();

    assertThat(uri.expand("a+b").toString()).isEqualTo("/hotel%20list/Z%C3%BCrich%20specials?q=a%2Bb");
  }

  @Test  // SPR-17168
  public void encodeAndExpandWithDollarSign() {
    UriComponents uri = UriComponentsBuilder.fromPath("/path").queryParam("q", "{value}").encode().build();
    assertThat(uri.expand("JavaClass$1.class").toString()).isEqualTo("/path?q=JavaClass%241.class");
  }

  @Test
  public void toUriEncoded() throws URISyntaxException {
    UriComponents uri = UriComponentsBuilder.fromUriString("https://example.com/hotel list/Z\u00fcrich").build();
    assertThat(uri.encode().toUri()).isEqualTo(new URI("https://example.com/hotel%20list/Z%C3%BCrich"));
  }

  @Test
  public void toUriNotEncoded() throws URISyntaxException {
    UriComponents uri = UriComponentsBuilder.fromUriString("https://example.com/hotel list/Z\u00fcrich").build();
    assertThat(uri.toUri()).isEqualTo(new URI("https://example.com/hotel%20list/Z\u00fcrich"));
  }

  @Test
  public void toUriAlreadyEncoded() throws URISyntaxException {
    UriComponents uri = UriComponentsBuilder.fromUriString("https://example.com/hotel%20list/Z%C3%BCrich").build(true);
    assertThat(uri.encode().toUri()).isEqualTo(new URI("https://example.com/hotel%20list/Z%C3%BCrich"));
  }

  @Test
  public void toUriWithIpv6HostAlreadyEncoded() throws URISyntaxException {
    UriComponents uri = UriComponentsBuilder.fromUriString(
            "http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich").build(true);

    assertThat(uri.encode().toUri()).isEqualTo(
            new URI("http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich"));
  }

  @Test
  public void expand() {
    UriComponents uri = UriComponentsBuilder.fromUriString("https://example.com").path("/{foo} {bar}").build();
    uri = uri.expand("1 2", "3 4");

    assertThat(uri.getPath()).isEqualTo("/1 2 3 4");
    assertThat(uri.toUriString()).isEqualTo("https://example.com/1 2 3 4");
  }

  @Test  // SPR-13311
  public void expandWithRegexVar() {
    String template = "/myurl/{name:[a-z]{1,5}}/show";
    UriComponents uri = UriComponentsBuilder.fromUriString(template).build();
    uri = uri.expand(Collections.singletonMap("name", "test"));

    assertThat(uri.getPath()).isEqualTo("/myurl/test/show");
  }

  @Test  // SPR-17630
  public void uirTemplateExpandWithMismatchedCurlyBraces() {
    UriComponents uri = UriComponentsBuilder.fromUriString("/myurl/?q={{{{").encode().build();
    assertThat(uri.toUriString()).isEqualTo("/myurl/?q=%7B%7B%7B%7B");
  }

  @Test  // gh-22447
  public void expandWithFragmentOrder() {
    UriComponents uri = UriComponentsBuilder
            .fromUriString("https://{host}/{path}#{fragment}").build()
            .expand("example.com", "foo", "bar");

    assertThat(uri.toUriString()).isEqualTo("https://example.com/foo#bar");
  }

  @Test  // SPR-12123
  public void port() {
    UriComponents uri1 = fromUriString("https://example.com:8080/bar").build();
    UriComponents uri2 = fromUriString("https://example.com/bar").port(8080).build();
    UriComponents uri3 = fromUriString("https://example.com/bar").port("{port}").build().expand(8080);
    UriComponents uri4 = fromUriString("https://example.com/bar").port("808{digit}").build().expand(0);

    assertThat(uri1.getPort()).isEqualTo(8080);
    assertThat(uri1.toUriString()).isEqualTo("https://example.com:8080/bar");
    assertThat(uri2.getPort()).isEqualTo(8080);
    assertThat(uri2.toUriString()).isEqualTo("https://example.com:8080/bar");
    assertThat(uri3.getPort()).isEqualTo(8080);
    assertThat(uri3.toUriString()).isEqualTo("https://example.com:8080/bar");
    assertThat(uri4.getPort()).isEqualTo(8080);
    assertThat(uri4.toUriString()).isEqualTo("https://example.com:8080/bar");
  }

  @Test
    // gh-28521
  void invalidPort() {
    assertExceptionsForInvalidPort(fromUriString("https://example.com:XXX/bar").build());
    assertExceptionsForInvalidPort(fromUriString("https://example.com/bar").port("XXX").build());
    assertExceptionsForInvalidPort(fromHttpUrl("https://example.com:XXX/bar").build());
    assertExceptionsForInvalidPort(fromHttpUrl("https://example.com/bar").port("XXX").build());
  }

  private void assertExceptionsForInvalidPort(UriComponents uriComponents) {
    assertThatIllegalStateException()
            .isThrownBy(uriComponents::getPort)
            .withMessage("The port must be an integer: XXX");
    assertThatIllegalStateException()
            .isThrownBy(uriComponents::toUri)
            .withMessage("The port must be an integer: XXX");
    assertThatIllegalStateException()
            .isThrownBy(uriComponents::toUriString)
            .withMessage("The port must be an integer: XXX");
    assertThatIllegalStateException()
            .isThrownBy(uriComponents::toString)
            .withMessage("The port must be an integer: XXX");
  }

  @Test
  public void expandEncoded() {
    assertThatIllegalStateException().isThrownBy(() ->
            UriComponentsBuilder.fromPath("/{foo}").build().encode().expand("bar"));
  }

  @Test
  public void invalidCharacters() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            UriComponentsBuilder.fromPath("/{foo}").build(true));
  }

  @Test
  public void invalidEncodedSequence() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            UriComponentsBuilder.fromPath("/fo%2o").build(true));
  }

  @Test
  public void normalize() {
    UriComponents uri = UriComponentsBuilder.fromUriString("https://example.com/foo/../bar").build();
    assertThat(uri.normalize().toString()).isEqualTo("https://example.com/bar");
  }

  @Test
  public void serializable() throws Exception {
    UriComponents uri = UriComponentsBuilder.fromUriString(
            "https://example.com").path("/{foo}").query("bar={baz}").build();

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(uri);
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
    UriComponents readObject = (UriComponents) ois.readObject();

    assertThat(uri.toString()).isEqualTo(readObject.toString());
  }

  @Test
  public void copyToUriComponentsBuilder() {
    UriComponents source = UriComponentsBuilder.fromPath("/foo/bar").pathSegment("ba/z").build();
    UriComponentsBuilder targetBuilder = UriComponentsBuilder.newInstance();
    source.copyToUriComponentsBuilder(targetBuilder);
    UriComponents result = targetBuilder.build().encode();

    assertThat(result.getPath()).isEqualTo("/foo/bar/ba%2Fz");
    assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar", "ba%2Fz"));
  }

  @Test
  public void equalsHierarchicalUriComponents() {
    String url = "https://example.com";
    UriComponents uric1 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bar={baz}").build();
    UriComponents uric2 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bar={baz}").build();
    UriComponents uric3 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bin={baz}").build();

    assertThat(uric1).isInstanceOf(HierarchicalUriComponents.class);
    assertThat(uric1).isEqualTo(uric1);
    assertThat(uric1).isEqualTo(uric2);
    assertThat(uric1).isNotEqualTo(uric3);
  }

  @Test
  public void equalsOpaqueUriComponents() {
    String baseUrl = "http:example.com";
    UriComponents uric1 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bar").build();
    UriComponents uric2 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bar").build();
    UriComponents uric3 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bin").build();

    assertThat(uric1).isInstanceOf(OpaqueUriComponents.class);
    assertThat(uric1).isEqualTo(uric1);
    assertThat(uric1).isEqualTo(uric2);
    assertThat(uric1).isNotEqualTo(uric3);
  }

}
