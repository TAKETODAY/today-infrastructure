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

package cn.taketoday.web.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.http.MediaType;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.accept.FixedContentNegotiationStrategy;
import cn.taketoday.web.accept.HeaderContentNegotiationStrategy;
import cn.taketoday.web.accept.MappingMediaTypeFileExtensionResolver;
import cn.taketoday.web.accept.ParameterContentNegotiationStrategy;
import cn.taketoday.web.accept.PathExtensionContentNegotiationStrategy;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.mock.web.MockContextImpl;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.mock.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
public class ContentNegotiatingViewResolverTests {

  private ContentNegotiatingViewResolver viewResolver;

  private HttpMockRequestImpl request;

  RequestContext requestContext;
  StaticWebApplicationContext wac = new StaticWebApplicationContext();

  @BeforeEach
  public void createViewResolver() {
    wac.setMockContext(new MockContextImpl());
    wac.refresh();
    viewResolver = new ContentNegotiatingViewResolver();
    viewResolver.setApplicationContext(wac);
    request = new HttpMockRequestImpl("GET", "/test");
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    this.requestContext = new MockRequestContext(wac, request, response);
    RequestContextHolder.set(requestContext);
  }

  @AfterEach
  public void resetRequestContextHolder() {
    RequestContextHolder.cleanup();
  }

  @Test
  public void getMediaTypeAcceptHeaderWithProduces() throws Exception {

    HandlerMatchingMetadata matchingMetadata = new HandlerMatchingMetadata(requestContext);
    matchingMetadata.setProducibleMediaTypes(new MediaType[] { MediaType.APPLICATION_XHTML_XML });
    requestContext.setMatchingMetadata(matchingMetadata);

    request.addHeader("Accept", "text/html,application/xml;q=0.9,application/xhtml+xml,*/*;q=0.8");
    viewResolver.afterPropertiesSet();
    List<MediaType> result = viewResolver.getMediaTypes(requestContext);
    assertThat(result.get(0)).as("Invalid content type").isEqualTo(new MediaType("application", "xhtml+xml"));
  }

  @Test
  public void resolveViewNameWithPathExtension() throws Exception {
    request.setRequestURI("/test");
    request.setParameter("format", "xls");

    String mediaType = "application/vnd.ms-excel";
    ContentNegotiationManager manager = new ContentNegotiationManager(
            new ParameterContentNegotiationStrategy(
                    Collections.singletonMap("xls", MediaType.parseMediaType(mediaType))));

    ViewResolver viewResolverMock = mock(ViewResolver.class);
    viewResolver.setContentNegotiationManager(manager);
    viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));
    viewResolver.afterPropertiesSet();

    View viewMock = mock(View.class, "application_xls");

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(null);
    given(viewResolverMock.resolveViewName(viewName + ".xls", locale)).willReturn(viewMock);
    given(viewMock.getContentType()).willReturn(mediaType);

    View result = viewResolver.resolveViewName(viewName, locale);
    assertThat(result).as("Invalid view").isSameAs(viewMock);
  }

  @Test
  public void resolveViewNameWithAcceptHeader() throws Exception {
    request.addHeader("Accept", "application/vnd.ms-excel");

    Map<String, MediaType> mapping = Collections.singletonMap("xls", MediaType.valueOf("application/vnd.ms-excel"));
    MappingMediaTypeFileExtensionResolver extensionsResolver = new MappingMediaTypeFileExtensionResolver(mapping);
    ContentNegotiationManager manager = new ContentNegotiationManager(new HeaderContentNegotiationStrategy());
    manager.addFileExtensionResolvers(extensionsResolver);
    viewResolver.setContentNegotiationManager(manager);

    ViewResolver viewResolverMock = mock(ViewResolver.class);
    viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

    View viewMock = mock(View.class, "application_xls");

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(null);
    given(viewResolverMock.resolveViewName(viewName + ".xls", locale)).willReturn(viewMock);
    given(viewMock.getContentType()).willReturn("application/vnd.ms-excel");

    View result = viewResolver.resolveViewName(viewName, locale);
    assertThat(result).as("Invalid view").isSameAs(viewMock);
  }

  @Test
  public void resolveViewNameWithInvalidAcceptHeader() throws Exception {
    request.addHeader("Accept", "application");

    ViewResolver viewResolverMock = mock(ViewResolver.class);
    viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));
    viewResolver.afterPropertiesSet();

    View result = viewResolver.resolveViewName("test", Locale.ENGLISH);
    assertThat(result).isNull();
  }

  @Test
  public void resolveViewNameWithRequestParameter() throws Exception {
    request.addParameter("format", "xls");

    Map<String, MediaType> mapping = Collections.singletonMap("xls", MediaType.valueOf("application/vnd.ms-excel"));
    ParameterContentNegotiationStrategy paramStrategy = new ParameterContentNegotiationStrategy(mapping);
    viewResolver.setContentNegotiationManager(new ContentNegotiationManager(paramStrategy));

    ViewResolver viewResolverMock = mock(ViewResolver.class);
    viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));
    viewResolver.afterPropertiesSet();

    View viewMock = mock(View.class, "application_xls");

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(null);
    given(viewResolverMock.resolveViewName(viewName + ".xls", locale)).willReturn(viewMock);
    given(viewMock.getContentType()).willReturn("application/vnd.ms-excel");

    View result = viewResolver.resolveViewName(viewName, locale);
    assertThat(result).as("Invalid view").isSameAs(viewMock);
  }

  @Test
  public void resolveViewNameWithDefaultContentType() throws Exception {
    request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

    MediaType mediaType = new MediaType("application", "xml");
    FixedContentNegotiationStrategy fixedStrategy = new FixedContentNegotiationStrategy(mediaType);
    viewResolver.setContentNegotiationManager(new ContentNegotiationManager(fixedStrategy));

    ViewResolver viewResolverMock1 = mock(ViewResolver.class, "viewResolver1");
    ViewResolver viewResolverMock2 = mock(ViewResolver.class, "viewResolver2");
    viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));
    viewResolver.afterPropertiesSet();

    View viewMock1 = mock(View.class, "application_xml");
    View viewMock2 = mock(View.class, "text_html");

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock1.resolveViewName(viewName, locale)).willReturn(viewMock1);
    given(viewResolverMock2.resolveViewName(viewName, locale)).willReturn(viewMock2);
    given(viewMock1.getContentType()).willReturn("application/xml");
    given(viewMock2.getContentType()).willReturn("text/html;charset=ISO-8859-1");

    View result = viewResolver.resolveViewName(viewName, locale);
    assertThat(result).as("Invalid view").isSameAs(viewMock1);
  }

  @Test
  public void resolveViewNameAcceptHeader() throws Exception {
    request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

    ViewResolver viewResolverMock1 = mock(ViewResolver.class);
    ViewResolver viewResolverMock2 = mock(ViewResolver.class);
    viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

    viewResolver.afterPropertiesSet();

    View viewMock1 = mock(View.class, "application_xml");
    View viewMock2 = mock(View.class, "text_html");

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock1.resolveViewName(viewName, locale)).willReturn(viewMock1);
    given(viewResolverMock2.resolveViewName(viewName, locale)).willReturn(viewMock2);
    given(viewMock1.getContentType()).willReturn("application/xml");
    given(viewMock2.getContentType()).willReturn("text/html;charset=ISO-8859-1");

    View result = viewResolver.resolveViewName(viewName, locale);
    assertThat(result).as("Invalid view").isSameAs(viewMock2);
  }

  // SPR-9160

  @Test
  public void resolveViewNameAcceptHeaderSortByQuality() throws Exception {
    request.addHeader("Accept", "text/plain;q=0.5, application/json");

    viewResolver.setContentNegotiationManager(new ContentNegotiationManager(new HeaderContentNegotiationStrategy()));

    ViewResolver htmlViewResolver = mock(ViewResolver.class);
    ViewResolver jsonViewResolver = mock(ViewResolver.class);
    viewResolver.setViewResolvers(Arrays.asList(htmlViewResolver, jsonViewResolver));

    View htmlView = mock(View.class, "text_html");
    View jsonViewMock = mock(View.class, "application_json");

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(htmlViewResolver.resolveViewName(viewName, locale)).willReturn(htmlView);
    given(jsonViewResolver.resolveViewName(viewName, locale)).willReturn(jsonViewMock);
    given(htmlView.getContentType()).willReturn("text/html");
    given(jsonViewMock.getContentType()).willReturn("application/json");

    View result = viewResolver.resolveViewName(viewName, locale);
    assertThat(result).as("Invalid view").isSameAs(jsonViewMock);
  }

  // SPR-9807

  @Test
  public void resolveViewNameAcceptHeaderWithSuffix() throws Exception {
    request.addHeader("Accept", "application/vnd.example-v2+xml");

    ViewResolver viewResolverMock = mock(ViewResolver.class);
    viewResolver.setViewResolvers(Arrays.asList(viewResolverMock));

    viewResolver.afterPropertiesSet();

    View viewMock = mock(View.class, "application_xml");

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
    given(viewMock.getContentType()).willReturn("application/*+xml");

    View result = viewResolver.resolveViewName(viewName, locale);

    assertThat(result).as("Invalid view").isSameAs(viewMock);
    assertThat(request.getAttribute(View.SELECTED_CONTENT_TYPE)).isEqualTo(new MediaType("application", "vnd.example-v2+xml"));
  }

  @Test
  public void resolveViewNameAcceptHeaderDefaultView() throws Exception {
    request.addHeader("Accept", "application/json");

    ViewResolver viewResolverMock1 = mock(ViewResolver.class);
    ViewResolver viewResolverMock2 = mock(ViewResolver.class);
    viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

    View viewMock1 = mock(View.class, "application_xml");
    View viewMock2 = mock(View.class, "text_html");
    View viewMock3 = mock(View.class, "application_json");

    List<View> defaultViews = new ArrayList<>();
    defaultViews.add(viewMock3);
    viewResolver.setDefaultViews(defaultViews);

    viewResolver.afterPropertiesSet();

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock1.resolveViewName(viewName, locale)).willReturn(viewMock1);
    given(viewResolverMock2.resolveViewName(viewName, locale)).willReturn(viewMock2);
    given(viewMock1.getContentType()).willReturn("application/xml");
    given(viewMock2.getContentType()).willReturn("text/html;charset=ISO-8859-1");
    given(viewMock3.getContentType()).willReturn("application/json");

    View result = viewResolver.resolveViewName(viewName, locale);
    assertThat(result).as("Invalid view").isSameAs(viewMock3);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void resolveViewNameFilename() throws Exception {
    request.setRequestURI("/test.html");

    ContentNegotiationManager manager =
            new ContentNegotiationManager(new PathExtensionContentNegotiationStrategy());

    ViewResolver viewResolverMock1 = mock(ViewResolver.class, "viewResolver1");
    ViewResolver viewResolverMock2 = mock(ViewResolver.class, "viewResolver2");
    viewResolver.setContentNegotiationManager(manager);
    viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

    viewResolver.afterPropertiesSet();

    View viewMock1 = mock(View.class, "application_xml");
    View viewMock2 = mock(View.class, "text_html");

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock1.resolveViewName(viewName, locale)).willReturn(viewMock1);
    given(viewResolverMock1.resolveViewName(viewName + ".html", locale)).willReturn(null);
    given(viewResolverMock2.resolveViewName(viewName, locale)).willReturn(null);
    given(viewResolverMock2.resolveViewName(viewName + ".html", locale)).willReturn(viewMock2);
    given(viewMock1.getContentType()).willReturn("application/xml");
    given(viewMock2.getContentType()).willReturn("text/html;charset=ISO-8859-1");

    View result = viewResolver.resolveViewName(viewName, locale);
    assertThat(result).as("Invalid view").isSameAs(viewMock2);
  }

  @Test
  public void resolveViewNameFilenameDefaultView() throws Exception {
    request.setRequestURI("/test.json");

    Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
    PathExtensionContentNegotiationStrategy pathStrategy =
            new PathExtensionContentNegotiationStrategy(mapping);
    viewResolver.setContentNegotiationManager(new ContentNegotiationManager(pathStrategy));

    ViewResolver viewResolverMock1 = mock(ViewResolver.class);
    ViewResolver viewResolverMock2 = mock(ViewResolver.class);
    viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

    View viewMock1 = mock(View.class, "application_xml");
    View viewMock2 = mock(View.class, "text_html");
    View viewMock3 = mock(View.class, "application_json");

    List<View> defaultViews = new ArrayList<>();
    defaultViews.add(viewMock3);
    viewResolver.setDefaultViews(defaultViews);

    viewResolver.afterPropertiesSet();

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock1.resolveViewName(viewName, locale)).willReturn(viewMock1);
    given(viewResolverMock1.resolveViewName(viewName + ".json", locale)).willReturn(null);
    given(viewResolverMock2.resolveViewName(viewName, locale)).willReturn(viewMock2);
    given(viewResolverMock2.resolveViewName(viewName + ".json", locale)).willReturn(null);
    given(viewMock1.getContentType()).willReturn("application/xml");
    given(viewMock2.getContentType()).willReturn("text/html;charset=ISO-8859-1");
    given(viewMock3.getContentType()).willReturn("application/json");

    View result = viewResolver.resolveViewName(viewName, locale);
    assertThat(result).as("Invalid view").isSameAs(viewMock3);
  }

  @Test
  public void resolveViewContentTypeNull() throws Exception {
    request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

    ViewResolver viewResolverMock = mock(ViewResolver.class);
    viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

    viewResolver.afterPropertiesSet();

    View viewMock = mock(View.class, "application_xml");

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
    given(viewMock.getContentType()).willReturn(null);

    View result = viewResolver.resolveViewName(viewName, locale);
    assertThat(result).as("Invalid view").isNull();
  }

  @Test
  public void resolveViewNoMatch() throws Exception {
    request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9");

    ViewResolver viewResolverMock = mock(ViewResolver.class);
    viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

    viewResolver.afterPropertiesSet();

    View viewMock = mock(View.class, "application_xml");

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
    given(viewMock.getContentType()).willReturn("application/pdf");

    View result = viewResolver.resolveViewName(viewName, locale);
    assertThat(result).as("Invalid view").isNull();
  }

  @Test
  public void resolveViewNoMatchUseUnacceptableStatus() throws Exception {
    viewResolver.setUseNotAcceptableStatusCode(true);
    request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9");

    ViewResolver viewResolverMock = mock(ViewResolver.class);
    viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

    viewResolver.afterPropertiesSet();

    View viewMock = mock(View.class, "application_xml");

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
    given(viewMock.getContentType()).willReturn("application/pdf");

    View result = viewResolver.resolveViewName(viewName, locale);
    assertThat(result).as("Invalid view").isNotNull();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    this.requestContext = new MockRequestContext(wac, request, response);
    RequestContextHolder.set(requestContext);
    result.render(null, requestContext);
    assertThat(response.getStatus()).as("Invalid status code set").isEqualTo(406);
  }

  @Test
  public void resolveQualityValue() throws Exception {
    request.addHeader("Accept", "text/html;q=0.9");

    ViewResolver viewResolverMock = mock(ViewResolver.class);
    viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

    viewResolver.afterPropertiesSet();

    View viewMock = mock(View.class, "text_html");

    String viewName = "view";
    Locale locale = Locale.ENGLISH;

    given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
    given(viewMock.getContentType()).willReturn("text/html");

    viewResolver.resolveViewName(viewName, locale);

    assertThat(request.getAttribute(View.SELECTED_CONTENT_TYPE)).isEqualTo(MediaType.TEXT_HTML);
  }

}
