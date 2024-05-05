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

package cn.taketoday.test.context.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.mock.web.MockContextImpl;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.web.mock.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies meta-annotation support for {@link WebAppConfiguration}
 * and {@link ContextConfiguration}.
 *
 * @author Sam Brannen
 * @see WebTestConfiguration
 * @since 4.0
 */
@ExtendWith(InfraExtension.class)
@WebTestConfiguration
class MetaAnnotationConfigWacTests {

  @Autowired
  WebApplicationContext wac;

  @Autowired
  MockContextImpl mockServletContext;

  @Autowired
  String foo;

  @Test
  void fooEnigmaAutowired() {
    assertThat(foo).isEqualTo("enigma");
  }

  @Test
  void basicWacFeatures() throws Exception {
    assertThat(wac.getServletContext()).as("ServletContext should be set in the WAC.").isNotNull();

    assertThat(mockServletContext).as("ServletContext should have been autowired from the WAC.").isNotNull();

    Object rootWac = mockServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    assertThat(rootWac).as("Root WAC must be stored in the ServletContext as: "
            + WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE).isNotNull();
    assertThat(rootWac).as("test WAC and Root WAC in ServletContext must be the same object.").isSameAs(wac);
    assertThat(wac.getServletContext()).as("ServletContext instances must be the same object.").isSameAs(mockServletContext);

    assertThat(mockServletContext.getRealPath("index.jsp")).as("Getting real path for ServletContext resource.").isEqualTo(new File("src/main/webapp/index.jsp").getCanonicalPath());
  }

}
