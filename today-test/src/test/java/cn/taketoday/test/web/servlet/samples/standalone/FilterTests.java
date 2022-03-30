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

package cn.taketoday.test.web.servlet.samples.standalone;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.validation.Errors;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.PostMapping;
import cn.taketoday.web.bind.annotation.ResponseBody;
import cn.taketoday.web.filter.OncePerRequestFilter;
import cn.taketoday.web.filter.ShallowEtagHeaderFilter;
import cn.taketoday.web.servlet.ModelAndView;
import cn.taketoday.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.concurrent.CompletableFuture;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.validation.Valid;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.post;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.flash;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.header;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.model;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.request;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Tests with {@link Filter}'s.
 * @author Rob Winch
 */
public class FilterTests {

	@Test
	public void whenFiltersCompleteMvcProcessesRequest() throws Exception {
		standaloneSetup(new PersonController())
			.addFilters(new ContinueFilter()).build()
			.perform(post("/persons").param("name", "Andy"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/person/1"))
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("id"))
				.andExpect(flash().attributeCount(1))
				.andExpect(flash().attribute("message", "success!"));
	}

	@Test
	public void filtersProcessRequest() throws Exception {
		standaloneSetup(new PersonController())
			.addFilters(new ContinueFilter(), new RedirectFilter()).build()
			.perform(post("/persons").param("name", "Andy"))
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	public void filterMappedBySuffix() throws Exception {
		standaloneSetup(new PersonController())
			.addFilter(new RedirectFilter(), "*.html").build()
			.perform(post("/persons.html").param("name", "Andy"))
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	public void filterWithExactMapping() throws Exception {
		standaloneSetup(new PersonController())
			.addFilter(new RedirectFilter(), "/p", "/persons").build()
			.perform(post("/persons").param("name", "Andy"))
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	public void filterSkipped() throws Exception {
		standaloneSetup(new PersonController())
			.addFilter(new RedirectFilter(), "/p", "/person").build()
			.perform(post("/persons").param("name", "Andy"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/person/1"))
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("id"))
				.andExpect(flash().attributeCount(1))
				.andExpect(flash().attribute("message", "success!"));
	}

	@Test
	public void filterWrapsRequestResponse() throws Exception {
		standaloneSetup(new PersonController())
			.addFilters(new WrappingRequestResponseFilter()).build()
			.perform(post("/user"))
				.andExpect(model().attribute("principal", WrappingRequestResponseFilter.PRINCIPAL_NAME));
	}

	@Test // SPR-16067, SPR-16695
	public void filterWrapsRequestResponseAndPerformsAsyncDispatch() throws Exception {
		MockMvc mockMvc = standaloneSetup(new PersonController())
				.addFilters(new WrappingRequestResponseFilter(), new ShallowEtagHeaderFilter())
				.build();

		MvcResult mvcResult = mockMvc.perform(get("/persons/1").accept(MediaType.APPLICATION_JSON))
				.andExpect(request().asyncStarted())
				.andExpect(request().asyncResult(new Person("Lukas")))
				.andReturn();

		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(header().longValue("Content-Length", 53))
				.andExpect(header().string("ETag", "\"0e37becb4f0c90709cb2e1efcc61eaa00\""))
				.andExpect(content().string("{\"name\":\"Lukas\",\"someDouble\":0.0,\"someBoolean\":false}"));
	}


	@Controller
	private static class PersonController {

		@PostMapping(path="/persons")
		public String save(@Valid Person person, Errors errors, RedirectAttributes redirectAttrs) {
			if (errors.hasErrors()) {
				return "person/add";
			}
			redirectAttrs.addAttribute("id", "1");
			redirectAttrs.addFlashAttribute("message", "success!");
			return "redirect:/person/{id}";
		}

		@PostMapping("/user")
		public ModelAndView user(Principal principal) {
			return new ModelAndView("user/view", "principal", principal.getName());
		}

		@GetMapping("/forward")
		public String forward() {
			return "forward:/persons";
		}

		@GetMapping("persons/{id}")
		@ResponseBody
		public CompletableFuture<Person> getPerson() {
			return CompletableFuture.completedFuture(new Person("Lukas"));
		}
	}

	private class ContinueFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request,
				HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

			filterChain.doFilter(request, response);
		}
	}

	private static class WrappingRequestResponseFilter extends OncePerRequestFilter {

		public static final String PRINCIPAL_NAME = "WrapRequestResponseFilterPrincipal";


		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws ServletException, IOException {

			filterChain.doFilter(new HttpServletRequestWrapper(request) {

				@Override
				public Principal getUserPrincipal() {
					return () -> PRINCIPAL_NAME;
				}

				// Like Spring Security does in HttpServlet3RequestFactory..

				@Override
				public AsyncContext getAsyncContext() {
					return super.getAsyncContext() != null ?
							new AsyncContextWrapper(super.getAsyncContext()) : null;
				}

			}, new HttpServletResponseWrapper(response));
		}
	}

	private class RedirectFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws ServletException, IOException {

			response.sendRedirect("/login");
		}
	}


	private static class AsyncContextWrapper implements AsyncContext {

		private final AsyncContext delegate;

		public AsyncContextWrapper(AsyncContext delegate) {
			this.delegate = delegate;
		}

		@Override
		public ServletRequest getRequest() {
			return this.delegate.getRequest();
		}

		@Override
		public ServletResponse getResponse() {
			return this.delegate.getResponse();
		}

		@Override
		public boolean hasOriginalRequestAndResponse() {
			return this.delegate.hasOriginalRequestAndResponse();
		}

		@Override
		public void dispatch() {
			this.delegate.dispatch();
		}

		@Override
		public void dispatch(String path) {
			this.delegate.dispatch(path);
		}

		@Override
		public void dispatch(ServletContext context, String path) {
			this.delegate.dispatch(context, path);
		}

		@Override
		public void complete() {
			this.delegate.complete();
		}

		@Override
		public void start(Runnable run) {
			this.delegate.start(run);
		}

		@Override
		public void addListener(AsyncListener listener) {
			this.delegate.addListener(listener);
		}

		@Override
		public void addListener(AsyncListener listener, ServletRequest req, ServletResponse res) {
			this.delegate.addListener(listener, req, res);
		}

		@Override
		public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
			return this.delegate.createListener(clazz);
		}

		@Override
		public void setTimeout(long timeout) {
			this.delegate.setTimeout(timeout);
		}

		@Override
		public long getTimeout() {
			return this.delegate.getTimeout();
		}
	}
}
