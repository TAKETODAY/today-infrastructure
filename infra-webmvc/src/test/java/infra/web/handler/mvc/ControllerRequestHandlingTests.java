/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler.mvc;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import infra.http.CacheControl;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.server.RequestPath;
import infra.session.Session;
import infra.web.ServerHttpExchange;
import infra.web.server.ResponseStatusException;
import infra.web.util.WebUtils;
import infra.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class ControllerRequestHandlingTests {

  @Test
  void synchronizesHandlerOnTheConfiguredSessionMutex() throws Exception {
    Object mutex = new Object();
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    Session session = mock(Session.class);
    given(request.getMethod()).willReturn(HttpMethod.GET);
    given(request.getMethodAsString()).willReturn("GET");
    given(request.getSession(false)).willReturn(session, (Session) null);
    given(session.getAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE)).willReturn(mutex);
    AbstractController controller = new AbstractController() {
      @Override
      protected Object handleRequestInternal(ServerHttpExchange context) {
        return Thread.holdsLock(mutex);
      }
    };

    controller.setSynchronizeOnSession(true);
    assertThat(controller.isSynchronizeOnSession()).isTrue();
    assertThat(controller.handleRequest(request)).isEqualTo(true);
    assertThat(controller.handleRequest(request)).isEqualTo(false);
  }

  @Test
  void urlFilenameControllerAppliesPrefixAndSuffixAndRejectsRedirectNames() {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    controller.setPrefix("prefix/");
    controller.setSuffix("Suffix");
    assertThat(controller.getPrefix()).isEqualTo("prefix/");
    assertThat(controller.getSuffix()).isEqualTo("Suffix");
    assertThat(controller.getViewNameForUrlPath("/catalog/index.html")).isEqualTo("prefix/catalog/indexSuffix");
    assertThat(controller.getViewNameForUrlPath("/catalog/index.html")).isEqualTo("prefix/catalog/indexSuffix");

    UrlFilenameViewController redirectController = new UrlFilenameViewController();
    redirectController.setPrefix("redirect:");
    assertThatThrownBy(() -> redirectController.getViewNameForUrlPath("/catalog/redirect.html"))
            .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void urlFilenameControllerCanPreserveSemicolonContent() throws Exception {
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    given(request.getMethod()).willReturn(HttpMethod.GET);
    given(request.getMethodAsString()).willReturn("GET");
    given(request.getRequestPath()).willReturn(RequestPath.parse("/catalog;lang=en/index.html", ""));
    UrlFilenameViewController controller = new UrlFilenameViewController();

    assertThat(((ModelAndView) controller.handleRequest(request)).getViewName()).isEqualTo("catalog/index");
    controller.setRemoveSemicolonContent(false);
    assertThat(((ModelAndView) controller.handleRequest(request)).getViewName()).isEqualTo("catalog;lang=en/index");
  }

  @Test
  void appliesCacheControlThenCacheSecondsThenDefault() {
    WebContentInterceptor interceptor = new WebContentInterceptor();
    interceptor.setCacheControl(CacheControl.noCache());
    interceptor.addCacheMapping(CacheControl.noStore(), "/private/**");
    Properties mappings = new Properties();
    mappings.setProperty("/private/**", "30");
    mappings.setProperty("/public/**", "60");
    interceptor.setCacheMappings(mappings);

    assertCacheHeader(interceptor, "/private/index", "no-store");
    assertCacheHeader(interceptor, "/public/index", "max-age=60");
    assertCacheHeader(interceptor, "/other/index", "no-cache");
  }

  private static void assertCacheHeader(WebContentInterceptor interceptor, String path, String value) {
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    given(context.getRequestPath()).willReturn(RequestPath.parse(path, ""));
    assertThat(interceptor.preProcessing(context, new Object())).isTrue();
    then(context).should().setHeader(HttpHeaders.CACHE_CONTROL, value);
  }

}
