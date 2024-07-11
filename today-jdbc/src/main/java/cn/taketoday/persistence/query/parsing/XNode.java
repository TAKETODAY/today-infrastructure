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

package cn.taketoday.persistence.query.parsing;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/11 11:27
 */
public class XNode {

  private final Node node;

  private final String name;

  @Nullable
  private final String body;

  private final Properties attributes;

  @Nullable
  private final Properties variables;

  private final XPathParser xpathParser;

  public XNode(XPathParser xpathParser, Node node, @Nullable Properties variables) {
    this.xpathParser = xpathParser;
    this.node = node;
    this.name = node.getNodeName();
    this.variables = variables;
    this.attributes = parseAttributes(node);
    this.body = parseBody(node);
  }

  public XNode newXNode(Node node) {
    return new XNode(xpathParser, node, variables);
  }

  @Nullable
  public XNode getParent() {
    Node parent = node.getParentNode();
    if (!(parent instanceof Element)) {
      return null;
    }
    else {
      return new XNode(xpathParser, parent, variables);
    }
  }

  public String getPath() {
    StringBuilder builder = new StringBuilder();
    Node current = node;
    while (current instanceof Element) {
      if (current != node) {
        builder.insert(0, "/");
      }
      builder.insert(0, current.getNodeName());
      current = current.getParentNode();
    }
    return builder.toString();
  }

  @Nullable
  public String evalString(String expression) {
    return xpathParser.evalString(node, expression);
  }

  @Nullable
  public Boolean evalBoolean(String expression) {
    return xpathParser.evalBoolean(node, expression);
  }

  @Nullable
  public Double evalDouble(String expression) {
    return xpathParser.evalDouble(node, expression);
  }

  @Nullable
  public List<XNode> evalNodes(String expression) {
    return xpathParser.evalNodes(node, expression);
  }

  @Nullable
  public XNode evalNode(String expression) {
    return xpathParser.evalNode(node, expression);
  }

  public Node getNode() {
    return node;
  }

  public String getName() {
    return name;
  }

  @Nullable
  public String getStringBody() {
    return getStringBody(null);
  }

  @Nullable
  public String getStringBody(@Nullable String def) {
    return body == null ? def : body;
  }

  @Nullable
  public Boolean getBooleanBody() {
    return getBooleanBody(null);
  }

  @Nullable
  public Boolean getBooleanBody(@Nullable Boolean def) {
    return body == null ? def : Boolean.valueOf(body);
  }

  @Nullable
  public Integer getIntBody() {
    return getIntBody(null);
  }

  @Nullable
  public Integer getIntBody(@Nullable Integer def) {
    return body == null ? def : Integer.valueOf(body);
  }

  @Nullable
  public Long getLongBody() {
    return getLongBody(null);
  }

  @Nullable
  public Long getLongBody(@Nullable Long def) {
    return body == null ? def : Long.valueOf(body);
  }

  @Nullable
  public Double getDoubleBody() {
    return getDoubleBody(null);
  }

  @Nullable
  public Double getDoubleBody(@Nullable Double def) {
    return body == null ? def : Double.valueOf(body);
  }

  @Nullable
  public Float getFloatBody() {
    return getFloatBody(null);
  }

  @Nullable
  public Float getFloatBody(@Nullable Float def) {
    return body == null ? def : Float.valueOf(body);
  }

  public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name) {
    return getEnumAttribute(enumType, name, null);
  }

  public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name, T def) {
    String value = getStringAttribute(name);
    return value == null ? def : Enum.valueOf(enumType, value);
  }

  /**
   * Return a attribute value as String.
   *
   * <p>
   * If attribute value is absent, return value that provided from supplier of default value.
   *
   * @param name attribute name
   * @param defSupplier a supplier of default value
   * @return the string attribute
   */
  public String getStringAttribute(String name, Supplier<String> defSupplier) {
    String value = attributes.getProperty(name);
    return value == null ? defSupplier.get() : value;
  }

  @Nullable
  public String getStringAttribute(String name) {
    return getStringAttribute(name, (String) null);
  }

  @Nullable
  public String getStringAttribute(String name, @Nullable String def) {
    String value = attributes.getProperty(name);
    return value == null ? def : value;
  }

  @Nullable
  public Boolean getBooleanAttribute(String name) {
    return getBooleanAttribute(name, null);
  }

  @Nullable
  public Boolean getBooleanAttribute(String name, @Nullable Boolean def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Boolean.valueOf(value);
  }

  @Nullable
  public Integer getIntAttribute(String name) {
    return getIntAttribute(name, null);
  }

  @Nullable
  public Integer getIntAttribute(String name, @Nullable Integer def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Integer.valueOf(value);
  }

  @Nullable
  public Long getLongAttribute(String name) {
    return getLongAttribute(name, null);
  }

  @Nullable
  public Long getLongAttribute(String name, @Nullable Long def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Long.valueOf(value);
  }

  @Nullable
  public Double getDoubleAttribute(String name) {
    return getDoubleAttribute(name, null);
  }

  @Nullable
  public Double getDoubleAttribute(String name, @Nullable Double def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Double.valueOf(value);
  }

  @Nullable
  public Float getFloatAttribute(String name) {
    return getFloatAttribute(name, null);
  }

  public Float getFloatAttribute(String name, @Nullable Float def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Float.valueOf(value);
  }

  public List<XNode> getChildren() {
    List<XNode> children = new ArrayList<>();
    NodeList nodeList = node.getChildNodes();
    if (nodeList != null) {
      for (int i = 0, n = nodeList.getLength(); i < n; i++) {
        Node node = nodeList.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          children.add(new XNode(xpathParser, node, variables));
        }
      }
    }
    return children;
  }

  public Properties getChildrenAsProperties() {
    Properties properties = new Properties();
    for (XNode child : getChildren()) {
      String name = child.getStringAttribute("name");
      String value = child.getStringAttribute("value");
      if (name != null && value != null) {
        properties.setProperty(name, value);
      }
    }
    return properties;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    toString(builder, 0);
    return builder.toString();
  }

  private void toString(StringBuilder builder, int level) {
    builder.append("<");
    builder.append(name);
    for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
      builder.append(" ");
      builder.append(entry.getKey());
      builder.append("=\"");
      builder.append(entry.getValue());
      builder.append("\"");
    }
    List<XNode> children = getChildren();
    if (!children.isEmpty()) {
      builder.append(">\n");
      for (XNode child : children) {
        indent(builder, level + 1);
        child.toString(builder, level + 1);
      }
      indent(builder, level);
      builder.append("</");
      builder.append(name);
      builder.append(">");
    }
    else if (body != null) {
      builder.append(">");
      builder.append(body);
      builder.append("</");
      builder.append(name);
      builder.append(">");
    }
    else {
      builder.append("/>");
      indent(builder, level);
    }
    builder.append("\n");
  }

  private void indent(StringBuilder builder, int level) {
    builder.append("    ".repeat(Math.max(0, level)));
  }

  private Properties parseAttributes(Node n) {
    Properties attributes = new Properties();
    NamedNodeMap attributeNodes = n.getAttributes();
    if (attributeNodes != null) {
      for (int i = 0; i < attributeNodes.getLength(); i++) {
        Node attribute = attributeNodes.item(i);
        String value = attribute.getNodeValue();
        attributes.put(attribute.getNodeName(), value);
      }
    }
    return attributes;
  }

  @Nullable
  private String parseBody(Node node) {
    String data = getBodyData(node);
    if (data == null) {
      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        data = getBodyData(child);
        if (data != null) {
          break;
        }
      }
    }
    return data;
  }

  @Nullable
  private String getBodyData(Node child) {
    if (child.getNodeType() == Node.CDATA_SECTION_NODE
            || child.getNodeType() == Node.TEXT_NODE) {
      return ((CharacterData) child).getData();
    }
    return null;
  }

}
