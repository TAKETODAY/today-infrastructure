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

import org.junit.jupiter.api.Test;
import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.setup.DefaultMockMvcBuilder;
import cn.taketoday.test.web.servlet.setup.StubWebApplicationContext;
import cn.taketoday.test.web.w.servlet.setup.StubWebApplicationContext;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.context.support.StaticWebApplicationContext;
import cn.taketoday.web.context.support.WebApplicationContextUtils;
import cn.taketoday.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Tests for {@link DefaultMockMvcBuilder}.
 *
 * @author Rob Winch
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Stephane Nicoll
 */
public class DefaultMockMvcBuilderTests {

	private final MockServletContext servletContext = new MockServletContext();

	@Test
	public void webAppContextSetupWithNullWac() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				webAppContextSetup(null))
			.withMessage("WebApplicationContext is required");
	}

	@Test
	public void webAppContextSetupWithNullServletContext() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				webAppContextSetup(new StubWebApplicationContext(null)))
			.withMessage("WebApplicationContext must have a ServletContext");
	}

	/**
	 * See SPR-12553 and SPR-13075.
	 */
	@Test
	public void rootWacServletContainerAttributePreviouslySet() {
		StubWebApplicationContext child = new StubWebApplicationContext(this.servletContext);
		this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, child);

		DefaultMockMvcBuilder builder = webAppContextSetup(child);
		assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext)).isSameAs(builder.initWebAppContext());
	}

	/**
	 * See SPR-12553 and SPR-13075.
	 */
	@Test
	public void rootWacServletContainerAttributePreviouslySetWithContextHierarchy() {
		StubWebApplicationContext root = new StubWebApplicationContext(this.servletContext);

		this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, root);

		StaticWebApplicationContext child = new StaticWebApplicationContext();
		child.setParent(root);
		child.setServletContext(this.servletContext);

		DefaultMockMvcBuilder builder = webAppContextSetup(child);
		assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext)).isSameAs(builder.initWebAppContext().getParent());
	}

	/**
	 * See SPR-12553 and SPR-13075.
	 */
	@Test
	public void rootWacServletContainerAttributeNotPreviouslySet() {
		StubWebApplicationContext root = new StubWebApplicationContext(this.servletContext);
		DefaultMockMvcBuilder builder = webAppContextSetup(root);
		WebApplicationContext wac = builder.initWebAppContext();
		assertThat(wac).isSameAs(root);
		assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext)).isSameAs(root);
	}

	/**
	 * See SPR-12553 and SPR-13075.
	 */
	@Test
	public void rootWacServletContainerAttributeNotPreviouslySetWithContextHierarchy() {
		StaticApplicationContext ear = new StaticApplicationContext();
		StaticWebApplicationContext root = new StaticWebApplicationContext();
		root.setParent(ear);
		root.setServletContext(this.servletContext);
		StaticWebApplicationContext dispatcher = new StaticWebApplicationContext();
		dispatcher.setParent(root);
		dispatcher.setServletContext(this.servletContext);

		DefaultMockMvcBuilder builder = webAppContextSetup(dispatcher);
		WebApplicationContext wac = builder.initWebAppContext();

		assertThat(wac).isSameAs(dispatcher);
		assertThat(wac.getParent()).isSameAs(root);
		assertThat(wac.getParent().getParent()).isSameAs(ear);
		assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext)).isSameAs(root);
	}

	/**
	 * See /SPR-14277
	 */
	@Test
	public void dispatcherServletCustomizer() {
		StubWebApplicationContext root = new StubWebApplicationContext(this.servletContext);
		DefaultMockMvcBuilder builder = webAppContextSetup(root);
		builder.addDispatcherServletCustomizer(ds -> ds.setContextId("test-id"));
		builder.dispatchOptions(true);
		MockMvc mvc = builder.build();
		DispatcherServlet ds = (DispatcherServlet) new DirectFieldAccessor(mvc)
				.getPropertyValue("servlet");
		assertThat(ds.getContextId()).isEqualTo("test-id");
	}

	@Test
	public void dispatcherServletCustomizerProcessedInOrder() {
		StubWebApplicationContext root = new StubWebApplicationContext(this.servletContext);
		DefaultMockMvcBuilder builder = webAppContextSetup(root);
		builder.addDispatcherServletCustomizer(ds -> ds.setContextId("test-id"));
		builder.addDispatcherServletCustomizer(ds -> ds.setContextId("override-id"));
		builder.dispatchOptions(true);
		MockMvc mvc = builder.build();
		DispatcherServlet ds = (DispatcherServlet) new DirectFieldAccessor(mvc)
				.getPropertyValue("servlet");
		assertThat(ds.getContextId()).isEqualTo("override-id");
	}

}
