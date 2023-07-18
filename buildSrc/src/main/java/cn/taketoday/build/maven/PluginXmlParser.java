/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.build.maven;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * A parser for a Maven plugin's {@code plugin.xml} file.
 *
 * @author Andy Wilkinson
 * @author Mike Smithson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class PluginXmlParser {

  private final XPath xpath;

  PluginXmlParser() {
    this.xpath = XPathFactory.newInstance().newXPath();
  }

  Plugin parse(File pluginXml) {
    try {
      Node root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pluginXml);
      List<Mojo> mojos = parseMojos(root);
      return new Plugin(textAt("//plugin/groupId", root), textAt("//plugin/artifactId", root),
              textAt("//plugin/version", root), textAt("//plugin/goalPrefix", root), mojos);
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private String textAt(String path, Node source) throws XPathExpressionException {
    String text = this.xpath.evaluate(path + "/text()", source);
    return text.isEmpty() ? null : text;
  }

  private List<Mojo> parseMojos(Node plugin) throws XPathExpressionException {
    List<Mojo> mojos = new ArrayList<>();
    for (Node mojoNode : nodesAt("//plugin/mojos/mojo", plugin)) {
      mojos.add(new Mojo(textAt("goal", mojoNode), format(textAt("description", mojoNode)),
              parseParameters(mojoNode)));
    }
    return mojos;
  }

  private Iterable<Node> nodesAt(String path, Node source) throws XPathExpressionException {
    return IterableNodeList.of((NodeList) this.xpath.evaluate(path, source, XPathConstants.NODESET));
  }

  private List<Parameter> parseParameters(Node mojoNode) throws XPathExpressionException {
    Map<String, String> defaultValues = new HashMap<>();
    Map<String, String> userProperties = new HashMap<>();
    for (Node parameterConfigurationNode : nodesAt("configuration/*", mojoNode)) {
      String userProperty = parameterConfigurationNode.getTextContent();
      if (userProperty != null && !userProperty.isEmpty()) {
        userProperties.put(parameterConfigurationNode.getNodeName(),
                userProperty.replace("${", "`").replace("}", "`"));
      }
      Node defaultValueAttribute = parameterConfigurationNode.getAttributes().getNamedItem("default-value");
      if (defaultValueAttribute != null && !defaultValueAttribute.getTextContent().isEmpty()) {
        defaultValues.put(parameterConfigurationNode.getNodeName(), defaultValueAttribute.getTextContent());
      }
    }
    List<Parameter> parameters = new ArrayList<>();
    for (Node parameterNode : nodesAt("parameters/parameter", mojoNode)) {
      parameters.add(parseParameter(parameterNode, defaultValues, userProperties));
    }
    return parameters;
  }

  private Parameter parseParameter(Node parameterNode, Map<String, String> defaultValues,
          Map<String, String> userProperties) throws XPathExpressionException {
    String description = textAt("description", parameterNode);
    return new Parameter(textAt("name", parameterNode), textAt("type", parameterNode),
            booleanAt("required", parameterNode), booleanAt("editable", parameterNode),
            (description != null) ? format(description) : "", defaultValues.get(textAt("name", parameterNode)),
            userProperties.get(textAt("name", parameterNode)), textAt("since", parameterNode));
  }

  private boolean booleanAt(String path, Node node) throws XPathExpressionException {
    return Boolean.parseBoolean(textAt(path, node));
  }

  private String format(String input) {
    return input.replace("<code>", "`")
            .replace("</code>", "`")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("<br>", " ")
            .replace("\n", " ")
            .replace("&quot;", "\"")
            .replaceAll("\\{@code (.*?)}", "`$1`")
            .replaceAll("\\{@link (.*?)}", "`$1`")
            .replaceAll("\\{@literal (.*?)}", "`$1`")
            .replaceAll("<a href=.\"(.*?)\".>(.*?)</a>", "$1[$2]");
  }

  private static final class IterableNodeList implements Iterable<Node> {

    private final NodeList nodeList;

    private IterableNodeList(NodeList nodeList) {
      this.nodeList = nodeList;
    }

    private static Iterable<Node> of(NodeList nodeList) {
      return new IterableNodeList(nodeList);
    }

    @Override
    public Iterator<Node> iterator() {

      return new Iterator<>() {

        private int index = 0;

        @Override
        public boolean hasNext() {
          return this.index < IterableNodeList.this.nodeList.getLength();
        }

        @Override
        public Node next() {
          return IterableNodeList.this.nodeList.item(this.index++);
        }

      };
    }

  }

  static final class Plugin {

    public final String groupId;

    public final String artifactId;

    public final String version;

    public final String goalPrefix;

    public final List<Mojo> mojos;

    private Plugin(String groupId, String artifactId, String version, String goalPrefix, List<Mojo> mojos) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
      this.goalPrefix = goalPrefix;
      this.mojos = mojos;
    }

  }

  static final class Mojo {

    public final String goal;

    public final String description;

    public final List<Parameter> parameters;

    private Mojo(String goal, String description, List<Parameter> parameters) {
      this.goal = goal;
      this.description = description;
      this.parameters = parameters;
    }

  }

  static final class Parameter {

    public final String name;

    public final String type;

    public final boolean required;

    public final boolean editable;

    public final String description;

    public final String defaultValue;

    public final String userProperty;

    public final String since;

    private Parameter(String name, String type, boolean required, boolean editable, String description,
            String defaultValue, String userProperty, String since) {
      this.name = name;
      this.type = type;
      this.required = required;
      this.editable = editable;
      this.description = description;
      this.defaultValue = defaultValue;
      this.userProperty = userProperty;
      this.since = since;
    }

    boolean isRequired() {
      return this.required;
    }

    boolean isEditable() {
      return this.editable;
    }

  }

}
