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

package cn.taketoday.test.web.servlet.samples.standalone.resultmatchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.ResultMatcher;
import cn.taketoday.web.bind.annotation.PathVariable;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.context.request.WebRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static cn.taketoday.http.HttpHeaders.IF_MODIFIED_SINCE;
import static cn.taketoday.http.HttpHeaders.LAST_MODIFIED;
import static cn.taketoday.http.HttpHeaders.VARY;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.header;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

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

	@Test  // SPR-10771
	public void doesNotExist() throws Exception {
		this.mockMvc.perform(get("/persons/1")).andExpect(header().doesNotExist("X-Custom-Header"));
	}

	@Test // SPR-10771
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
		HttpHeaders headers = HttpHeaders.create();
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
			// SPR-10659: ensure header name is in the message
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
		public ResponseEntity<Person> showEntity(@PathVariable long id, WebRequest request) {
			return ResponseEntity
					.ok()
					.lastModified(this.timestamp)
					.header("X-Rate-Limiting", "42")
					.header("Vary", "foo", "bar")
					.body(new Person("Jason"));
		}
	}

}
