/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/3 12:37
 */
class ProblemDetailTests {

  @Test
  void equalsAndHashCode() {
    ProblemDetail pd1 = ProblemDetail.forRawStatusCode(500);
    ProblemDetail pd2 = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    ProblemDetail pd3 = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    ProblemDetail pd4 = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "some detail");

    assertThat(pd1).isEqualTo(pd2);
    assertThat(pd2).isEqualTo(pd1);
    assertThat(pd1.hashCode()).isEqualTo(pd2.hashCode());

    assertThat(pd3).isNotEqualTo(pd4);
    assertThat(pd4).isNotEqualTo(pd3);
    assertThat(pd3.hashCode()).isNotEqualTo(pd4.hashCode());

    assertThat(pd1).isNotEqualTo(pd3);
    assertThat(pd1).isNotEqualTo(pd4);
    assertThat(pd2).isNotEqualTo(pd3);
    assertThat(pd2).isNotEqualTo(pd4);
    assertThat(pd1.hashCode()).isNotEqualTo(pd3.hashCode());
    assertThat(pd1.hashCode()).isNotEqualTo(pd4.hashCode());
  }

  @Test
    // gh-30294
  void equalsAndHashCodeWithDeserialization() throws Exception {
    ProblemDetail originalDetail = ProblemDetail.forRawStatusCode(500);

    ObjectMapper mapper = new ObjectMapper();
    byte[] bytes = mapper.writeValueAsBytes(originalDetail);
    ProblemDetail deserializedDetail = mapper.readValue(bytes, ProblemDetail.class);

    assertThat(originalDetail).isEqualTo(deserializedDetail);
    assertThat(deserializedDetail).isEqualTo(originalDetail);
    assertThat(originalDetail.hashCode()).isEqualTo(deserializedDetail.hashCode());
  }
}