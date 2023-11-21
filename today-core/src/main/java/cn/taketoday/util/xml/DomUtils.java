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

package cn.taketoday.util.xml;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Convenience methods for working with the DOM API,
 * in particular for working with DOM Nodes and DOM Elements.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Costin Leau
 * @author Arjen Poutsma
 * @author Luke Taylor
 * @see Node
 * @see Element
 * @since 4.0
 */
public abstract class DomUtils {

  /**
   * Retrieves all child elements of the given DOM element that match any of the given element names.
   * Only looks at the direct child level of the given element; do not go into further depth
   * (in contrast to the DOM API's {@code getElementsByTagName} method).
   *
   * @param ele the DOM element to analyze
   * @param childEleNames the child element names to look for
   * @return a List of child {@code org.w3c.dom.Element} instances
   * @see Element
   * @see Element#getElementsByTagName
   */
  public static List<Element> getChildElementsByTagName(Element ele, String... childEleNames) {
    Assert.notNull(ele, "Element is required");
    Assert.notNull(childEleNames, "Element names collection is required");
    List<String> childEleNameList = Arrays.asList(childEleNames);
    NodeList nl = ele.getChildNodes();
    ArrayList<Element> childEles = new ArrayList<>();
    int length = nl.getLength();
    for (int i = 0; i < length; i++) {
      Node node = nl.item(i);
      if (node instanceof Element && nodeNameMatch(node, childEleNameList)) {
        childEles.add((Element) node);
      }
    }
    return childEles;
  }

  /**
   * Retrieves all child elements of the given DOM element that match the given element name.
   * Only look at the direct child level of the given element; do not go into further depth
   * (in contrast to the DOM API's {@code getElementsByTagName} method).
   *
   * @param ele the DOM element to analyze
   * @param childEleName the child element name to look for
   * @return a List of child {@code org.w3c.dom.Element} instances
   * @see Element
   * @see Element#getElementsByTagName
   */
  public static List<Element> getChildElementsByTagName(Element ele, String childEleName) {
    return getChildElementsByTagName(ele, new String[] { childEleName });
  }

  /**
   * Utility method that returns the first child element identified by its name.
   *
   * @param ele the DOM element to analyze
   * @param childEleName the child element name to look for
   * @return the {@code org.w3c.dom.Element} instance, or {@code null} if none found
   */
  @Nullable
  public static Element getChildElementByTagName(Element ele, String childEleName) {
    Assert.notNull(ele, "Element is required");
    Assert.notNull(childEleName, "Element name is required");
    NodeList nl = ele.getChildNodes();
    int length = nl.getLength();
    for (int i = 0; i < length; i++) {
      Node node = nl.item(i);
      if (node instanceof Element && nodeNameMatch(node, childEleName)) {
        return (Element) node;
      }
    }
    return null;
  }

  /**
   * Utility method that returns the first child element value identified by its name.
   *
   * @param ele the DOM element to analyze
   * @param childEleName the child element name to look for
   * @return the extracted text value, or {@code null} if no child element found
   */
  @Nullable
  public static String getChildElementValueByTagName(Element ele, String childEleName) {
    Element child = getChildElementByTagName(ele, childEleName);
    return child != null ? getTextValue(child) : null;
  }

  /**
   * Retrieves all child elements of the given DOM element.
   *
   * @param ele the DOM element to analyze
   * @return a List of child {@code org.w3c.dom.Element} instances
   */
  public static List<Element> getChildElements(Element ele) {
    Assert.notNull(ele, "Element is required");
    ArrayList<Element> childEles = new ArrayList<>();
    NodeList nl = ele.getChildNodes();
    int length = nl.getLength();
    for (int i = 0; i < length; i++) {
      Node node = nl.item(i);
      if (node instanceof Element element) {
        childEles.add(element);
      }
    }
    return childEles;
  }

  /**
   * Extracts the text value from the given DOM element, ignoring XML comments.
   * <p>Appends all CharacterData nodes and EntityReference nodes into a single
   * String value, excluding Comment nodes. Only exposes actual user-specified
   * text, no default values of any kind.
   *
   * @see CharacterData
   * @see EntityReference
   * @see Comment
   */
  public static String getTextValue(Element valueEle) {
    Assert.notNull(valueEle, "Element is required");
    StringBuilder sb = new StringBuilder();
    NodeList nl = valueEle.getChildNodes();
    int length = nl.getLength();
    for (int i = 0; i < length; i++) {
      Node item = nl.item(i);
      if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
        sb.append(item.getNodeValue());
      }
    }
    return sb.toString();
  }

  /**
   * Namespace-aware equals comparison. Returns {@code true} if either
   * {@link Node#getLocalName} or {@link Node#getNodeName} equals
   * {@code desiredName}, otherwise returns {@code false}.
   */
  public static boolean nodeNameEquals(Node node, String desiredName) {
    Assert.notNull(node, "Node is required");
    Assert.notNull(desiredName, "Desired name is required");
    return nodeNameMatch(node, desiredName);
  }

  /**
   * Returns a SAX {@code ContentHandler} that transforms callback calls to DOM {@code Node}s.
   *
   * @param node the node to publish events to
   * @return the content handler
   */
  public static ContentHandler createContentHandler(Node node) {
    return new DomContentHandler(node);
  }

  /**
   * Matches the given node's name and local name against the given desired name.
   */
  private static boolean nodeNameMatch(Node node, String desiredName) {
    return desiredName.equals(node.getNodeName()) || desiredName.equals(node.getLocalName());
  }

  /**
   * Matches the given node's name and local name against the given desired names.
   */
  private static boolean nodeNameMatch(Node node, Collection<?> desiredNames) {
    return desiredNames.contains(node.getNodeName()) || desiredNames.contains(node.getLocalName());
  }

}
