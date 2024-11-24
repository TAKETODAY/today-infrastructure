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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import infra.core.Conventions;
import infra.http.HttpStatus;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.test.web.mock.MvcResult;
import infra.test.web.mock.ResultMatcher;
import infra.test.web.mock.StubMvcResult;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link StatusResultMatchers}.
 *
 * @author Rossen Stoyanchev
 */
public class StatusResultMatchersTests {

  private final StatusResultMatchers matchers = new StatusResultMatchers();

  private final HttpMockRequestImpl request = new HttpMockRequestImpl();

  @Test
  public void testHttpStatusCodeResultMatchers() throws Exception {
    List<AssertionError> failures = new ArrayList<>();
    for (HttpStatus status : HttpStatus.values()) {
      MockHttpResponseImpl response = new MockHttpResponseImpl();
      response.setStatus(status.value());
      MvcResult mvcResult = new StubMvcResult(request, null, null, null, null, null, response);
      try {
        Method method = getMethodForHttpStatus(status);
        ResultMatcher matcher = (ResultMatcher) ReflectionUtils.invokeMethod(method, this.matchers);
        try {
          matcher.match(mvcResult);
        }
        catch (AssertionError error) {
          failures.add(error);
        }
      }
      catch (Exception ex) {
        throw new Exception("Failed to obtain ResultMatcher for status " + status, ex);
      }
    }
    if (!failures.isEmpty()) {
      fail("Failed status codes: " + failures);
    }
  }

  private Method getMethodForHttpStatus(HttpStatus status) throws NoSuchMethodException {
    String name = status.name().toLowerCase().replace("_", "-");
    name = "is" + StringUtils.capitalize(Conventions.attributeNameToPropertyName(name));
    return StatusResultMatchers.class.getMethod(name);
  }

  @Test
  public void statusRanges() throws Exception {
    for (HttpStatus status : HttpStatus.values()) {
      MockHttpResponseImpl response = new MockHttpResponseImpl();
      response.setStatus(status.value());
      MvcResult mvcResult = new StubMvcResult(request, null, null, null, null, null, response);
      switch (status.series().value()) {
        case 1 -> this.matchers.is1xxInformational().match(mvcResult);
        case 2 -> this.matchers.is2xxSuccessful().match(mvcResult);
        case 3 -> this.matchers.is3xxRedirection().match(mvcResult);
        case 4 -> this.matchers.is4xxClientError().match(mvcResult);
        case 5 -> this.matchers.is5xxServerError().match(mvcResult);
        default -> fail("Unexpected range for status code value " + status);
      }
    }
  }

}
