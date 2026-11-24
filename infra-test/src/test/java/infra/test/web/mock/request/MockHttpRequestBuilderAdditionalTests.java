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

package infra.test.web.mock.request;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.util.LinkedMultiValueMap;
import infra.web.RedirectModel;
import infra.web.client.ApiVersionInserter;
import infra.web.mock.DefaultMockContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockSession;
import infra.web.mock.api.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class MockHttpRequestBuilderAdditionalTests {

  private final DefaultMockContext context = new DefaultMockContext();

  @Test
  void formFieldsAreEncodedAndExposedAsParameters() throws Exception {
    LinkedMultiValueMap<String, String> extra = new LinkedMultiValueMap<>();
    extra.add("other", "a&b");
    MockRequest request = new MockHttpRequestBuilder(HttpMethod.POST, "/")
            .formField("name", "hello world").formFields(extra).buildRequest(context);
    assertThat(request.getContentAsString()).isEqualTo("name=hello+world&other=a%26b");
    assertThat(request.getContentType()).startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    assertThat(request.getParameter("name")).isEqualTo("hello world");
    assertThat(request.getParameter("other")).isEqualTo("a&b");

    MockRequest encoded = new MockHttpRequestBuilder(HttpMethod.POST, "/")
            .characterEncoding(StandardCharsets.ISO_8859_1).contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .content(new byte[0]).formField("name", "é").buildRequest(context);
    assertThat(encoded.getContentAsString()).isEqualTo("name=%E9");
    assertThat(encoded.getParameter("name")).isEqualTo("é");
  }

  @Test
  void formFieldsRejectExistingBodyAndIncompatibleContentType() {
    assertThatIllegalStateException().isThrownBy(() -> new MockHttpRequestBuilder(HttpMethod.POST, "/")
            .content("body").formField("a", "b").buildRequest(context))
            .withMessage("Could not write form data with an existing body");
    assertThatIllegalStateException().isThrownBy(() -> new MockHttpRequestBuilder(HttpMethod.POST, "/")
            .contentType(MediaType.APPLICATION_JSON).formField("a", "b").buildRequest(context))
            .withMessageContaining("is not compatible");
    MockRequest invalidType = new MockHttpRequestBuilder(HttpMethod.POST, "/")
            .contentType("invalid").content("body").buildRequest(context);
    assertThat(invalidType.getParameterMap()).isEmpty();
    assertThat(invalidType.getContentAsByteArray()).isEqualTo("body".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void contentLengthDoesNotOverrideExplicitLengthOrTransferEncoding() {
    MockRequest length = new MockHttpRequestBuilder(HttpMethod.POST, "/")
            .content("body").header(HttpHeaders.CONTENT_LENGTH, 10).buildRequest(context);
    assertThat(length.getHeader(HttpHeaders.CONTENT_LENGTH)).isEqualTo("10");
    MockRequest chunked = new MockHttpRequestBuilder(HttpMethod.POST, "/")
            .content("body").header(HttpHeaders.TRANSFER_ENCODING, "chunked").buildRequest(context);
    assertThat(chunked.getHeader(HttpHeaders.CONTENT_LENGTH)).isNull();
  }

  @Test
  void headerCollectionDateAndConsumerAreSupported() {
    ZonedDateTime modified = ZonedDateTime.parse("2026-01-01T00:00:00Z");
    MockRequest request = new MockHttpRequestBuilder(HttpMethod.GET, "/")
            .header("X-Multiple", List.of("one", "two"))
            .header("X-Date", new Date(0)).header("X-Mixed", 1, "two")
            .headers(headers -> headers.set("X-Consumer", "updated"))
            .acceptCharset(StandardCharsets.UTF_8).ifModifiedSince(modified).ifNoneMatch("\"tag\"")
            .buildRequest(context);
    assertThat(Collections.list(request.getHeaders("X-Multiple"))).containsExactly("one", "two");
    assertThat(request.getHeader("X-Date")).isEqualTo("Thu, 01 Jan 1970 00:00:00 GMT");
    assertThat(Collections.list(request.getHeaders("X-Mixed"))).containsExactly("1", "two");
    assertThat(request.getHeader("X-Consumer")).isEqualTo("updated");
    assertThat(request.getHeader(HttpHeaders.ACCEPT_CHARSET)).contains("utf-8");
    assertThat(request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)).isEqualTo(modified.toInstant().toEpochMilli());
    assertThat(request.getHeader(HttpHeaders.IF_NONE_MATCH)).isEqualTo("\"tag\"");
  }

  @Test
  void mergeInheritsMissingValuesWithoutOverridingChildValues() throws Exception {
    Principal principal = () -> "parent";
    MockSession session = new MockSession();
    MockHttpRequestBuilder parent = new MockHttpRequestBuilder(HttpMethod.POST, "/parent")
            .secure(true).principal(principal).session(session).remoteAddress("127.0.0.2")
            .characterEncoding(StandardCharsets.UTF_8).contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header("X-Shared", "parent").header("X-Inherited", "one", "two")
            .param("shared", "parent").param("inherited", "value")
            .queryParam("q", "parent").queryParam("other", "value")
            .formField("form", "parent").formField("otherForm", "value")
            .cookie(new Cookie("shared", "parent"), new Cookie("other", "value"))
            .locale(Locale.US, Locale.FRANCE)
            .requestAttr("shared", "parent").requestAttr("inherited", "value")
            .sessionAttr("shared", "parent").sessionAttr("inherited", "value")
            .redirectModelAttr("shared", "parent").redirectModelAttr("inherited", "value");
    MockHttpRequestBuilder child = new MockHttpRequestBuilder(HttpMethod.POST)
            .header("X-Shared", "child").param("shared", "child").queryParam("q", "child")
            .formField("form", "child").cookie(new Cookie("shared", "child"))
            .locale(Locale.US).requestAttr("shared", "child").sessionAttr("shared", "child")
            .redirectModelAttr("shared", "child");
    assertThat(child.isMergeEnabled()).isTrue();
    assertThat(child.merge(null)).isSameAs(child);
    assertThat(child.merge(parent)).isSameAs(child);
    MockRequest request = child.buildRequest(context);
    assertThat(request.getRequestURI()).isEqualTo("/parent");
    assertThat(request.isSecure()).isTrue();
    assertThat(request.getUserPrincipal()).isSameAs(principal);
    assertThat(request.getSession()).isSameAs(session);
    assertThat(request.getRemoteAddr()).isEqualTo("127.0.0.2");
    assertThat(request.getHeader("X-Shared")).isEqualTo("child");
    assertThat(Collections.list(request.getHeaders("X-Inherited"))).containsExactly("one", "two");
    assertThat(request.getParameter("shared")).isEqualTo("child");
    assertThat(request.getParameter("inherited")).isEqualTo("value");
    assertThat(request.getParameter("q")).isEqualTo("child");
    assertThat(request.getParameter("form")).isEqualTo("child");
    assertThat(request.getParameter("otherForm")).isEqualTo("value");
    assertThat(request.getCookies()).extracting(Cookie::getValue).containsExactly("child", "value");
    assertThat(Collections.list(request.getLocales())).containsExactly(Locale.US, Locale.FRANCE);
    assertThat(request.getAttribute("shared")).isEqualTo("child");
    assertThat(request.getAttribute("inherited")).isEqualTo("value");
    assertThat(session.getAttribute("shared")).isEqualTo("child");
    assertThat(session.getAttribute("inherited")).isEqualTo("value");
    RedirectModel redirectModel = (RedirectModel) request.getAttribute(RedirectModel.INPUT_ATTRIBUTE);
    assertThat(redirectModel.getAttribute("shared")).isEqualTo("child");
    assertThat(redirectModel.getAttribute("inherited")).isEqualTo("value");
    assertThatIllegalArgumentException().isThrownBy(() -> child.merge("invalid"))
            .withMessageContaining("Cannot merge");
  }

  @Test
  void mergeKeepsExplicitChildMetadataAndBody() {
    Principal principal = () -> "child";
    MockSession session = new MockSession();
    MockHttpRequestBuilder parent = new MockHttpRequestBuilder(HttpMethod.POST, "/parent")
            .secure(true).principal(() -> "parent").session(new MockSession()).remoteAddress("127.0.0.2")
            .characterEncoding(StandardCharsets.UTF_8).contentType(MediaType.TEXT_PLAIN).content("parent")
            .apiVersion("1").apiVersionInserter(ApiVersionInserter.forHeader("X-Version"));
    MockHttpRequestBuilder child = new MockHttpRequestBuilder(HttpMethod.POST, "/child")
            .secure(false).principal(principal).session(session).remoteAddress("127.0.0.3")
            .characterEncoding(StandardCharsets.ISO_8859_1).contentType(MediaType.APPLICATION_JSON).content("child")
            .apiVersion("2").apiVersionInserter(ApiVersionInserter.forHeader("X-Child-Version"));
    child.merge(parent);
    MockRequest request = child.buildRequest(context);
    assertThat(request.getRequestURI()).isEqualTo("/child");
    assertThat(request.isSecure()).isFalse();
    assertThat(request.getUserPrincipal()).isSameAs(principal);
    assertThat(request.getSession()).isSameAs(session);
    assertThat(request.getRemoteAddr()).isEqualTo("127.0.0.3");
    assertThat(request.getCharacterEncoding()).isEqualTo("ISO-8859-1");
    assertThat(request.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    assertThat(request.getContentAsByteArray()).isEqualTo("child".getBytes(StandardCharsets.UTF_8));
    assertThat(request.getHeader("X-Child-Version")).isEqualTo("2");
    assertThat(request.getHeader("X-Version")).isNull();
  }

  @Test
  void versionRequiresInserterAndCanBeInherited() {
    assertThatIllegalStateException().isThrownBy(() -> new MockHttpRequestBuilder(HttpMethod.GET, "/")
            .apiVersion("1").buildRequest(context)).withMessage("No ApiVersionInserter");
    MockHttpRequestBuilder parent = new MockHttpRequestBuilder(HttpMethod.GET, "/")
            .apiVersion("1").apiVersionInserter(ApiVersionInserter.forQueryParam("version"));
    MockHttpRequestBuilder child = new MockHttpRequestBuilder(HttpMethod.GET);
    child.merge(parent);
    assertThat(child.buildRequest(context).getParameter("version")).isEqualTo("1");
  }

  @Test
  void uriSetterValidatesAndExpandsTemplates() {
    MockHttpRequestBuilder builder = new MockHttpRequestBuilder(HttpMethod.GET);
    assertThatIllegalArgumentException().isThrownBy(() -> builder.buildRequest(context))
            .withMessage("'uri' is required");
    assertThatIllegalArgumentException().isThrownBy(() -> builder.uri("relative"));
    assertThatIllegalArgumentException().isThrownBy(() -> builder.uri((String) null));
    assertThat(builder.uri("").buildRequest(context).getRequestURI()).isEqualTo("/");
    MockRequest request = builder.uri("https://example.org/{name}", "a b").buildRequest(context);
    assertThat(request.getRequestURI()).isEqualTo("/a%20b");
    assertThat(request.getUriTemplate()).isEqualTo("https://example.org/{name}");
  }

}
