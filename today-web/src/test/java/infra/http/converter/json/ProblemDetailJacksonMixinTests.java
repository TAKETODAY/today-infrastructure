/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.converter.json;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.net.URI;

import infra.http.HttpStatus;
import infra.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/9 10:36
 */
class ProblemDetailJacksonMixinTests {

  private final ObjectMapper mapper = JsonMapper.builder().addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class).build();

  @Test
  void writeStatusAndHeaders() throws Exception {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Missing header");
    testWrite(detail,
            """
                    {
                      "title": "Bad Request",
                      "status": 400,
                      "detail": "Missing header"
                    }""");
  }

  @Test
  void writeCustomProperty() throws Exception {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Missing header");
    detail.setProperty("host", "abc.org");
    detail.setProperty("user", null);

    testWrite(detail, """
            {
            	"title": "Bad Request",
            	"status": 400,
            	"detail": "Missing header",
            	"host": "abc.org",
            	"user": null
            }""");
  }

  @Test
  void readCustomProperty() {
    ProblemDetail detail = this.mapper.readValue("""
            {
            	"type": "about:blank",
            	"title": "Bad Request",
            	"status": 400,
            	"detail": "Missing header",
            	"host": "abc.org",
            	"user": null
            }""", ProblemDetail.class);

    assertThat(detail.getType()).isEqualTo(URI.create("about:blank"));
    assertThat(detail.getTitle()).isEqualTo("Bad Request");
    assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(detail.getDetail()).isEqualTo("Missing header");
    assertThat(detail.getProperties())
            .containsEntry("host", "abc.org")
            .containsEntry("user", null);
  }

  @Test
  void readCustomPropertyFromXml() {
    ObjectMapper xmlMapper = XmlMapper.builder().addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class).build();
    ProblemDetail detail = xmlMapper.readValue("""
            <problem xmlns="urn:ietf:rfc:7807">
            	<type>about:blank</type>
            	<title>Bad Request</title>
            	<status>400</status>
            	<detail>Missing header</detail>
            	<host>abc.org</host>
            </problem>""", ProblemDetail.class);

    assertThat(detail.getType()).isEqualTo(URI.create("about:blank"));
    assertThat(detail.getTitle()).isEqualTo("Bad Request");
    assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(detail.getDetail()).isEqualTo("Missing header");
    assertThat(detail.getProperties()).containsEntry("host", "abc.org");
  }

  private void testWrite(ProblemDetail problemDetail, String expected) throws JSONException {
    String output = this.mapper.writeValueAsString(problemDetail);
    JSONAssert.assertEquals(expected, output, false);
  }

}
