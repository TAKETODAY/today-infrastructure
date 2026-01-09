/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
