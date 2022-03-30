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

package cn.taketoday.test.web.servlet.setup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.mock.web.MockFilterChain;
import cn.taketoday.mock.web.MockFilterConfig;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;

import cn.taketoday.test.web.servlet.setup.PatternMappingFilterProxy;
import cn.taketoday.test.web.w.servlet.setup.PatternMappingFilterProxy;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.assertj.core.api.Assertions.assertThat;




/**
 * @author Rob Winch
 */
public class ConditionalDelegatingFilterProxyTests {

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private MockFilterChain filterChain;

	private MockFilter delegate;

	private PatternMappingFilterProxy filter;


	@BeforeEach
	public void setup() {
		request = new MockHttpServletRequest();
		request.setContextPath("/context");
		response = new MockHttpServletResponse();
		filterChain = new MockFilterChain();
		delegate = new MockFilter();
	}


	@Test
	public void init() throws Exception {
		FilterConfig config = new MockFilterConfig();
		filter = new PatternMappingFilterProxy(delegate, "/");
		filter.init(config);
		assertThat(delegate.filterConfig).isEqualTo(config);
	}

	@Test
	public void destroy() throws Exception {
		filter = new PatternMappingFilterProxy(delegate, "/");
		filter.destroy();
		assertThat(delegate.destroy).isTrue();
	}

	@Test
	public void matchExact() throws Exception {
		assertFilterInvoked("/test", "/test");
	}

	@Test
	public void matchExactEmpty() throws Exception {
		assertFilterInvoked("", "");
	}

	@Test
	public void matchPathMappingAllFolder() throws Exception {
		assertFilterInvoked("/test/this", "/*");
	}

	@Test
	public void matchPathMappingAll() throws Exception {
		assertFilterInvoked("/test", "/*");
	}

	@Test
	public void matchPathMappingAllContextRoot() throws Exception {
		assertFilterInvoked("", "/*");
	}

	@Test
	public void matchPathMappingContextRootAndSlash() throws Exception {
		assertFilterInvoked("/", "/*");
	}

	@Test
	public void matchPathMappingFolderPatternWithMultiFolderPath() throws Exception {
		assertFilterInvoked("/test/this/here", "/test/*");
	}

	@Test
	public void matchPathMappingFolderPattern() throws Exception {
		assertFilterInvoked("/test/this", "/test/*");
	}

	@Test
	public void matchPathMappingNoSuffix() throws Exception {
		assertFilterInvoked("/test/", "/test/*");
	}

	@Test
	public void matchPathMappingMissingSlash() throws Exception {
		assertFilterInvoked("/test", "/test/*");
	}

	@Test
	public void noMatchPathMappingMulti() throws Exception {
		assertFilterNotInvoked("/this/test/here", "/test/*");
	}

	@Test
	public void noMatchPathMappingEnd() throws Exception {
		assertFilterNotInvoked("/this/test", "/test/*");
	}

	@Test
	public void noMatchPathMappingEndSuffix() throws Exception {
		assertFilterNotInvoked("/test2/", "/test/*");
	}

	@Test
	public void noMatchPathMappingMissingSlash() throws Exception {
		assertFilterNotInvoked("/test2", "/test/*");
	}

	@Test
	public void matchExtensionMulti() throws Exception {
		assertFilterInvoked("/test/this/here.html", "*.html");
	}

	@Test
	public void matchExtension() throws Exception {
		assertFilterInvoked("/test/this.html", "*.html");
	}

	@Test
	public void matchExtensionNoPrefix() throws Exception {
		assertFilterInvoked("/.html", "*.html");
	}

	@Test
	public void matchExtensionNoFolder() throws Exception {
		assertFilterInvoked("/test.html", "*.html");
	}

	@Test
	public void noMatchExtensionNoSlash() throws Exception {
		assertFilterNotInvoked(".html", "*.html");
	}

	@Test
	public void noMatchExtensionSlashEnd() throws Exception {
		assertFilterNotInvoked("/index.html/", "*.html");
	}

	@Test
	public void noMatchExtensionPeriodEnd() throws Exception {
		assertFilterNotInvoked("/index.html.", "*.html");
	}

	@Test
	public void noMatchExtensionLarger() throws Exception {
		assertFilterNotInvoked("/index.htm", "*.html");
	}

	@Test
	public void noMatchInvalidPattern() throws Exception {
		// pattern uses extension mapping but starts with / (treated as exact match)
		assertFilterNotInvoked("/index.html", "/*.html");
	}

	/*
	 * Below are tests from Table 12-1 of the Servlet Specification
	 */
	@Test
	public void specPathMappingMultiFolderPattern() throws Exception {
		assertFilterInvoked("/foo/bar/index.html", "/foo/bar/*");
	}

	@Test
	public void specPathMappingMultiFolderPatternAlternate() throws Exception {
		assertFilterInvoked("/foo/bar/index.bop", "/foo/bar/*");
	}

	@Test
	public void specPathMappingNoSlash() throws Exception {
		assertFilterInvoked("/baz", "/baz/*");
	}

	@Test
	public void specPathMapping() throws Exception {
		assertFilterInvoked("/baz/index.html", "/baz/*");
	}

	@Test
	public void specExactMatch() throws Exception {
		assertFilterInvoked("/catalog", "/catalog");
	}

	@Test
	public void specExtensionMappingSingleFolder() throws Exception {
		assertFilterInvoked("/catalog/racecar.bop", "*.bop");
	}

	@Test
	public void specExtensionMapping() throws Exception {
		assertFilterInvoked("/index.bop", "*.bop");
	}

	private void assertFilterNotInvoked(String requestUri, String pattern) throws Exception {
		request.setRequestURI(request.getContextPath() + requestUri);
		filter = new PatternMappingFilterProxy(delegate, pattern);
		filter.doFilter(request, response, filterChain);

		assertThat(delegate.request).isNull();
		assertThat(delegate.response).isNull();
		assertThat(delegate.chain).isNull();

		assertThat(filterChain.getRequest()).isEqualTo(request);
		assertThat(filterChain.getResponse()).isEqualTo(response);
		filterChain = new MockFilterChain();
	}

	private void assertFilterInvoked(String requestUri, String pattern) throws Exception {
		request.setRequestURI(request.getContextPath() + requestUri);
		filter = new PatternMappingFilterProxy(delegate, pattern);
		filter.doFilter(request, response, filterChain);

		assertThat(delegate.request).isEqualTo(request);
		assertThat(delegate.response).isEqualTo(response);
		assertThat(delegate.chain).isEqualTo(filterChain);
		delegate = new MockFilter();
	}


	private static class MockFilter implements Filter {

		private FilterConfig filterConfig;

		private ServletRequest request;

		private ServletResponse response;

		private FilterChain chain;

		private boolean destroy;

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			this.filterConfig = filterConfig;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
			this.request = request;
			this.response = response;
			this.chain = chain;
		}

		@Override
		public void destroy() {
			this.destroy = true;
		}
	}

}
