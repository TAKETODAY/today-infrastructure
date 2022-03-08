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

package cn.taketoday.scripting.config;

import org.w3c.dom.Element;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.TypedStringValue;
import cn.taketoday.beans.factory.xml.BeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.util.StringUtils;

/**
 * A {@link BeanDefinitionParser} for use when loading scripting XML.
 *
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 21:48
 */
class ScriptingDefaultsParser implements BeanDefinitionParser {

  private static final String REFRESH_CHECK_DELAY_ATTRIBUTE = "refresh-check-delay";

  private static final String PROXY_TARGET_CLASS_ATTRIBUTE = "proxy-target-class";

  @Override
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    BeanDefinition bd =
            LangNamespaceUtils.registerScriptFactoryPostProcessorIfNecessary(parserContext.getRegistry());
    String refreshCheckDelay = element.getAttribute(REFRESH_CHECK_DELAY_ATTRIBUTE);
    if (StringUtils.hasText(refreshCheckDelay)) {
      bd.getPropertyValues().add("defaultRefreshCheckDelay", Long.valueOf(refreshCheckDelay));
    }
    String proxyTargetClass = element.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE);
    if (StringUtils.hasText(proxyTargetClass)) {
      bd.getPropertyValues().add("defaultProxyTargetClass", new TypedStringValue(proxyTargetClass, Boolean.class));
    }
    return null;
  }

}
