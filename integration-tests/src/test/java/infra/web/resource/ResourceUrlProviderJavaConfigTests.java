/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.mock.api.http.HttpMock;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpMockResponse;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockFilterChain;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.config.annotation.ResourceHandlerRegistry;
import infra.web.config.annotation.WebMvcConfigurationSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ResourceUrlProvider} with the latter configured in Framework MVC Java config.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceUrlProviderJavaConfigTests {

  private final TestMock testMock = new TestMock();

  private MockFilterChain filterChain;

  private HttpMockRequestImpl request;

  private MockHttpResponseImpl response;

  @BeforeEach
  public void setup() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(WebConfig.class);
    context.refresh();

    this.request = new HttpMockRequestImpl("GET", "/");
    this.response = new MockHttpResponseImpl();

    this.filterChain = new MockFilterChain(this.testMock/*,
            new ResourceUrlEncodingFilter(),
            (request, response, chain) -> {
              Object urlProvider = context.getBean(ResourceUrlProvider.class);
              request.setAttribute(ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR, urlProvider);
              chain.doFilter(request, response);
            }*/);
  }

//  @Test
//  public void resolvePathWithServletMappedAsRoot() throws Exception {
//    this.request.setRequestURI("/myapp/index");
//    this.request.setServletPath("/index");
//    this.filterChain.doFilter(this.request, this.response);
//
//    assertThat(resolvePublicResourceUrlPath("/myapp/resources/foo.css")).isEqualTo("/myapp/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css");
//  }

  @Test
  public void resolvePathNoMatch() throws Exception {
    this.request.setRequestURI("/myapp/index");
    this.filterChain.doFilter(this.request, this.response);

    assertThat(resolvePublicResourceUrlPath("/myapp/myservlet/index")).isEqualTo("/myapp/myservlet/index");
  }

  private String resolvePublicResourceUrlPath(String path) {
    return this.testMock.wrappedResponse.encodeURL(path);
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

  @SuppressWarnings("serial")
  private static class TestMock extends HttpMock {

    private HttpMockResponse wrappedResponse;

    @Override
    protected void doGet(HttpMockRequest request, HttpMockResponse response) {
      this.wrappedResponse = response;
    }
  }

}
