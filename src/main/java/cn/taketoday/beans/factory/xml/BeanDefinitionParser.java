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

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.lang.Nullable;

/**
 * Interface used by the {@link DefaultBeanDefinitionDocumentReader} to handle custom,
 * top-level (directly under {@code <beans/>}) tags.
 *
 * <p>Implementations are free to turn the metadata in the custom tag into as many
 * {@link BeanDefinition BeanDefinitions} as required.
 *
 * <p>The parser locates a {@link BeanDefinitionParser} from the associated
 * {@link NamespaceHandler} for the namespace in which the custom tag resides.
 *
 * @author Rob Harrop
 * @see NamespaceHandler
 * @see BeanDefinitionParser
 * @since 4.0
 */
public interface BeanDefinitionParser {

  /**
   * Parse the specified {@link Element} and register the resulting
   * {@link BeanDefinition BeanDefinition(s)} with the
   * {@link cn.taketoday.beans.factory.xml.ParserContext#getRegistry() BeanDefinitionRegistry}
   * embedded in the supplied {@link ParserContext}.
   * <p>Implementations must return the primary {@link BeanDefinition} that results
   * from the parse if they will ever be used in a nested fashion (for example as
   * an inner tag in a {@code <property/>} tag). Implementations may return
   * {@code null} if they will <strong>not</strong> be used in a nested fashion.
   *
   * @param element the element that is to be parsed into one or more {@link BeanDefinition BeanDefinitions}
   * @param parserContext the object encapsulating the current state of the parsing process;
   * provides access to a {@link cn.taketoday.beans.factory.support.BeanDefinitionRegistry}
   * @return the primary {@link BeanDefinition}
   */
  @Nullable
  BeanDefinition parse(Element element, ParserContext parserContext);

}
