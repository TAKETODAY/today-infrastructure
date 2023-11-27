/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.mock.web;

import org.junit.jupiter.api.Test;

import jakarta.servlet.jsp.PageContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@code MockPageContext} class.
 *
 * @author Rick Evans
 */
class MockPageContextTests {

	private final String key = "foo";

	private final String value = "bar";

	private final MockPageContext ctx = new MockPageContext();

	@Test
	void setAttributeWithNoScopeUsesPageScope() throws Exception {
		ctx.setAttribute(key, value);
		assertThat(ctx.getAttribute(key, PageContext.PAGE_SCOPE)).isEqualTo(value);
		assertThat(ctx.getAttribute(key, PageContext.APPLICATION_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.REQUEST_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.SESSION_SCOPE)).isNull();
	}

	@Test
	void removeAttributeWithNoScopeSpecifiedRemovesValueFromAllScopes() throws Exception {
		ctx.setAttribute(key, value, PageContext.APPLICATION_SCOPE);
		ctx.removeAttribute(key);

		assertThat(ctx.getAttribute(key, PageContext.PAGE_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.APPLICATION_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.REQUEST_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.SESSION_SCOPE)).isNull();
	}

}
