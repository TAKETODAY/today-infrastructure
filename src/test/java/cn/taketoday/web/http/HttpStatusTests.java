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

package cn.taketoday.web.http;

import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author TODAY 2021/4/15 14:27
 */
public class HttpStatusTests {
  private final Map<Integer, String> statusCodes = new LinkedHashMap<>();

  @Before
  public void createStatusCodes() {
    statusCodes.put(100, "CONTINUE");
    statusCodes.put(101, "SWITCHING_PROTOCOLS");
    statusCodes.put(102, "PROCESSING");
    statusCodes.put(103, "CHECKPOINT");

    statusCodes.put(200, "OK");
    statusCodes.put(201, "CREATED");
    statusCodes.put(202, "ACCEPTED");
    statusCodes.put(203, "NON_AUTHORITATIVE_INFORMATION");
    statusCodes.put(204, "NO_CONTENT");
    statusCodes.put(205, "RESET_CONTENT");
    statusCodes.put(206, "PARTIAL_CONTENT");
    statusCodes.put(207, "MULTI_STATUS");
    statusCodes.put(208, "ALREADY_REPORTED");
    statusCodes.put(226, "IM_USED");

    statusCodes.put(300, "MULTIPLE_CHOICES");
    statusCodes.put(301, "MOVED_PERMANENTLY");
    statusCodes.put(302, "FOUND");
    statusCodes.put(303, "SEE_OTHER");
    statusCodes.put(304, "NOT_MODIFIED");
    statusCodes.put(305, "USE_PROXY");
    statusCodes.put(307, "TEMPORARY_REDIRECT");
    statusCodes.put(308, "PERMANENT_REDIRECT");

    statusCodes.put(400, "BAD_REQUEST");
    statusCodes.put(401, "UNAUTHORIZED");
    statusCodes.put(402, "PAYMENT_REQUIRED");
    statusCodes.put(403, "FORBIDDEN");
    statusCodes.put(404, "NOT_FOUND");
    statusCodes.put(405, "METHOD_NOT_ALLOWED");
    statusCodes.put(406, "NOT_ACCEPTABLE");
    statusCodes.put(407, "PROXY_AUTHENTICATION_REQUIRED");
    statusCodes.put(408, "REQUEST_TIMEOUT");
    statusCodes.put(409, "CONFLICT");
    statusCodes.put(410, "GONE");
    statusCodes.put(411, "LENGTH_REQUIRED");
    statusCodes.put(412, "PRECONDITION_FAILED");
    statusCodes.put(413, "PAYLOAD_TOO_LARGE");
    statusCodes.put(414, "URI_TOO_LONG");
    statusCodes.put(415, "UNSUPPORTED_MEDIA_TYPE");
    statusCodes.put(416, "REQUESTED_RANGE_NOT_SATISFIABLE");
    statusCodes.put(417, "EXPECTATION_FAILED");
    statusCodes.put(418, "I_AM_A_TEAPOT");
    statusCodes.put(419, "INSUFFICIENT_SPACE_ON_RESOURCE");
    statusCodes.put(420, "METHOD_FAILURE");
    statusCodes.put(421, "DESTINATION_LOCKED");
    statusCodes.put(422, "UNPROCESSABLE_ENTITY");
    statusCodes.put(423, "LOCKED");
    statusCodes.put(424, "FAILED_DEPENDENCY");
    statusCodes.put(425, "TOO_EARLY");
    statusCodes.put(426, "UPGRADE_REQUIRED");
    statusCodes.put(428, "PRECONDITION_REQUIRED");
    statusCodes.put(429, "TOO_MANY_REQUESTS");
    statusCodes.put(431, "REQUEST_HEADER_FIELDS_TOO_LARGE");
    statusCodes.put(451, "UNAVAILABLE_FOR_LEGAL_REASONS");

    statusCodes.put(500, "INTERNAL_SERVER_ERROR");
    statusCodes.put(501, "NOT_IMPLEMENTED");
    statusCodes.put(502, "BAD_GATEWAY");
    statusCodes.put(503, "SERVICE_UNAVAILABLE");
    statusCodes.put(504, "GATEWAY_TIMEOUT");
    statusCodes.put(505, "HTTP_VERSION_NOT_SUPPORTED");
    statusCodes.put(506, "VARIANT_ALSO_NEGOTIATES");
    statusCodes.put(507, "INSUFFICIENT_STORAGE");
    statusCodes.put(508, "LOOP_DETECTED");
    statusCodes.put(509, "BANDWIDTH_LIMIT_EXCEEDED");
    statusCodes.put(510, "NOT_EXTENDED");
    statusCodes.put(511, "NETWORK_AUTHENTICATION_REQUIRED");
  }

  @Test
  public void fromMapToEnum() {
    for (Map.Entry<Integer, String> entry : statusCodes.entrySet()) {
      int value = entry.getKey();
      HttpStatus status = HttpStatus.valueOf(value);
      assertThat(status.value()).as("Invalid value").isEqualTo(value);
      assertThat(status.name()).as("Invalid name for [" + value + "]").isEqualTo(entry.getValue());
    }
  }

  @Test
  public void fromEnumToMap() {
    for (HttpStatus status : HttpStatus.values()) {
      int code = status.value();
      // The following status codes have more than one corresponding HttpStatus enum constant.
      if (code == 302 || code == 413 || code == 414) {
        continue;
      }
      assertThat(statusCodes).as("Map has no value for [" + code + "]").containsKey(code);
      assertThat(status.name()).as("Invalid name for [" + code + "]").isEqualTo(statusCodes.get(code));
    }
  }

  @Test
  public void allStatusSeriesShouldMatchExpectations() {
    // The Series of an HttpStatus is set manually, so we make sure it is the correct one.
    for (HttpStatus status : HttpStatus.values()) {
      HttpStatus.Series expectedSeries = HttpStatus.Series.valueOf(status.value());
      assertThat(status.series()).isEqualTo(expectedSeries);
    }
  }

}
