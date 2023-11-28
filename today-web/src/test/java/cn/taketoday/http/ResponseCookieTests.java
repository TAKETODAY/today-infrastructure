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

package cn.taketoday.http;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ResponseCookie}.
 *
 * @author Rossen Stoyanchev
 */
public class ResponseCookieTests {

  @Test
  public void basic() {

    assertThat(ResponseCookie.from("id", null).build().toString()).isEqualTo("id=");
    assertThat(ResponseCookie.from("id", "1fWa").build().toString()).isEqualTo("id=1fWa");

    ResponseCookie cookie = ResponseCookie.from("id", "1fWa")
            .domain("abc").path("/path").maxAge(0).httpOnly(true).secure(true).sameSite("None")
            .build();

    assertThat(cookie.toString()).isEqualTo("id=1fWa; Path=/path; Domain=abc; " +
            "Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; " +
            "Secure; HttpOnly; SameSite=None");
  }

  @Test
  public void nameChecks() {

    Arrays.asList("id", "i.d.", "i-d", "+id", "i*d", "i$d", "#id")
            .forEach(name -> ResponseCookie.from(name, "value").build());

    Arrays.asList("\"id\"", "id\t", "i\td", "i d", "i;d", "{id}", "[id]", "\"", "id\u0091")
            .forEach(name -> assertThatThrownBy(() -> ResponseCookie.from(name, "value").build())
                    .hasMessageContaining("RFC2616 token"));
  }

  @Test
  public void valueChecks() {

    Arrays.asList("1fWa", "", null, "1f=Wa", "1f-Wa", "1f/Wa", "1.f.W.a.")
            .forEach(value -> ResponseCookie.from("id", value).build());

    Arrays.asList("1f\tWa", "\t", "1f Wa", "1f;Wa", "\"1fWa", "1f\\Wa", "1f\"Wa", "\"", "1fWa\u0005", "1f\u0091Wa")
            .forEach(value -> assertThatThrownBy(() -> ResponseCookie.from("id", value).build())
                    .hasMessageContaining("RFC2616 cookie value"));
  }

  @Test
  public void domainChecks() {

    Arrays.asList("abc", "abc.org", "abc-def.org", "abc3.org", ".abc.org")
            .forEach(domain -> ResponseCookie.from("n", "v").domain(domain).build());

    Arrays.asList("-abc.org", "abc.org.", "abc.org-")
            .forEach(domain -> assertThatThrownBy(() -> ResponseCookie.from("n", "v").domain(domain).build())
                    .hasMessageContaining("Invalid first/last char"));

    Arrays.asList("abc..org", "abc.-org", "abc-.org")
            .forEach(domain -> assertThatThrownBy(() -> ResponseCookie.from("n", "v").domain(domain).build())
                    .hasMessageContaining("invalid cookie domain char"));
  }

  @Test // gh-24663
  public void domainWithEmptyDoubleQuotes() {

    Arrays.asList("\"\"", "\t\"\" ", " \" \t \"\t")
            .forEach(domain -> {
              ResponseCookie cookie = ResponseCookie.fromClientResponse("id", "1fWa").domain(domain).build();
              assertThat(cookie.getDomain()).isNull();
            });

  }
}
