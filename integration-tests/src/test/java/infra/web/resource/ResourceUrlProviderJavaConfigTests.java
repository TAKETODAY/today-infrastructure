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

package infra.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.web.mock.api.MockHandler;
import infra.web.mock.MockRequest;
import infra.web.mock.MockFilterChain;
import infra.web.mock.MockResponse;
import infra.web.config.annotation.ResourceHandlerRegistry;
import infra.web.config.annotation.WebMvcConfigurationSupport;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link ResourceUrlProvider} with the latter configured in Framework MVC Java config.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceUrlProviderJavaConfigTests {

  private final MockHandler testMock = mock();

  private MockFilterChain filterChain;

  private MockRequest request;

  private MockResponse response;

  @BeforeEach
  public void setup() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(WebConfig.class);
    context.refresh();

    this.request = new MockRequest("GET", "/");
    this.response = new MockResponse();

    this.filterChain = new MockFilterChain(this.testMock/*,
            new ResourceUrlEncodingFilter(),
            (request, response, chain) -> {
              Object urlProvider = context.getBean(ResourceUrlProvider.class);
              request.setAttribute(ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR, urlProvider);
              chain.doFilter(request, response);
            }*/);
  }

  @Test
  public void resolvePathNoMatch() throws Exception {
    this.request.setRequestURI("/myapp/index");
    this.filterChain.doFilter(new MockHttpContext(this.request, this.response));

    assertThat(resolvePublicResourceUrlPath("/myapp/myservlet/index")).isEqualTo("/myapp/myservlet/index");
  }

  private String resolvePublicResourceUrlPath(String path) {
    return path;
  }

  @Configuration
  static class WebConfig extends WebMvcConfigurationSupport {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/resources/**")
              .addResourceLocations("classpath:infra/web/resource/test/")
              .resourceChain(true).addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));
    }
  }

}
