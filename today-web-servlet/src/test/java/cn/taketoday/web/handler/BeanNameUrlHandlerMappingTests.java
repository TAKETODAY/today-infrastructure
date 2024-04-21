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

package cn.taketoday.web.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.servlet.ConfigurableWebApplicationContext;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.support.XmlWebApplicationContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockServletContext;
import jakarta.servlet.ServletException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/31 16:21
 */
class BeanNameUrlHandlerMappingTests {

  private ConfigurableWebApplicationContext wac;

  @BeforeEach
  public void setUp() throws Exception {
    MockServletContext sc = new MockServletContext("");
    wac = new XmlWebApplicationContext();
    wac.setServletContext(sc);
    wac.setConfigLocations("/cn/taketoday/web/handler/map1.xml");
    wac.refresh();
  }

  @Test
  public void requestsWithoutHandlers() throws Exception {
    HandlerMapping hm = (HandlerMapping) wac.getBean("handlerMapping");

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/myapp/mypath/nonsense.html");
    req.setContextPath("/myapp");
    ServletRequestContext request = new ServletRequestContext(null, req, new MockHttpServletResponse());
    Object h = hm.getHandler(request);
    assertThat(h).as("Handler is null").isNull();

    req = new MockHttpServletRequest("GET", "/foo/bar/baz.html");
    h = hm.getHandler(new ServletRequestContext(null, req, new MockHttpServletResponse()));
    assertThat(h).as("Handler is null").isNull();
  }

  @Test
  public void requestsWithSubPaths() throws Exception {
    HandlerMapping hm = (HandlerMapping) wac.getBean("handlerMapping");
    doTestRequestsWithSubPaths(hm);
  }

  @Test
  public void requestsWithSubPathsInParentContext() throws Exception {
    BeanNameUrlHandlerMapping hm = new BeanNameUrlHandlerMapping();
    hm.setDetectHandlersInAncestorContexts(true);
    hm.setApplicationContext(new StaticApplicationContext(wac));
    doTestRequestsWithSubPaths(hm);
  }

  private void doTestRequestsWithSubPaths(HandlerMapping hm) throws Exception {
    Object bean = wac.getBean("godCtrl");

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mypath/welcome.html");
    HandlerExecutionChain hec = getChain(hm, req);
    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/myapp/mypath/welcome.html");
    req.setContextPath("/myapp");
    hec = getChain(hm, req);
    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/myapp/mypath/welcome.html");
    req.setContextPath("/myapp");
    req.setServletPath("/mypath/welcome.html");
    hec = getChain(hm, req);
    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/myapp/myservlet/mypath/welcome.html");
    req.setContextPath("/myapp");
    req.setServletPath("/myservlet");
    hec = getChain(hm, req);
    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/myapp/myapp/mypath/welcome.html");
    req.setContextPath("/myapp");
    req.setServletPath("/myapp");
    hec = getChain(hm, req);
    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/mypath/show.html");
    hec = getChain(hm, req);
    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/mypath/bookseats.html");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();
  }

  @Nullable
  private static HandlerExecutionChain getChain(HandlerMapping hm, MockHttpServletRequest req) throws Exception {
    return (HandlerExecutionChain) hm.getHandler(new ServletRequestContext(null, req, new MockHttpServletResponse()));
  }

  @Test
  public void requestsWithFullPaths() throws Exception {

    BeanNameUrlHandlerMapping hm = new BeanNameUrlHandlerMapping();
    hm.setApplicationContext(wac);
    Object bean = wac.getBean("godCtrl");

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mypath/welcome.html");
    HandlerExecutionChain hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/myapp/mypath/welcome.html");
    req.setContextPath("/myapp");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/myapp/mypath/welcome.html");
    req.setContextPath("/myapp");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/mypath/welcome.html");
    req.setContextPath("");
    req.setServletPath("/");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();
  }

  @Test
  public void asteriskMatches() throws Exception {
    HandlerMapping hm = (HandlerMapping) wac.getBean("handlerMapping");
    Object bean = wac.getBean("godCtrl");

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mypath/test.html");
    HandlerExecutionChain hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/mypath/testarossa");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/mypath/tes");
    hec = getChain(hm, req);

    assertThat(hec).as("Handler is correct bean").isNull();
  }

  @Test
  public void overlappingMappings() throws Exception {
    BeanNameUrlHandlerMapping hm = (BeanNameUrlHandlerMapping) wac.getBean("handlerMapping");
    Object anotherHandler = new Object();
    hm.registerHandler("/mypath/testaross*", anotherHandler);
    Object bean = wac.getBean("godCtrl");

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mypath/test.html");
    HandlerExecutionChain hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/mypath/testarossa");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == anotherHandler).as("Handler is correct bean").isTrue();

    req = new MockHttpServletRequest("GET", "/mypath/tes");
    hec = getChain(hm, req);

    assertThat(hec).as("Handler is correct bean").isNull();
  }

  @Test
  public void doubleMappings() throws ServletException {
    BeanNameUrlHandlerMapping hm = (BeanNameUrlHandlerMapping) wac.getBean("handlerMapping");
    assertThatIllegalStateException().isThrownBy(() ->
            hm.registerHandler("/mypath/welcome.html", new Object()));
  }

  @Test
  void aliasPlaceHolder() throws ServletException {

    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(BeanNameUrlHandlerMapping.class);
      context.getEnvironment().getPropertySources()
              .addFirst(new MapPropertySource("placeholder", Map.of("request.path", "/req-path")));
      context.register(AliasPlaceHolderConfig.class);
      context.refresh();

      BeanNameUrlHandlerMapping urlMapping = context.getBean(BeanNameUrlHandlerMapping.class);
      assertThat(urlMapping.getHandlerMap()).hasSize(2);
    }
  }

  @Configuration
  static class AliasPlaceHolderConfig {

    @Component({ "obj", "/path" })
    Object obj() {
      return new Object();
    }

    @Component({ "obj11", "${request.path}" })
    Object path() {
      return new Object();
    }

  }
}