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

package cn.taketoday.web.util;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class WebUtilsTests {

  @Test
  public void parseMatrixVariablesString() {
    MultiValueMap<String, String> variables;

    variables = WebUtils.parseMatrixVariables(null);
    assertThat(variables).hasSize(0);

    variables = WebUtils.parseMatrixVariables("year");
    assertThat(variables).hasSize(1);
    assertThat(variables.getFirst("year")).isEqualTo("");

    variables = WebUtils.parseMatrixVariables("year=2012");
    assertThat(variables).hasSize(1);
    assertThat(variables.getFirst("year")).isEqualTo("2012");

    variables = WebUtils.parseMatrixVariables("year=2012;colors=red,blue,green");
    assertThat(variables).hasSize(2);
    assertThat(variables.get("colors")).containsExactly("red", "blue", "green");
    assertThat(variables.getFirst("year")).isEqualTo("2012");

    variables = WebUtils.parseMatrixVariables(";year=2012;colors=red,blue,green;");
    assertThat(variables).hasSize(2);
    assertThat(variables.get("colors")).containsExactly("red", "blue", "green");
    assertThat(variables.getFirst("year")).isEqualTo("2012");

    variables = WebUtils.parseMatrixVariables("colors=red;colors=blue;colors=green");
    assertThat(variables).hasSize(1);
    assertThat(variables.get("colors")).containsExactly("red", "blue", "green");

    variables = WebUtils.parseMatrixVariables("jsessionid=c0o7fszeb1");
    assertThat(variables).isEmpty();

    variables = WebUtils.parseMatrixVariables("a=b;jsessionid=c0o7fszeb1;c=d");
    assertThat(variables).hasSize(2);
    assertThat(variables.get("a")).containsExactly("b");
    assertThat(variables.get("c")).containsExactly("d");

    variables = WebUtils.parseMatrixVariables("a=b;jsessionid=c0o7fszeb1;c=d");
    assertThat(variables).hasSize(2);
    assertThat(variables.get("a")).containsExactly("b");
    assertThat(variables.get("c")).containsExactly("d");
  }

  @Test
  public void isValidOrigin() {
    List<String> allowed = Collections.emptyList();
    assertThat(checkValidOrigin("mydomain1.example", -1, "http://mydomain1.example", allowed)).isTrue();
    assertThat(checkValidOrigin("mydomain1.example", -1, "http://mydomain2.example", allowed)).isFalse();

    allowed = Collections.singletonList("*");
    assertThat(checkValidOrigin("mydomain1.example", -1, "http://mydomain2.example", allowed)).isTrue();

    allowed = Collections.singletonList("http://mydomain1.example");
    assertThat(checkValidOrigin("mydomain2.example", -1, "http://mydomain1.example", allowed)).isTrue();
    assertThat(checkValidOrigin("mydomain2.example", -1, "http://mydomain3.example", allowed)).isFalse();
  }

  @Test
  public void isSameOrigin() {
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example")).isTrue();
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example:80")).isTrue();
    assertThat(checkSameOrigin("https", "mydomain1.example", 443, "https://mydomain1.example")).isTrue();
    assertThat(checkSameOrigin("https", "mydomain1.example", 443, "https://mydomain1.example:443")).isTrue();
    assertThat(checkSameOrigin("http", "mydomain1.example", 123, "http://mydomain1.example:123")).isTrue();
    assertThat(checkSameOrigin("ws", "mydomain1.example", -1, "ws://mydomain1.example")).isTrue();
    assertThat(checkSameOrigin("wss", "mydomain1.example", 443, "wss://mydomain1.example")).isTrue();

    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain2.example")).isFalse();
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "https://mydomain1.example")).isFalse();
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "invalid-origin")).isFalse();
    assertThat(checkSameOrigin("https", "mydomain1.example", -1, "http://mydomain1.example")).isFalse();

    // Handling of invalid origins as described in SPR-13478
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example/")).isTrue();
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example:80/")).isTrue();
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example/path")).isTrue();
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example:80/path")).isTrue();
    assertThat(checkSameOrigin("http", "mydomain2.example", -1, "http://mydomain1.example/")).isFalse();
    assertThat(checkSameOrigin("http", "mydomain2.example", -1, "http://mydomain1.example:80/")).isFalse();
    assertThat(checkSameOrigin("http", "mydomain2.example", -1, "http://mydomain1.example/path")).isFalse();
    assertThat(checkSameOrigin("http", "mydomain2.example", -1, "http://mydomain1.example:80/path")).isFalse();

    // Handling of IPv6 hosts as described in SPR-13525
    assertThat(checkSameOrigin("http", "[::1]", -1, "http://[::1]")).isTrue();
    assertThat(checkSameOrigin("http", "[::1]", 8080, "http://[::1]:8080")).isTrue();
    assertThat(checkSameOrigin("http",
            "[2001:0db8:0000:85a3:0000:0000:ac1f:8001]", -1,
            "http://[2001:0db8:0000:85a3:0000:0000:ac1f:8001]")).isTrue();
    assertThat(checkSameOrigin("http",
            "[2001:0db8:0000:85a3:0000:0000:ac1f:8001]", 8080,
            "http://[2001:0db8:0000:85a3:0000:0000:ac1f:8001]:8080")).isTrue();
    assertThat(checkSameOrigin("http", "[::1]", -1, "http://[::1]:8080")).isFalse();
    assertThat(checkSameOrigin("http", "[::1]", 8080,
            "http://[2001:0db8:0000:85a3:0000:0000:ac1f:8001]:8080")).isFalse();
  }

  @Test  // SPR-16262
  public void isSameOriginWithXForwardedHeaders() throws Exception {
    String server = "mydomain1.example";
    testWithXForwardedHeaders(server, -1, "https", null, -1, "https://mydomain1.example");
    testWithXForwardedHeaders(server, 123, "https", null, -1, "https://mydomain1.example");
    testWithXForwardedHeaders(server, -1, "https", "mydomain2.example", -1, "https://mydomain2.example");
    testWithXForwardedHeaders(server, 123, "https", "mydomain2.example", -1, "https://mydomain2.example");
    testWithXForwardedHeaders(server, -1, "https", "mydomain2.example", 456, "https://mydomain2.example:456");
    testWithXForwardedHeaders(server, 123, "https", "mydomain2.example", 456, "https://mydomain2.example:456");
  }

  @Test  // SPR-16262
  public void isSameOriginWithForwardedHeader() throws Exception {
    String server = "mydomain1.example";
    testWithForwardedHeader(server, -1, "proto=https", "https://mydomain1.example");
    testWithForwardedHeader(server, 123, "proto=https", "https://mydomain1.example");
    testWithForwardedHeader(server, -1, "proto=https; host=mydomain2.example", "https://mydomain2.example");
    testWithForwardedHeader(server, 123, "proto=https; host=mydomain2.example", "https://mydomain2.example");
    testWithForwardedHeader(server, -1, "proto=https; host=mydomain2.example:456", "https://mydomain2.example:456");
    testWithForwardedHeader(server, 123, "proto=https; host=mydomain2.example:456", "https://mydomain2.example:456");
  }

  private boolean checkValidOrigin(String serverName, int port, String originHeader, List<String> allowed) {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setServerName(serverName);
    if (port != -1) {
      servletRequest.setServerPort(port);
    }
    servletRequest.addHeader(HttpHeaders.ORIGIN, originHeader);
    ServletRequestContext context = new ServletRequestContext(null, servletRequest, new MockHttpServletResponse());
    return WebUtils.isValidOrigin(context, allowed);
  }

  private boolean checkSameOrigin(String scheme, String serverName, int port, String originHeader) {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setScheme(scheme);
    servletRequest.setServerName(serverName);
    if (port != -1) {
      servletRequest.setServerPort(port);
    }
    servletRequest.addHeader(HttpHeaders.ORIGIN, originHeader);

    ServletRequestContext context = new ServletRequestContext(null, servletRequest, new MockHttpServletResponse());
    return WebUtils.isSameOrigin(context);
  }

  private void testWithXForwardedHeaders(String serverName, int port, String forwardedProto,
          String forwardedHost, int forwardedPort, String originHeader) throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setServerName(serverName);
    if (port != -1) {
      request.setServerPort(port);
    }
    if (forwardedProto != null) {
      request.addHeader("X-Forwarded-Proto", forwardedProto);
    }
    if (forwardedHost != null) {
      request.addHeader("X-Forwarded-Host", forwardedHost);
    }
    if (forwardedPort != -1) {
      request.addHeader("X-Forwarded-Port", String.valueOf(forwardedPort));
    }
    request.addHeader(HttpHeaders.ORIGIN, originHeader);

//    HttpServletRequest requestToUse = adaptFromForwardedHeaders(request);
//    ServerHttpRequest httpRequest = new ServletServerHttpRequest(requestToUse);
//
//    assertThat(WebUtils.isSameOrigin(httpRequest)).isTrue();
  }

  private void testWithForwardedHeader(String serverName, int port, String forwardedHeader,
          String originHeader) throws Exception {

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setServerName(serverName);
    if (port != -1) {
      request.setServerPort(port);
    }
    request.addHeader("Forwarded", forwardedHeader);
    request.addHeader(HttpHeaders.ORIGIN, originHeader);

//    HttpServletRequest requestToUse = adaptFromForwardedHeaders(request);
//    ServerHttpRequest httpRequest = new ServletServerHttpRequest(requestToUse);

  }

}
