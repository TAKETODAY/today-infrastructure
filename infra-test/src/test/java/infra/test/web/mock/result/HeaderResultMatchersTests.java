/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
