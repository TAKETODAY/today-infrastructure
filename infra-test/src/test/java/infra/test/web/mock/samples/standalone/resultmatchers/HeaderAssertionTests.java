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

package infra.test.web.mock.samples.standalone.resultmatchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import infra.http.HttpHeaders;
import infra.http.ResponseEntity;
import infra.stereotype.Controller;
import infra.test.web.Person;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.ResultMatcher;
import infra.web.RequestContext;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestMapping;

import static infra.http.HttpHeaders.IF_MODIFIED_SINCE;
import static infra.http.HttpHeaders.LAST_MODIFIED;
import static infra.http.HttpHeaders.VARY;
import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.header;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * Examples of expectations on response header values.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 */
public class HeaderAssertionTests {

  private static final String ERROR_MESSAGE = "Should have thrown an AssertionError";

  private String now;

  private String minuteAgo;

  private MockMvc mockMvc;

  private final long currentTime = System.currentTimeMillis();

  private SimpleDateFormat dateFormat;

  @BeforeEach
  public void setup() {
    this.dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    this.now = dateFormat.format(new Date(this.currentTime));
    this.minuteAgo = dateFormat.format(new Date(this.currentTime - (1000 * 60)));

    PersonController controller = new PersonController();
    controller.setStubTimestamp(this.currentTime);
    this.mockMvc = standaloneSetup(controller).build();
  }

  @Test
  public void stringWithCorrectResponseHeaderValue() throws Exception {
    this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, minuteAgo))
            .andExpect(header().string(LAST_MODIFIED, now));
  }

  @Test
  public void stringWithMatcherAndCorrectResponseHeaderValue() throws Exception {
    this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, minuteAgo))
            .andExpect(header().string(LAST_MODIFIED, equalTo(now)));
  }

  @Test
  public void multiStringHeaderValue() throws Exception {
    this.mockMvc.perform(get("/persons/1")).andExpect(header().stringValues(VARY, "foo", "bar"));
  }

  @Test
  public void multiStringHeaderValueWithMatchers() throws Exception {
    this.mockMvc.perform(get("/persons/1"))
            .andExpect(header().stringValues(VARY, hasItems(containsString("foo"), startsWith("bar"))));
  }

  @Test
  public void dateValueWithCorrectResponseHeaderValue() throws Exception {
    this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, minuteAgo))
            .andExpect(header().dateValue(LAST_MODIFIED, this.currentTime));
  }

  @Test
  public void longValueWithCorrectResponseHeaderValue() throws Exception {
    this.mockMvc.perform(get("/persons/1"))
            .andExpect(header().longValue("X-Rate-Limiting", 42));
  }

  @Test
  public void stringWithMissingResponseHeader() throws Exception {
    this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, now))
            .andExpect(status().isNotModified())
            .andExpect(header().stringValues("X-Custom-Header"));
  }

  @Test
  public void stringWithMatcherAndMissingResponseHeader() throws Exception {
    this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, now))
            .andExpect(status().isNotModified())
            .andExpect(header().string("X-Custom-Header", nullValue()));
  }

  @Test
  public void longValueWithMissingResponseHeader() throws Exception {
    try {
      this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, now))
              .andExpect(status().isNotModified())
              .andExpect(header().longValue("X-Custom-Header", 99L));

      fail(ERROR_MESSAGE);
    }
    catch (AssertionError err) {
      if (ERROR_MESSAGE.equals(err.getMessage())) {
        throw err;
      }
      assertThat(err.getMessage()).isEqualTo("Response does not contain header 'X-Custom-Header'");
    }
  }

  @Test
  public void exists() throws Exception {
    this.mockMvc.perform(get("/persons/1")).andExpect(header().exists(LAST_MODIFIED));
  }

  @Test
  public void existsFail() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            this.mockMvc.perform(get("/persons/1")).andExpect(header().exists("X-Custom-Header")));
  }

  @Test
  public void doesNotExist() throws Exception {
    this.mockMvc.perform(get("/persons/1")).andExpect(header().doesNotExist("X-Custom-Header"));
  }

  @Test
  public void doesNotExistFail() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            this.mockMvc.perform(get("/persons/1")).andExpect(header().doesNotExist(LAST_MODIFIED)));
  }

  @Test
  public void longValueWithIncorrectResponseHeaderValue() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            this.mockMvc.perform(get("/persons/1")).andExpect(header().longValue("X-Rate-Limiting", 1)));
  }

  @Test
  public void stringWithMatcherAndIncorrectResponseHeaderValue() throws Exception {
    long secondLater = this.currentTime + 1000;
    String expected = this.dateFormat.format(new Date(secondLater));
    assertIncorrectResponseHeader(header().string(LAST_MODIFIED, expected), expected);
    assertIncorrectResponseHeader(header().string(LAST_MODIFIED, equalTo(expected)), expected);
    // Comparison by date uses HttpHeaders to format the date in the error message.
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setDate("expected", secondLater);
    assertIncorrectResponseHeader(header().dateValue(LAST_MODIFIED, secondLater), headers.getFirst("expected"));
  }

  private void assertIncorrectResponseHeader(ResultMatcher matcher, String expected) throws Exception {
    try {
      this.mockMvc.perform(get("/persons/1")
                      .header(IF_MODIFIED_SINCE, minuteAgo))
              .andExpect(matcher);

      fail(ERROR_MESSAGE);
    }
    catch (AssertionError err) {
      if (ERROR_MESSAGE.equals(err.getMessage())) {
        throw err;
      }
      // : ensure header name is in the message
      // Unfortunately, we can't control formatting from JUnit or Hamcrest.
      assertMessageContains(err, "Response header '" + LAST_MODIFIED + "'");
      assertMessageContains(err, expected);
      assertMessageContains(err, this.now);
    }
  }

  private void assertMessageContains(AssertionError error, String expected) {
    assertThat(error.getMessage().contains(expected)).as("Failure message should contain [" + expected + "], actual is [" + error.getMessage() + "]").isTrue();
  }

  @Controller
  private static class PersonController {

    private long timestamp;

    public void setStubTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }

    @RequestMapping("/persons/{id}")
    public ResponseEntity<Person> showEntity(@PathVariable long id, RequestContext request) {
      return ResponseEntity
              .ok()
              .lastModified(this.timestamp)
              .header("X-Rate-Limiting", "42")
              .header("Vary", "foo", "bar")
              .body(new Person("Jason"));
    }
  }

}
