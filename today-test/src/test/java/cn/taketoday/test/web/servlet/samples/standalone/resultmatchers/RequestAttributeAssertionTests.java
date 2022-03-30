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
import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.servlet.HandlerMapping;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.request;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Examples of expectations on created request attributes.
 *
 * @author Rossen Stoyanchev
 */
public class RequestAttributeAssertionTests {

	private final MockMvc mockMvc = standaloneSetup(new SimpleController()).build();


	@Test
	void requestAttributeEqualTo() throws Exception {
		this.mockMvc.perform(get("/main/1").servletPath("/main"))
			.andExpect(request().attribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/{id}"))
			.andExpect(request().attribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/1"));
	}

	@Test
	void requestAttributeMatcher() throws Exception {
		String producibleMediaTypes = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;

		this.mockMvc.perform(get("/1"))
			.andExpect(request().attribute(producibleMediaTypes, hasItem(MediaType.APPLICATION_JSON)))
			.andExpect(request().attribute(producibleMediaTypes, not(hasItem(MediaType.APPLICATION_XML))));

		this.mockMvc.perform(get("/main/1").servletPath("/main"))
			.andExpect(request().attribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, equalTo("/{id}")))
			.andExpect(request().attribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, equalTo("/1")));
	}


	@Controller
	private static class SimpleController {

		@RequestMapping(path="/{id}", produces="application/json")
		String show() {
			return "view";
		}
	}

}
