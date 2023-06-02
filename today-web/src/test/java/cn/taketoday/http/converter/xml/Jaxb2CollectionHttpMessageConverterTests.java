/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.converter.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;

import cn.taketoday.core.TypeReference;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.MockHttpInputMessage;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.lang.Nullable;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test fixture for {@link Jaxb2CollectionHttpMessageConverter}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class Jaxb2CollectionHttpMessageConverterTests {

  private Jaxb2CollectionHttpMessageConverter<?> converter;

  private Type rootElementListType;

  private Type rootElementSetType;

  private Type typeListType;

  private Type typeSetType;

  @BeforeEach
  public void setup() {
    converter = new Jaxb2CollectionHttpMessageConverter<Collection<Object>>();
    rootElementListType = new TypeReference<List<RootElement>>() { }.getType();
    rootElementSetType = new TypeReference<Set<RootElement>>() { }.getType();
    typeListType = new TypeReference<List<TestType>>() { }.getType();
    typeSetType = new TypeReference<Set<TestType>>() { }.getType();
  }

  @Test
  public void canRead() {
    assertThat(converter.canRead(rootElementListType, null, null)).isTrue();
    assertThat(converter.canRead(rootElementSetType, null, null)).isTrue();
    assertThat(converter.canRead(typeSetType, null, null)).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void readXmlRootElementList() throws Exception {
    String content = "<list><rootElement><type s=\"1\"/></rootElement><rootElement><type s=\"2\"/></rootElement></list>";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
    List<RootElement> result = (List<RootElement>) converter.read(rootElementListType, null, inputMessage);

    assertThat(result.size()).as("Invalid result").isEqualTo(2);
    assertThat(result.get(0).type.s).as("Invalid result").isEqualTo("1");
    assertThat(result.get(1).type.s).as("Invalid result").isEqualTo("2");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void readXmlRootElementSet() throws Exception {
    String content = "<set><rootElement><type s=\"1\"/></rootElement><rootElement><type s=\"2\"/></rootElement></set>";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
    Set<RootElement> result = (Set<RootElement>) converter.read(rootElementSetType, null, inputMessage);

    assertThat(result.size()).as("Invalid result").isEqualTo(2);
    assertThat(result.contains(new RootElement("1"))).as("Invalid result").isTrue();
    assertThat(result.contains(new RootElement("2"))).as("Invalid result").isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void readXmlTypeList() throws Exception {
    String content = "<list><foo s=\"1\"/><bar s=\"2\"/></list>";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
    List<TestType> result = (List<TestType>) converter.read(typeListType, null, inputMessage);

    assertThat(result.size()).as("Invalid result").isEqualTo(2);
    assertThat(result.get(0).s).as("Invalid result").isEqualTo("1");
    assertThat(result.get(1).s).as("Invalid result").isEqualTo("2");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void readXmlTypeSet() throws Exception {
    String content = "<set><foo s=\"1\"/><bar s=\"2\"/></set>";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
    Set<TestType> result = (Set<TestType>) converter.read(typeSetType, null, inputMessage);

    assertThat(result.size()).as("Invalid result").isEqualTo(2);
    assertThat(result.contains(new TestType("1"))).as("Invalid result").isTrue();
    assertThat(result.contains(new TestType("2"))).as("Invalid result").isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void readXmlRootElementExternalEntityDisabled() throws Exception {
    Resource external = new ClassPathResource("external.txt", getClass());
    String content = "<!DOCTYPE root [" +
            "  <!ELEMENT external ANY >\n" +
            "  <!ENTITY ext SYSTEM \"" + external.getURI() + "\" >]>" +
            "  <list><rootElement><type s=\"1\"/><external>&ext;</external></rootElement></list>";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));

    converter = new Jaxb2CollectionHttpMessageConverter<Collection<Object>>() {
      @Override
      protected XMLInputFactory createXmlInputFactory() {
        XMLInputFactory inputFactory = super.createXmlInputFactory();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, true);
        return inputFactory;
      }
    };

    try {
      Collection<RootElement> result = converter.read(rootElementListType, null, inputMessage);
      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().external).isEqualTo("");
    }
    catch (HttpMessageNotReadableException ex) {
      // Some parsers raise an exception
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void readXmlRootElementExternalEntityEnabled() throws Exception {
    Resource external = new ClassPathResource("external.txt", getClass());
    String content = "<!DOCTYPE root [" +
            "  <!ELEMENT external ANY >\n" +
            "  <!ENTITY ext SYSTEM \"" + external.getURI() + "\" >]>" +
            "  <list><rootElement><type s=\"1\"/><external>&ext;</external></rootElement></list>";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));

    Jaxb2CollectionHttpMessageConverter<?> c = new Jaxb2CollectionHttpMessageConverter<Collection<Object>>() {
      @Override
      protected XMLInputFactory createXmlInputFactory() {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
        return inputFactory;
      }
    };

    Collection<RootElement> result = c.read(rootElementListType, null, inputMessage);
    assertThat(result).hasSize(1);
    assertThat(result.iterator().next().external).isEqualTo("Foo Bar");
  }

  @Test
  public void testXmlBomb() throws Exception {
    // https://en.wikipedia.org/wiki/Billion_laughs
    // https://msdn.microsoft.com/en-us/magazine/ee335713.aspx
    String content = """
            <?xml version="1.0"?>
            <!DOCTYPE lolz [
            	<!ENTITY lol "lol">
            	<!ELEMENT lolz (#PCDATA)>
            	<!ENTITY lol1 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
            	<!ENTITY lol2 "&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;">
            	<!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
            	<!ENTITY lol4 "&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;">
            	<!ENTITY lol5 "&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;">
            	<!ENTITY lol6 "&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;">
            	<!ENTITY lol7 "&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;">
            	<!ENTITY lol8 "&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;">
            	<!ENTITY lol9 "&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;">
            ]>
            <list><rootElement><external>&lol9;</external></rootElement></list>""";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
    assertThatExceptionOfType(HttpMessageNotReadableException.class)
            .isThrownBy(() -> this.converter.read(this.rootElementListType, null, inputMessage))
            .withMessageContaining("\"lol9\"");
  }

  @XmlRootElement
  public static class RootElement {

    public RootElement() {
    }

    public RootElement(String s) {
      this.type = new TestType(s);
    }

    @XmlElement
    public TestType type = new TestType();

    @XmlElement(required = false)
    public String external;

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof RootElement other) {
        return this.type.equals(other.type);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return type.hashCode();
    }
  }

  @XmlType
  public static class TestType {

    public TestType() {
    }

    public TestType(String s) {
      this.s = s;
    }

    @XmlAttribute
    public String s = "Hello World";

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof TestType other) {
        return this.s.equals(other.s);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return s.hashCode();
    }
  }

}
