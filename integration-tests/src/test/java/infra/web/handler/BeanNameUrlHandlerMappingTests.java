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

package infra.web.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.support.StaticApplicationContext;
import infra.core.env.MapPropertySource;
import org.jspecify.annotations.Nullable;
import infra.mock.api.MockException;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.stereotype.Component;
import infra.web.HandlerMapping;
import infra.web.mock.ConfigurableWebApplicationContext;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.XmlWebApplicationContext;

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
    MockContextImpl sc = new MockContextImpl("");
    wac = new XmlWebApplicationContext();
    wac.setMockContext(sc);
    wac.setConfigLocations("/infra/web/handler/map1.xml");
    wac.refresh();
  }

  @Test
  public void requestsWithoutHandlers() throws Exception {
    HandlerMapping hm = (HandlerMapping) wac.getBean("handlerMapping");

    HttpMockRequestImpl req = new HttpMockRequestImpl("GET", "/mypath/nonsense.html");
    MockRequestContext request = new MockRequestContext(null, req, new MockHttpResponseImpl());
    Object h = hm.getHandler(request);
    assertThat(h).as("Handler is null").isNull();

    req = new HttpMockRequestImpl("GET", "/foo/bar/baz.html");
    h = hm.getHandler(new MockRequestContext(null, req, new MockHttpResponseImpl()));
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

    HttpMockRequestImpl req = new HttpMockRequestImpl("GET", "/mypath/welcome.html");
    HandlerExecutionChain hec = getChain(hm, req);
    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/welcome.html");
    hec = getChain(hm, req);
    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/welcome.html");
    hec = getChain(hm, req);
    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/welcome.html");
    hec = getChain(hm, req);
    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/welcome.html");
    hec = getChain(hm, req);
    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/show.html");
    hec = getChain(hm, req);
    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/bookseats.html");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();
  }

  @Nullable
  private static HandlerExecutionChain getChain(HandlerMapping hm, HttpMockRequestImpl req) throws Exception {
    return (HandlerExecutionChain) hm.getHandler(new MockRequestContext(null, req, new MockHttpResponseImpl()));
  }

  @Test
  public void requestsWithFullPaths() throws Exception {

    BeanNameUrlHandlerMapping hm = new BeanNameUrlHandlerMapping();
    hm.setApplicationContext(wac);
    Object bean = wac.getBean("godCtrl");

    HttpMockRequestImpl req = new HttpMockRequestImpl("GET", "/mypath/welcome.html");
    HandlerExecutionChain hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/welcome.html");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/welcome.html");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/welcome.html");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();
  }

  @Test
  public void asteriskMatches() throws Exception {
    HandlerMapping hm = (HandlerMapping) wac.getBean("handlerMapping");
    Object bean = wac.getBean("godCtrl");

    HttpMockRequestImpl req = new HttpMockRequestImpl("GET", "/mypath/test.html");
    HandlerExecutionChain hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/testarossa");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/tes");
    hec = getChain(hm, req);

    assertThat(hec).as("Handler is correct bean").isNull();
  }

  @Test
  public void overlappingMappings() throws Exception {
    BeanNameUrlHandlerMapping hm = (BeanNameUrlHandlerMapping) wac.getBean("handlerMapping");
    Object anotherHandler = new Object();
    hm.registerHandler("/mypath/testaross*", anotherHandler);
    Object bean = wac.getBean("godCtrl");

    HttpMockRequestImpl req = new HttpMockRequestImpl("GET", "/mypath/test.html");
    HandlerExecutionChain hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == bean).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/testarossa");
    hec = getChain(hm, req);

    assertThat(hec != null && hec.getRawHandler() == anotherHandler).as("Handler is correct bean").isTrue();

    req = new HttpMockRequestImpl("GET", "/mypath/tes");
    hec = getChain(hm, req);

    assertThat(hec).as("Handler is correct bean").isNull();
  }

  @Test
  public void doubleMappings() throws MockException {
    BeanNameUrlHandlerMapping hm = (BeanNameUrlHandlerMapping) wac.getBean("handlerMapping");
    assertThatIllegalStateException().isThrownBy(() ->
            hm.registerHandler("/mypath/welcome.html", new Object()));
  }

  @Test
  void aliasPlaceHolder() throws MockException {

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