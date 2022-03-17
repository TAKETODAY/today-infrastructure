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

package cn.taketoday.cache.config;

import org.w3c.dom.Element;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.xml.NamespaceHandlerSupport;
import cn.taketoday.util.StringUtils;

/**
 * {@code NamespaceHandler} allowing for the configuration of declarative
 * cache management using either XML or using annotations.
 *
 * <p>This namespace handler is the central piece of functionality in the
 * Framework cache management facilities.
 *
 * @author Costin Leau
 * @since 4.0
 */
public class CacheNamespaceHandler extends NamespaceHandlerSupport {

  static final String CACHE_MANAGER_ATTRIBUTE = "cache-manager";

  static final String DEFAULT_CACHE_MANAGER_BEAN_NAME = "cacheManager";

  static String extractCacheManager(Element element) {
    return (element.hasAttribute(CacheNamespaceHandler.CACHE_MANAGER_ATTRIBUTE) ?
            element.getAttribute(CacheNamespaceHandler.CACHE_MANAGER_ATTRIBUTE) :
            CacheNamespaceHandler.DEFAULT_CACHE_MANAGER_BEAN_NAME);
  }

  static BeanDefinition parseKeyGenerator(Element element, BeanDefinition def) {
    String name = element.getAttribute("key-generator");
    if (StringUtils.hasText(name)) {
      def.getPropertyValues().add("keyGenerator", new RuntimeBeanReference(name.trim()));
    }
    return def;
  }

  @Override
  public void init() {
    registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenCacheBeanDefinitionParser());
    registerBeanDefinitionParser("advice", new CacheAdviceParser());
  }

}
