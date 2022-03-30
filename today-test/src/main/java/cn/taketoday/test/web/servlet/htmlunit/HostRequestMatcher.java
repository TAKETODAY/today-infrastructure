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

package cn.taketoday.test.web.servlet.htmlunit;

import com.gargoylesoftware.htmlunit.WebRequest;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link WebRequestMatcher} that allows matching on the host and optionally
 * the port of {@code WebRequest#getUrl()}.
 *
 * <p>For example, the following would match any request to the host
 * {@code "code.jquery.com"} without regard for the port.
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com");</pre>
 *
 * <p>Multiple hosts can also be passed in. For example, the following would
 * match any request to the host {@code "code.jquery.com"} or the host
 * {@code "cdn.com"} without regard for the port.
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com", "cdn.com");</pre>
 *
 * <p>Alternatively, one can also specify the port. For example, the following would match
 * any request to the host {@code "code.jquery.com"} with the port of {@code 80}.
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.example:80");</pre>
 *
 * <p>The above {@code cdnMatcher} would match {@code "http://code.jquery.example/jquery.js"}
 * which has a default port of {@code 80} and {@code "http://code.jquery.example:80/jquery.js"}.
 * However, it would not match {@code "https://code.jquery.example/jquery.js"}
 * which has a default port of {@code 443}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @see UrlRegexRequestMatcher
 * @see DelegatingWebConnection
 * @since 4.2
 */
public final class HostRequestMatcher implements WebRequestMatcher {

  private final Set<String> hosts = new HashSet<>();

  /**
   * Create a new {@code HostRequestMatcher} for the given hosts &mdash;
   * for example: {@code "localhost"}, {@code "example.com:443"}, etc.
   *
   * @param hosts the hosts to match on
   */
  public HostRequestMatcher(String... hosts) {
    Collections.addAll(this.hosts, hosts);
  }

  @Override
  public boolean matches(WebRequest request) {
    URL url = request.getUrl();
    String host = url.getHost();

    if (this.hosts.contains(host)) {
      return true;
    }

    int port = url.getPort();
    if (port == -1) {
      port = url.getDefaultPort();
    }
    String hostAndPort = host + ":" + port;

    return this.hosts.contains(hostAndPort);
  }

}
