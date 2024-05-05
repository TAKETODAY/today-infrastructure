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

package cn.taketoday.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.mock.web.MockFilterChain;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.config.ResourceHandlerRegistry;
import cn.taketoday.web.config.WebMvcConfigurationSupport;
import cn.taketoday.mock.api.http.HttpServlet;
import cn.taketoday.mock.api.http.HttpServletRequest;
import cn.taketoday.mock.api.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ResourceUrlProvider} with the latter configured in Framework MVC Java config.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceUrlProviderJavaConfigTests {

  private final TestServlet servlet = new TestServlet();

  private MockFilterChain filterChain;

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  @BeforeEach
  public void setup() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(WebConfig.class);
    context.refresh();

    this.request = new MockHttpServletRequest("GET", "/");
    this.response = new MockHttpServletResponse();

    this.filterChain = new MockFilterChain(this.servlet/*,
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
    return this.servlet.wrappedResponse.encodeURL(path);
  }

  @Configuration
  static class WebConfig extends WebMvcConfigurationSupport {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/resources/**")
              .addResourceLocations("classpath:cn/taketoday/web/resource/test/")
              .resourceChain(true).addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));
    }
  }

  @SuppressWarnings("serial")
  private static class TestServlet extends HttpServlet {

    private HttpServletResponse wrappedResponse;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
      this.wrappedResponse = response;
    }
  }

}
