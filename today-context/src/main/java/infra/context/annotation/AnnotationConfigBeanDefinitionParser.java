/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.annotation;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.parsing.BeanComponentDefinition;
import infra.beans.factory.parsing.CompositeComponentDefinition;
import infra.beans.factory.xml.BeanDefinitionParser;
import infra.beans.factory.xml.ParserContext;

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
@SuppressWarnings("NullAway")
public class AnnotationConfigBeanDefinitionParser implements BeanDefinitionParser {

  @Override
  @Nullable
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    Object source = parserContext.extractSource(element);

    // Obtain bean definitions for all relevant BeanPostProcessors.
    AnnotationConfigUtils.registerAnnotationConfigProcessors(parserContext.getRegistry(), holder -> {
      holder.setSource(source);
      // Nest the concrete beans in the surrounding component.
      parserContext.registerComponent(new BeanComponentDefinition(holder));
    });

    // Register component for the surrounding <context:annotation-config> element.
    CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
    parserContext.pushContainingComponent(compDefinition);

    // Finally register the composite component.
    parserContext.popAndRegisterContainingComponent();

    return null;
  }

}
