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

package cn.taketoday.http.converter.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.net.URI;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/9 10:36
 */
class ProblemDetailJacksonMixinTests {

  private final ObjectMapper mapper = new Jackson2ObjectMapperBuilder().build();

  @Test
  void writeStatusAndHeaders() throws Exception {
    testWrite(
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Missing header"),
            "{\"type\":\"about:blank\"," +
                    "\"title\":\"Bad Request\"," +
                    "\"status\":400," +
                    "\"detail\":\"Missing header\"}");
  }

  @Test
  void writeCustomProperty() throws Exception {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Missing header");
    problemDetail.setProperty("host", "abc.org");

    testWrite(problemDetail,
            "{\"type\":\"about:blank\"," +
                    "\"title\":\"Bad Request\"," +
                    "\"status\":400," +
                    "\"detail\":\"Missing header\"," +
                    "\"host\":\"abc.org\"}");
  }

  @Test
  void readCustomProperty() throws Exception {
    ProblemDetail problemDetail = this.mapper.readValue(
            "{\"type\":\"about:blank\"," +
                    "\"title\":\"Bad Request\"," +
                    "\"status\":400," +
                    "\"detail\":\"Missing header\"," +
                    "\"host\":\"abc.org\"}", ProblemDetail.class);

    assertThat(problemDetail.getType()).isEqualTo(URI.create("about:blank"));
    assertThat(problemDetail.getTitle()).isEqualTo("Bad Request");
    assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problemDetail.getDetail()).isEqualTo("Missing header");
    assertThat(problemDetail.getProperties()).containsEntry("host", "abc.org");
  }

  private void testWrite(ProblemDetail problemDetail, String expected) throws Exception {
    String output = this.mapper.writeValueAsString(problemDetail);
    assertThat(output).isEqualTo(expected);
  }

}
