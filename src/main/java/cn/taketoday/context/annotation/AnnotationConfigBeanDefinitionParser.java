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

package cn.taketoday.context.annotation;

import org.w3c.dom.Element;

import java.util.Set;

import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.parsing.CompositeComponentDefinition;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.xml.BeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.lang.Nullable;

/**
 * Parser for the &lt;context:annotation-config/&gt; element.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Christian Dupuis
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotationConfigUtils
 * @since 4.0 2022/3/7 18:04
 */
public class AnnotationConfigBeanDefinitionParser implements BeanDefinitionParser {

  @Override
  @Nullable
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    Object source = parserContext.extractSource(element);

    // Obtain bean definitions for all relevant BeanPostProcessors.
    Set<BeanDefinition> processorDefinitions =
            AnnotationConfigUtils.registerAnnotationConfigProcessors(parserContext.getRegistry(), source);

    // Register component for the surrounding <context:annotation-config> element.
    CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
    parserContext.pushContainingComponent(compDefinition);

    // Nest the concrete beans in the surrounding component.
    for (BeanDefinition processorDefinition : processorDefinitions) {
      parserContext.registerComponent(new BeanComponentDefinition(processorDefinition));
    }

    // Finally register the composite component.
    parserContext.popAndRegisterContainingComponent();

    return null;
  }

}
