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

package cn.taketoday.beans.factory.xml;

import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.FieldRetrievingFactoryBean;
import cn.taketoday.beans.factory.support.ListFactoryBean;
import cn.taketoday.beans.factory.support.MapFactoryBean;
import cn.taketoday.beans.factory.support.PropertiesFactoryBean;
import cn.taketoday.beans.factory.support.PropertyPathFactoryBean;
import cn.taketoday.beans.factory.support.SetFactoryBean;
import cn.taketoday.util.StringUtils;

/**
 * {@link NamespaceHandler} for the {@code util} namespace.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class UtilNamespaceHandler extends NamespaceHandlerSupport {

  private static final String SCOPE_ATTRIBUTE = "scope";

  @Override
  public void init() {
    registerBeanDefinitionParser("constant", new ConstantBeanDefinitionParser());
    registerBeanDefinitionParser("property-path", new PropertyPathBeanDefinitionParser());
    registerBeanDefinitionParser("list", new ListBeanDefinitionParser());
    registerBeanDefinitionParser("set", new SetBeanDefinitionParser());
    registerBeanDefinitionParser("map", new MapBeanDefinitionParser());
    registerBeanDefinitionParser("properties", new PropertiesBeanDefinitionParser());
  }

  private static class ConstantBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(Element element) {
      return FieldRetrievingFactoryBean.class;
    }

    @Override
    protected String resolveId(Element element, BeanDefinition definition, ParserContext parserContext) {
      String id = super.resolveId(element, definition, parserContext);
      if (!StringUtils.hasText(id)) {
        id = element.getAttribute("static-field");
      }
      return id;
    }
  }

  private static class PropertyPathBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(Element element) {
      return PropertyPathFactoryBean.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
      String path = element.getAttribute("path");
      if (!StringUtils.hasText(path)) {
        parserContext.getReaderContext().error("Attribute 'path' must not be empty", element);
        return;
      }
      int dotIndex = path.indexOf('.');
      if (dotIndex == -1) {
        parserContext.getReaderContext().error(
                "Attribute 'path' must follow pattern 'beanName.propertyName'", element);
        return;
      }
      String beanName = path.substring(0, dotIndex);
      String propertyPath = path.substring(dotIndex + 1);
      builder.addPropertyValue("targetBeanName", beanName);
      builder.addPropertyValue("propertyPath", propertyPath);
    }

    @Override
    protected String resolveId(Element element, BeanDefinition definition, ParserContext parserContext) {
      String id = super.resolveId(element, definition, parserContext);
      if (!StringUtils.hasText(id)) {
        id = element.getAttribute("path");
      }
      return id;
    }
  }

  private static class ListBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(Element element) {
      return ListFactoryBean.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
      List<Object> parsedList = parserContext.getDelegate().parseListElement(element, builder.getRawBeanDefinition());
      builder.addPropertyValue("sourceList", parsedList);

      String listClass = element.getAttribute("list-class");
      if (StringUtils.hasText(listClass)) {
        builder.addPropertyValue("targetListClass", listClass);
      }

      String scope = element.getAttribute(SCOPE_ATTRIBUTE);
      if (StringUtils.isNotEmpty(scope)) {
        builder.setScope(scope);
      }
    }
  }

  private static class SetBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(Element element) {
      return SetFactoryBean.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
      Set<Object> parsedSet = parserContext.getDelegate().parseSetElement(element, builder.getRawBeanDefinition());
      builder.addPropertyValue("sourceSet", parsedSet);

      String setClass = element.getAttribute("set-class");
      if (StringUtils.hasText(setClass)) {
        builder.addPropertyValue("targetSetClass", setClass);
      }

      String scope = element.getAttribute(SCOPE_ATTRIBUTE);
      if (StringUtils.isNotEmpty(scope)) {
        builder.setScope(scope);
      }
    }
  }

  private static class MapBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(Element element) {
      return MapFactoryBean.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
      Map<Object, Object> parsedMap = parserContext.getDelegate().parseMapElement(element, builder.getRawBeanDefinition());
      builder.addPropertyValue("sourceMap", parsedMap);

      String mapClass = element.getAttribute("map-class");
      if (StringUtils.hasText(mapClass)) {
        builder.addPropertyValue("targetMapClass", mapClass);
      }

      String scope = element.getAttribute(SCOPE_ATTRIBUTE);
      if (StringUtils.isNotEmpty(scope)) {
        builder.setScope(scope);
      }
    }
  }

  private static class PropertiesBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(Element element) {
      return PropertiesFactoryBean.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
      Properties parsedProps = parserContext.getDelegate().parsePropsElement(element);
      builder.addPropertyValue("properties", parsedProps);

      String location = element.getAttribute("location");
      if (StringUtils.isNotEmpty(location)) {
        location = parserContext.getReaderContext().getEnvironment().resolvePlaceholders(location);
        String[] locations = StringUtils.commaDelimitedListToStringArray(location);
        builder.addPropertyValue("locations", locations);
      }

      builder.addPropertyValue("ignoreResourceNotFound",
              Boolean.valueOf(element.getAttribute("ignore-resource-not-found")));

      builder.addPropertyValue("localOverride",
              Boolean.valueOf(element.getAttribute("local-override")));

      String scope = element.getAttribute(SCOPE_ATTRIBUTE);
      if (StringUtils.isNotEmpty(scope)) {
        builder.setScope(scope);
      }
    }
  }

}
