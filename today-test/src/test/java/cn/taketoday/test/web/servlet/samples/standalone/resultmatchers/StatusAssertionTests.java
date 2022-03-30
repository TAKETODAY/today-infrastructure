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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.bind.annotation.ExceptionHandler;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.ResponseStatus;
import cn.taketoday.web.bind.annotation.RestController;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static cn.taketoday.http.HttpStatus.BAD_REQUEST;
import static cn.taketoday.http.HttpStatus.CREATED;
import static cn.taketoday.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static cn.taketoday.http.HttpStatus.I_AM_A_TEAPOT;
import static cn.taketoday.http.HttpStatus.NOT_IMPLEMENTED;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Examples of expectations on the status and the status reason found in the response.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@TestInstance(PER_CLASS)
class StatusAssertionTests {

	private final MockMvc mockMvc = standaloneSetup(new StatusController()).build();

	@Test
	void httpStatus() throws Exception {
		this.mockMvc.perform(get("/created")).andExpect(status().isCreated());
		this.mockMvc.perform(get("/createdWithComposedAnnotation")).andExpect(status().isCreated());
		this.mockMvc.perform(get("/badRequest")).andExpect(status().isBadRequest());
	}

	@Test
	void statusCode() throws Exception {
		this.mockMvc.perform(get("/teaPot")).andExpect(status().is(I_AM_A_TEAPOT.value()));
		this.mockMvc.perform(get("/created")).andExpect(status().is(CREATED.value()));
		this.mockMvc.perform(get("/createdWithComposedAnnotation")).andExpect(status().is(CREATED.value()));
		this.mockMvc.perform(get("/badRequest")).andExpect(status().is(BAD_REQUEST.value()));
		this.mockMvc.perform(get("/throwsException")).andExpect(status().is(I_AM_A_TEAPOT.value()));
	}

	@Test
	void statusCodeWithMatcher() throws Exception {
		this.mockMvc.perform(get("/badRequest")).andExpect(status().is(equalTo(BAD_REQUEST.value())));
	}

	@Test
	void reason() throws Exception {
		this.mockMvc.perform(get("/badRequest")).andExpect(status().reason("Expired token"));
	}

	@Test
	void reasonWithMatcher() throws Exception {
		this.mockMvc.perform(get("/badRequest")).andExpect(status().reason(equalTo("Expired token")));
		this.mockMvc.perform(get("/badRequest")).andExpect(status().reason(endsWith("token")));
	}


	@RequestMapping
	@ResponseStatus
	@Retention(RetentionPolicy.RUNTIME)
	@interface Get {

		@AliasFor(annotation = RequestMapping.class, attribute = "path")
		String[] path() default {};

		@AliasFor(annotation = ResponseStatus.class, attribute = "code")
		HttpStatus status() default INTERNAL_SERVER_ERROR;
	}

	@RestController
	@ResponseStatus(I_AM_A_TEAPOT)
	private static class StatusController {

		@RequestMapping("/teaPot")
		void teaPot() {
		}

		@RequestMapping("/created")
		@ResponseStatus(CREATED)
		void created() {
		}

		@Get(path = "/createdWithComposedAnnotation", status = CREATED)
		void createdWithComposedAnnotation() {
		}

		@RequestMapping("/badRequest")
		@ResponseStatus(code = BAD_REQUEST, reason = "Expired token")
		void badRequest() {
		}

		@RequestMapping("/notImplemented")
		@ResponseStatus(NOT_IMPLEMENTED)
		void notImplemented() {
		}

		@RequestMapping("/throwsException")
		@ResponseStatus(NOT_IMPLEMENTED)
		void throwsException() {
			throw new IllegalStateException();
		}

		@ExceptionHandler
		void exceptionHandler(IllegalStateException ex) {
		}
	}

}
