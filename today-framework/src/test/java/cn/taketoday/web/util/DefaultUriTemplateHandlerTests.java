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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultUriTemplateHandler}.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("deprecation")
public class DefaultUriTemplateHandlerTests {

  private final DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();

  @Test
  public void baseUrlWithoutPath() throws Exception {
    this.handler.setBaseUrl("http://localhost:8080");
    URI actual = this.handler.expand("/myapiresource");

    assertThat(actual.toString()).isEqualTo("http://localhost:8080/myapiresource");
  }

  @Test
  public void baseUrlWithPath() throws Exception {
    this.handler.setBaseUrl("http://localhost:8080/context");
    URI actual = this.handler.expand("/myapiresource");

    assertThat(actual.toString()).isEqualTo("http://localhost:8080/context/myapiresource");
  }

  @Test  // SPR-14147
  public void defaultUriVariables() throws Exception {
    Map<String, String> defaultVars = new HashMap<>(2);
    defaultVars.put("host", "api.example.com");
    defaultVars.put("port", "443");
    this.handler.setDefaultUriVariables(defaultVars);

    Map<String, Object> vars = new HashMap<>(1);
    vars.put("id", 123L);

    String template = "https://{host}:{port}/v42/customers/{id}";
    URI actual = this.handler.expand(template, vars);

    assertThat(actual.toString()).isEqualTo("https://api.example.com:443/v42/customers/123");
  }

  @Test
  public void parsePathIsOff() throws Exception {
    this.handler.setParsePath(false);
    Map<String, String> vars = new HashMap<>(2);
    vars.put("hotel", "1");
    vars.put("publicpath", "pics/logo.png");
    String template = "https://example.com/hotels/{hotel}/pic/{publicpath}";
    URI actual = this.handler.expand(template, vars);

    assertThat(actual.toString()).isEqualTo("https://example.com/hotels/1/pic/pics/logo.png");
  }

  @Test
  public void parsePathIsOn() throws Exception {
    this.handler.setParsePath(true);
    Map<String, String> vars = new HashMap<>(2);
    vars.put("hotel", "1");
    vars.put("publicpath", "pics/logo.png");
    vars.put("scale", "150x150");
    String template = "https://example.com/hotels/{hotel}/pic/{publicpath}/size/{scale}";
    URI actual = this.handler.expand(template, vars);

    assertThat(actual.toString()).isEqualTo("https://example.com/hotels/1/pic/pics%2Flogo.png/size/150x150");
  }

  @Test
  public void strictEncodingIsOffWithMap() throws Exception {
    this.handler.setStrictEncoding(false);
    Map<String, String> vars = new HashMap<>(2);
    vars.put("userId", "john;doe");
    String template = "https://www.example.com/user/{userId}/dashboard";
    URI actual = this.handler.expand(template, vars);

    assertThat(actual.toString()).isEqualTo("https://www.example.com/user/john;doe/dashboard");
  }

  @Test
  public void strictEncodingOffWithArray() throws Exception {
    this.handler.setStrictEncoding(false);
    String template = "https://www.example.com/user/{userId}/dashboard";
    URI actual = this.handler.expand(template, "john;doe");

    assertThat(actual.toString()).isEqualTo("https://www.example.com/user/john;doe/dashboard");
  }

  @Test
  public void strictEncodingOnWithMap() throws Exception {
    this.handler.setStrictEncoding(true);
    Map<String, String> vars = new HashMap<>(2);
    vars.put("userId", "john;doe");
    String template = "https://www.example.com/user/{userId}/dashboard";
    URI actual = this.handler.expand(template, vars);

    assertThat(actual.toString()).isEqualTo("https://www.example.com/user/john%3Bdoe/dashboard");
  }

  @Test
  public void strictEncodingOnWithArray() throws Exception {
    this.handler.setStrictEncoding(true);
    String template = "https://www.example.com/user/{userId}/dashboard";
    URI actual = this.handler.expand(template, "john;doe");

    assertThat(actual.toString()).isEqualTo("https://www.example.com/user/john%3Bdoe/dashboard");
  }

  @Test  // SPR-14147
  public void strictEncodingAndDefaultUriVariables() throws Exception {
    Map<String, String> defaultVars = new HashMap<>(1);
    defaultVars.put("host", "www.example.com");
    this.handler.setDefaultUriVariables(defaultVars);
    this.handler.setStrictEncoding(true);

    Map<String, Object> vars = new HashMap<>(1);
    vars.put("userId", "john;doe");

    String template = "https://{host}/user/{userId}/dashboard";
    URI actual = this.handler.expand(template, vars);

    assertThat(actual.toString()).isEqualTo("https://www.example.com/user/john%3Bdoe/dashboard");
  }

}
