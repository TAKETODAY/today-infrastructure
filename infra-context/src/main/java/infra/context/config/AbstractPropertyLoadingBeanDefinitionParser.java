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

package infra.context.config;

import org.w3c.dom.Element;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import infra.beans.factory.xml.ParserContext;
import infra.util.StringUtils;

/**
 * Abstract parser for &lt;context:property-.../&gt; elements.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class AbstractPropertyLoadingBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

  @Override
  protected boolean shouldGenerateId() {
    return true;
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    String location = element.getAttribute("location");
    if (StringUtils.isNotEmpty(location)) {
      location = parserContext.getReaderContext().getEnvironment().resolvePlaceholders(location);
      String[] locations = StringUtils.commaDelimitedListToStringArray(location);
      builder.addPropertyValue("locations", locations);
    }

    String propertiesRef = element.getAttribute("properties-ref");
    if (StringUtils.isNotEmpty(propertiesRef)) {
      builder.addPropertyReference("properties", propertiesRef);
    }

    String fileEncoding = element.getAttribute("file-encoding");
    if (StringUtils.isNotEmpty(fileEncoding)) {
      builder.addPropertyValue("fileEncoding", fileEncoding);
    }

    String order = element.getAttribute("order");
    if (StringUtils.isNotEmpty(order)) {
      builder.addPropertyValue("order", Integer.valueOf(order));
    }

    builder.addPropertyValue("ignoreResourceNotFound",
            Boolean.valueOf(element.getAttribute("ignore-resource-not-found")));

    builder.addPropertyValue("localOverride",
            Boolean.valueOf(element.getAttribute("local-override")));

    builder.setEnableDependencyInjection(false);
    builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
  }

}
