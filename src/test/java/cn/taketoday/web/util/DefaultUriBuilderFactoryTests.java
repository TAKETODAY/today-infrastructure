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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.web.util.DefaultUriBuilderFactory.EncodingMode;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultUriBuilderFactory}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultUriBuilderFactoryTests {

  @Test
  public void defaultSettings() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
    URI uri = factory.uriString("/foo/{id}").build("a/b");
    assertThat(uri.toString()).isEqualTo("/foo/a%2Fb");
  }

  @Test // SPR-17465
  public void defaultSettingsWithBuilder() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
    URI uri = factory.builder().path("/foo/{id}").build("a/b");
    assertThat(uri.toString()).isEqualTo("/foo/a%2Fb");
  }

  @Test
  public void baseUri() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("https://foo.example/v1?id=123");
    URI uri = factory.uriString("/bar").port(8080).build();
    assertThat(uri.toString()).isEqualTo("https://foo.example:8080/v1/bar?id=123");
  }

  @Test
  public void baseUriWithFullOverride() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("https://foo.example/v1?id=123");
    URI uri = factory.uriString("https://example.com/1/2").build();
    assertThat(uri.toString()).as("Use of host should case baseUri to be completely ignored").isEqualTo("https://example.com/1/2");
  }

  @Test
  public void baseUriWithPathOverride() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("https://foo.example/v1");
    URI uri = factory.builder().replacePath("/baz").build();
    assertThat(uri.toString()).isEqualTo("https://foo.example/baz");
  }

  @Test
  public void defaultUriVars() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("https://{host}/v1");
    factory.setDefaultUriVariables(singletonMap("host", "foo.example"));
    URI uri = factory.uriString("/{id}").build(singletonMap("id", "123"));
    assertThat(uri.toString()).isEqualTo("https://foo.example/v1/123");
  }

  @Test
  public void defaultUriVarsWithOverride() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("https://{host}/v1");
    factory.setDefaultUriVariables(singletonMap("host", "spring.io"));
    URI uri = factory.uriString("/bar").build(singletonMap("host", "docs.spring.io"));
    assertThat(uri.toString()).isEqualTo("https://docs.spring.io/v1/bar");
  }

  @Test
  public void defaultUriVarsWithEmptyVarArg() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("https://{host}/v1");
    factory.setDefaultUriVariables(singletonMap("host", "foo.example"));
    URI uri = factory.uriString("/bar").build();
    assertThat(uri.toString()).as("Expected delegation to build(Map) method").isEqualTo("https://foo.example/v1/bar");
  }

  @Test
  public void defaultUriVarsSpr14147() {
    Map<String, String> defaultUriVars = new HashMap<>(2);
    defaultUriVars.put("host", "api.example.com");
    defaultUriVars.put("port", "443");
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
    factory.setDefaultUriVariables(defaultUriVars);

    URI uri = factory.expand("https://{host}:{port}/v42/customers/{id}", singletonMap("id", 123L));
    assertThat(uri.toString()).isEqualTo("https://api.example.com:443/v42/customers/123");
  }

  @Test
  public void encodeTemplateAndValues() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
    factory.setEncodingMode(EncodingMode.TEMPLATE_AND_VALUES);
    UriBuilder uriBuilder = factory.uriString("/hotel list/{city} specials?q={value}");

    String expected = "/hotel%20list/Z%C3%BCrich%20specials?q=a%2Bb";

    Map<String, Object> vars = new HashMap<>();
    vars.put("city", "Z\u00fcrich");
    vars.put("value", "a+b");

    assertThat(uriBuilder.build("Z\u00fcrich", "a+b").toString()).isEqualTo(expected);
    assertThat(uriBuilder.build(vars).toString()).isEqualTo(expected);
  }

  @Test
  public void encodingValuesOnly() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
    factory.setEncodingMode(EncodingMode.VALUES_ONLY);
    UriBuilder uriBuilder = factory.uriString("/foo/a%2Fb/{id}");

    String id = "c/d";
    String expected = "/foo/a%2Fb/c%2Fd";

    assertThat(uriBuilder.build(id).toString()).isEqualTo(expected);
    assertThat(uriBuilder.build(singletonMap("id", id)).toString()).isEqualTo(expected);
  }

  @Test
  public void encodingValuesOnlySpr14147() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
    factory.setEncodingMode(EncodingMode.VALUES_ONLY);
    factory.setDefaultUriVariables(singletonMap("host", "www.example.com"));
    UriBuilder uriBuilder = factory.uriString("https://{host}/user/{userId}/dashboard");

    assertThat(uriBuilder.build(singletonMap("userId", "john;doe")).toString()).isEqualTo("https://www.example.com/user/john%3Bdoe/dashboard");
  }

  @Test
  public void encodingNone() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
    factory.setEncodingMode(EncodingMode.NONE);
    UriBuilder uriBuilder = factory.uriString("/foo/a%2Fb/{id}");

    String id = "c%2Fd";
    String expected = "/foo/a%2Fb/c%2Fd";

    assertThat(uriBuilder.build(id).toString()).isEqualTo(expected);
    assertThat(uriBuilder.build(singletonMap("id", id)).toString()).isEqualTo(expected);
  }

  @Test
  public void parsePathWithDefaultSettings() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("/foo/{bar}");
    URI uri = factory.uriString("/baz/{id}").build("a/b", "c/d");
    assertThat(uri.toString()).isEqualTo("/foo/a%2Fb/baz/c%2Fd");
  }

  @Test
  public void parsePathIsTurnedOff() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("/foo/{bar}");
    factory.setEncodingMode(EncodingMode.URI_COMPONENT);
    factory.setParsePath(false);
    URI uri = factory.uriString("/baz/{id}").build("a/b", "c/d");
    assertThat(uri.toString()).isEqualTo("/foo/a/b/baz/c/d");
  }

  @Test // SPR-15201
  public void pathWithTrailingSlash() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
    URI uri = factory.expand("https://localhost:8080/spring/");
    assertThat(uri.toString()).isEqualTo("https://localhost:8080/spring/");
  }

  @Test
  public void pathWithDuplicateSlashes() {
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
    URI uri = factory.expand("/foo/////////bar");
    assertThat(uri.toString()).isEqualTo("/foo/bar");
  }

}
