/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.test.web.mock.result;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import infra.http.HttpHeaders;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.test.web.mock.MvcResult;
import infra.test.web.mock.StubMvcResult;

/**
 * Unit tests for {@link HeaderResultMatchers}.
 *
 * @author Rossen Stoyanchev
 */
public class HeaderResultMatchersTests {

  private final HeaderResultMatchers matchers = new HeaderResultMatchers();

  private final MockHttpResponseImpl response = new MockHttpResponseImpl();

  private final MvcResult mvcResult =
          new StubMvcResult(new HttpMockRequestImpl(), null, null, null, null, null, this.response);

  @Test
  public void matchDateFormattedWithHttpHeaders() throws Exception {

    long epochMilli = ZonedDateTime.of(2018, 10, 5, 0, 0, 0, 0, ZoneId.of("GMT")).toInstant().toEpochMilli();
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setDate("myDate", epochMilli);
    this.response.setHeader("d", headers.getFirst("myDate"));

    this.matchers.dateValue("d", epochMilli).match(this.mvcResult);
  }

}
