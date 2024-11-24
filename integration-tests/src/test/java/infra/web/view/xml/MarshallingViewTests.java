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

package infra.web.view.xml;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamResult;

import infra.oxm.Marshaller;
import infra.validation.BeanPropertyBindingResult;
import infra.validation.BindingResult;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import jakarta.xml.bind.JAXBElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/4/8 12:01
 */
class MarshallingViewTests {

  private Marshaller marshallerMock = mock();

  private MarshallingView view = new MarshallingView(marshallerMock);

  @Test
  void getContentType() {
    assertThat(view.getContentType()).as("Invalid content type").isEqualTo("application/xml");
  }

  @Test
  void isExposePathVars() {
    assertThat(view.isExposePathVariables()).as("Must not expose path variables").isFalse();
  }

  @Test
  void isExposePathVarsDefaultConstructor() {
    assertThat(new MarshallingView().isExposePathVariables()).as("Must not expose path variables").isFalse();
  }

  @Test
  void renderModelKey() throws Exception {
    Object toBeMarshalled = new Object();
    String modelKey = "key";
    view.setModelKey(modelKey);
    Map<String, Object> model = new HashMap<>();
    model.put(modelKey, toBeMarshalled);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    given(marshallerMock.supports(Object.class)).willReturn(true);
    marshallerMock.marshal(eq(toBeMarshalled), isA(StreamResult.class));

    RequestContext context = new MockRequestContext(null, request, response);

    view.render(model, context);
    assertThat(response.getContentType()).as("Invalid content type").isEqualTo("application/xml");
    assertThat(response.getContentLength()).as("Invalid content length").isEqualTo(0);
  }

  @Test
  void renderModelKeyWithJaxbElement() throws Exception {
    String toBeMarshalled = "value";
    String modelKey = "key";
    view.setModelKey(modelKey);
    Map<String, Object> model = new HashMap<>();
    model.put(modelKey, new JAXBElement<>(new QName("model"), String.class, toBeMarshalled));

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    given(marshallerMock.supports(String.class)).willReturn(true);
    marshallerMock.marshal(eq(toBeMarshalled), isA(StreamResult.class));

    RequestContext context = new MockRequestContext(null, request, response);

    view.render(model, context);
    assertThat(response.getContentType()).as("Invalid content type").isEqualTo("application/xml");
    assertThat(response.getContentLength()).as("Invalid content length").isEqualTo(0);
  }

  @Test
  void renderInvalidModelKey() throws Exception {
    Object toBeMarshalled = new Object();
    String modelKey = "key";
    view.setModelKey("invalidKey");
    Map<String, Object> model = new HashMap<>();
    model.put(modelKey, toBeMarshalled);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    RequestContext context = new MockRequestContext(null, request, response);

    assertThatIllegalStateException()
            .isThrownBy(() ->
                    view.render(model, context));

    assertThat(response.getContentLength()).as("Invalid content length").isEqualTo(0);
  }

  @Test
  void renderNullModelValue() throws Exception {
    String modelKey = "key";
    Map<String, Object> model = new HashMap<>();
    model.put(modelKey, null);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    RequestContext context = new MockRequestContext(null, request, response);

    assertThatIllegalStateException().isThrownBy(() ->
            view.render(model, context));

    assertThat(response.getContentLength()).as("Invalid content length").isEqualTo(0);
  }

  @Test
  void renderModelKeyUnsupported() throws Exception {
    Object toBeMarshalled = new Object();
    String modelKey = "key";
    view.setModelKey(modelKey);
    Map<String, Object> model = new HashMap<>();
    model.put(modelKey, toBeMarshalled);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    given(marshallerMock.supports(Object.class)).willReturn(false);

    RequestContext context = new MockRequestContext(null, request, response);

    assertThatIllegalStateException().isThrownBy(() ->
            view.render(model, context));
  }

  @Test
  void renderNoModelKey() throws Exception {
    Object toBeMarshalled = new Object();
    String modelKey = "key";
    Map<String, Object> model = new HashMap<>();
    model.put(modelKey, toBeMarshalled);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    given(marshallerMock.supports(Object.class)).willReturn(true);

    RequestContext context = new MockRequestContext(null, request, response);

    view.render(model, context);
    assertThat(response.getContentType()).as("Invalid content type").isEqualTo("application/xml");
    assertThat(response.getContentLength()).as("Invalid content length").isEqualTo(0);
    verify(marshallerMock).marshal(eq(toBeMarshalled), isA(StreamResult.class));
  }

  @Test
  void renderNoModelKeyAndBindingResultFirst() throws Exception {
    Object toBeMarshalled = new Object();
    String modelKey = "key";
    Map<String, Object> model = new LinkedHashMap<>();
    model.put(BindingResult.MODEL_KEY_PREFIX + modelKey, new BeanPropertyBindingResult(toBeMarshalled, modelKey));
    model.put(modelKey, toBeMarshalled);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    given(marshallerMock.supports(BeanPropertyBindingResult.class)).willReturn(true);
    given(marshallerMock.supports(Object.class)).willReturn(true);

    RequestContext context = new MockRequestContext(null, request, response);

    view.render(model, context);
    assertThat(response.getContentType()).as("Invalid content type").isEqualTo("application/xml");
    assertThat(response.getContentLength()).as("Invalid content length").isEqualTo(0);
    verify(marshallerMock).marshal(eq(toBeMarshalled), isA(StreamResult.class));
  }

  @Test
  void testRenderUnsupportedModel() throws Exception {
    Object toBeMarshalled = new Object();
    String modelKey = "key";
    Map<String, Object> model = new HashMap<>();
    model.put(modelKey, toBeMarshalled);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    given(marshallerMock.supports(Object.class)).willReturn(false);

    RequestContext context = new MockRequestContext(null, request, response);

    assertThatIllegalStateException().isThrownBy(() ->
            view.render(model, context));
  }

}