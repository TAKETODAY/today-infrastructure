/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.view.xml;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import cn.taketoday.http.MediaType;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mock.MockHttpServletRequest;
import cn.taketoday.web.mock.MockHttpServletResponse;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.view.View;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
public class MappingJackson2XmlViewTests {

  private MappingJackson2XmlView view = new MappingJackson2XmlView();

  private MockHttpServletRequest request = new MockHttpServletRequest();

  private MockHttpServletResponse response = new MockHttpServletResponse();

  private Context jsContext = ContextFactory.getGlobal().enterContext();

  private ScriptableObject jsScope = jsContext.initStandardObjects();
  RequestContext context = new ServletRequestContext(null, request, response);

  @Test
  public void isExposePathVars() {
    assertThat(view.isExposePathVariables()).as("Must not expose path variables").isFalse();
  }

  @Test
  public void renderSimpleMap() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("foo", "bar");
    RequestContext context = new ServletRequestContext(null, request, response);

    view.setUpdateContentLength(true);
    view.render(model, context);

    assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");

    MediaType mediaType = MediaType.parseMediaType(response.getContentType());
    assertThat(mediaType.isCompatibleWith(MediaType.parseMediaType(MappingJackson2XmlView.DEFAULT_CONTENT_TYPE))).isTrue();

    String jsonResult = response.getContentAsString();
    assertThat(jsonResult.length() > 0).isTrue();
    assertThat(response.getContentLength()).isEqualTo(jsonResult.length());

    validateResult();
  }

  @Test
  public void renderWithSelectedContentType() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("foo", "bar");
    RequestContext context = new ServletRequestContext(null, request, response);

    view.render(model, context);
    MediaType mediaType = MediaType.parseMediaType(response.getContentType());
    assertThat(mediaType.isCompatibleWith(MediaType.APPLICATION_XML)).isTrue();

    request.setAttribute(View.SELECTED_CONTENT_TYPE, new MediaType("application", "vnd.example-v2+xml"));
    view.render(model, context);

    mediaType = MediaType.parseMediaType(response.getContentType());
    assertThat(mediaType.isCompatibleWith(MediaType.parseMediaType("application/vnd.example-v2+xml"))).isTrue();
  }

  @Test
  public void renderCaching() throws Exception {
    view.setDisableCaching(false);

    Map<String, Object> model = new HashMap<>();
    model.put("foo", "bar");

    view.render(model, context);

    assertThat(response.getHeader("Cache-Control")).isNull();
  }

  @Test
  public void renderSimpleBean() throws Exception {
    Object bean = new TestBeanSimple();
    Map<String, Object> model = new HashMap<>();
    model.put("foo", bean);

    view.setUpdateContentLength(true);
    view.render(model, context);

    assertThat(response.getContentAsString().length() > 0).isTrue();
    assertThat(response.getContentLength()).isEqualTo(response.getContentAsString().length());

    validateResult();
  }

  @Test
  public void renderWithCustomSerializerLocatedByAnnotation() throws Exception {
    Object bean = new TestBeanSimpleAnnotated();
    Map<String, Object> model = new HashMap<>();
    model.put("foo", bean);

    view.render(model, context);

    assertThat(response.getContentAsString().length() > 0).isTrue();
    assertThat(response.getContentAsString().contains("<testBeanSimple>custom</testBeanSimple>")).isTrue();

    validateResult();
  }

  @Test
  public void renderWithCustomSerializerLocatedByFactory() throws Exception {
    SerializerFactory factory = new DelegatingSerializerFactory(null);
    XmlMapper mapper = new XmlMapper();
    mapper.setSerializerFactory(factory);
    view.setObjectMapper(mapper);

    Object bean = new TestBeanSimple();
    Map<String, Object> model = new HashMap<>();
    model.put("foo", bean);

    view.render(model, context);

    String result = response.getContentAsString();
    assertThat(result.length() > 0).isTrue();
    assertThat(result.contains("custom</testBeanSimple>")).isTrue();

    validateResult();
  }

  @Test
  public void renderOnlySpecifiedModelKey() throws Exception {

    view.setModelKey("bar");
    Map<String, Object> model = new HashMap<>();
    model.put("foo", "foo");
    model.put("bar", "bar");
    model.put("baz", "baz");

    view.render(model, context);

    String result = response.getContentAsString();
    assertThat(result.length() > 0).isTrue();
    assertThat(result.contains("foo")).isFalse();
    assertThat(result.contains("bar")).isTrue();
    assertThat(result.contains("baz")).isFalse();

    validateResult();
  }

  @Test
  public void renderModelWithMultipleKeys() throws Exception {
    Map<String, Object> model = new TreeMap<>();
    model.put("foo", "foo");
    model.put("bar", "bar");

    assertThatIllegalStateException().isThrownBy(() ->
            view.render(model, context));
  }

  @Test
  public void renderSimpleBeanWithJsonView() throws Exception {
    Object bean = new TestBeanSimple();
    Map<String, Object> model = new HashMap<>();
    model.put("foo", bean);
    model.put(JsonView.class.getName(), MyJacksonView1.class);

    view.setUpdateContentLength(true);
    view.render(model, context);

    String content = response.getContentAsString();
    assertThat(content.length() > 0).isTrue();
    assertThat(response.getContentLength()).isEqualTo(content.length());
    assertThat(content.contains("foo")).isTrue();
    assertThat(content.contains("boo")).isFalse();
    assertThat(content.contains(JsonView.class.getName())).isFalse();
  }

  private void validateResult() throws Exception {
    Object xmlResult =
            jsContext.evaluateString(jsScope, "(" + response.getContentAsString() + ")", "XML Stream", 1, null);
    assertThat(xmlResult).as("XML Result did not eval as valid JavaScript").isNotNull();
    MediaType mediaType = MediaType.parseMediaType(response.getContentType());
    assertThat(mediaType.isCompatibleWith(MediaType.APPLICATION_XML)).isTrue();
  }

  public interface MyJacksonView1 {
  }

  public interface MyJacksonView2 {
  }

  @SuppressWarnings("unused")
  public static class TestBeanSimple {

    @JsonView(MyJacksonView1.class)
    private String property1 = "foo";

    private boolean test = false;

    @JsonView(MyJacksonView2.class)
    private String property2 = "boo";

    private TestChildBean child = new TestChildBean();

    public String getProperty1() {
      return property1;
    }

    public boolean getTest() {
      return test;
    }

    public String getProperty2() {
      return property2;
    }

    public Date getNow() {
      return new Date();
    }

    public TestChildBean getChild() {
      return child;
    }
  }

  @JsonSerialize(using = TestBeanSimpleSerializer.class)
  public static class TestBeanSimpleAnnotated extends TestBeanSimple {
  }

  public static class TestChildBean {

    private String value = "bar";

    private String baz = null;

    private TestBeanSimple parent = null;

    public String getValue() {
      return value;
    }

    public String getBaz() {
      return baz;
    }

    public TestBeanSimple getParent() {
      return parent;
    }

    public void setParent(TestBeanSimple parent) {
      this.parent = parent;
    }
  }

  public static class TestBeanSimpleSerializer extends JsonSerializer<Object> {

    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      jgen.writeStartObject();
      jgen.writeFieldName("testBeanSimple");
      jgen.writeString("custom");
      jgen.writeEndObject();
    }
  }

  @SuppressWarnings("serial")
  public static class DelegatingSerializerFactory extends BeanSerializerFactory {

    protected DelegatingSerializerFactory(SerializerFactoryConfig config) {
      super(config);
    }

    @Override
    public JsonSerializer<Object> createSerializer(SerializerProvider prov, JavaType type) throws JsonMappingException {
      if (type.getRawClass() == TestBeanSimple.class) {
        return new TestBeanSimpleSerializer();
      }
      else {
        return super.createSerializer(prov, type);
      }
    }
  }

}
