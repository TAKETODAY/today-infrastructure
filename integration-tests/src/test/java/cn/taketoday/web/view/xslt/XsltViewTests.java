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

package cn.taketoday.web.view.xslt;

import org.assertj.core.api.Assertions;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.servlet.support.StaticWebApplicationContext;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockServletContext;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class XsltViewTests {

  private static final String HTML_OUTPUT = "/cn/taketoday/web/view/xslt/products.xsl";

  private final MockHttpServletRequest request = new MockHttpServletRequest();

  private final MockHttpServletResponse response = new MockHttpServletResponse();

  private final MockServletContext servletContext = new MockServletContext();

  @Test
  public void withNoSource() throws Exception {
    final XsltView view = getXsltView(HTML_OUTPUT);
    assertThatIllegalArgumentException().isThrownBy(() ->
            view.render(emptyMap(), ServletUtils.getRequestContext(request, response)));
  }

  @Test
  public void withoutUrl() throws Exception {
    final XsltView view = new XsltView();
    assertThatIllegalArgumentException().isThrownBy(
            view::afterPropertiesSet);
  }

  @Test
  public void simpleTransformWithSource() throws Exception {
    Source source = new StreamSource(getProductDataResource().getInputStream());
    doTestWithModel(singletonMap("someKey", source));
  }

  @Test
  public void testSimpleTransformWithDocument() throws Exception {
    org.w3c.dom.Document document = getDomDocument();
    doTestWithModel(singletonMap("someKey", document));
  }

  @Test
  public void testSimpleTransformWithNode() throws Exception {
    org.w3c.dom.Document document = getDomDocument();
    doTestWithModel(singletonMap("someKey", document.getDocumentElement()));
  }

  @Test
  public void testSimpleTransformWithInputStream() throws Exception {
    doTestWithModel(singletonMap("someKey", getProductDataResource().getInputStream()));
  }

  @Test
  public void testSimpleTransformWithReader() throws Exception {
    doTestWithModel(singletonMap("someKey", new InputStreamReader(getProductDataResource().getInputStream())));
  }

  @Test
  public void testSimpleTransformWithResource() throws Exception {
    doTestWithModel(singletonMap("someKey", getProductDataResource()));
  }

  @Test
  public void testWithSourceKey() throws Exception {
    XsltView view = getXsltView(HTML_OUTPUT);
    view.setSourceKey("actualData");

    Map<String, Object> model = new HashMap<>();
    model.put("actualData", getProductDataResource());
    model.put("otherData", new ClassPathResource("dummyData.xsl", getClass()));

    view.render(model, ServletUtils.getRequestContext(request, response));
    assertHtmlOutput(this.response.getContentAsString());
  }

  @Test
  public void testContentTypeCarriedFromTemplate() throws Exception {
    XsltView view = getXsltView(HTML_OUTPUT);

    Source source = new StreamSource(getProductDataResource().getInputStream());
    view.render(singletonMap("someKey", source), ServletUtils.getRequestContext(request, response));
    assertThat(this.response.getContentType().startsWith("text/html")).isTrue();
    assertThat(this.response.getCharacterEncoding()).isEqualTo("UTF-8");
  }

  @Test
  public void testModelParametersCarriedAcross() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("someKey", getProductDataResource());
    model.put("title", "Product List");
    doTestWithModel(model);
    assertThat(this.response.getContentAsString().contains("Product List")).isTrue();
  }

  @Test
  public void testStaticAttributesCarriedAcross() throws Exception {
    XsltView view = getXsltView(HTML_OUTPUT);
    view.setSourceKey("actualData");
    view.addStaticAttribute("title", "Product List");

    Map<String, Object> model = new HashMap<>();
    model.put("actualData", getProductDataResource());
    model.put("otherData", new ClassPathResource("dummyData.xsl", getClass()));

    view.render(model, ServletUtils.getRequestContext(request, response));
    assertHtmlOutput(this.response.getContentAsString());
    assertThat(this.response.getContentAsString().contains("Product List")).isTrue();

  }

  private org.w3c.dom.Document getDomDocument() throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = dbf.newDocumentBuilder();
    org.w3c.dom.Document document = builder.parse(getProductDataResource().getInputStream());
    return document;
  }

  private void doTestWithModel(Map<String, Object> model) throws Exception {
    XsltView view = getXsltView(HTML_OUTPUT);
    view.render(model, ServletUtils.getRequestContext(request, response));
    assertHtmlOutput(this.response.getContentAsString());
  }

  @SuppressWarnings("rawtypes")
  private void assertHtmlOutput(String output) throws Exception {
    SAXReader reader = new SAXReader();
    Document document = reader.read(new StringReader(output));
    List nodes = document.getRootElement().selectNodes("/html/body/table/tr");

    Element tr1 = (Element) nodes.get(0);
    assertRowElement(tr1, "1", "Whatsit", "12.99");
    Element tr2 = (Element) nodes.get(1);
    assertRowElement(tr2, "2", "Thingy", "13.99");
    Element tr3 = (Element) nodes.get(2);
    assertRowElement(tr3, "3", "Gizmo", "14.99");
    Element tr4 = (Element) nodes.get(3);
    assertRowElement(tr4, "4", "Cranktoggle", "11.99");
  }

  private void assertRowElement(Element elem, String id, String name, String price) {
    Element idElem = elem.elements().get(0);
    Element nameElem = elem.elements().get(1);
    Element priceElem = elem.elements().get(2);

    Assertions.assertThat(idElem.getText()).as("ID incorrect.").isEqualTo(id);
    Assertions.assertThat(nameElem.getText()).as("Name incorrect.").isEqualTo(name);
    Assertions.assertThat(priceElem.getText()).as("Price incorrect.").isEqualTo(price);
  }

  private XsltView getXsltView(String templatePath) {
    XsltView view = new XsltView();
    view.setUrl(templatePath);
    view.setApplicationContext(new StaticWebApplicationContext(servletContext));
    view.initApplicationContext();
    return view;
  }

  private Resource getProductDataResource() {
    return new ClassPathResource("productData.xml", getClass());
  }

}
