/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.beans.factory.xml;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import infra.beans.PropertyValues;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.RuntimeBeanReference;
import infra.core.Conventions;
import infra.lang.Nullable;

/**
 * Simple {@code NamespaceHandler} implementation that maps custom attributes
 * directly through to bean properties. An important point to note is that this
 * {@code NamespaceHandler} does not have a corresponding schema since there
 * is no way to know in advance all possible attribute names.
 *
 * <p>An example of the usage of this {@code NamespaceHandler} is shown below:
 *
 * <pre class="code">
 * &lt;bean id=&quot;rob&quot; class=&quot;..TestBean&quot; p:name=&quot;Rob Harrop&quot; p:spouse-ref=&quot;sally&quot;/&gt;</pre>
 *
 * Here the '{@code p:name}' corresponds directly to the '{@code name}'
 * property on class '{@code TestBean}'. The '{@code p:spouse-ref}'
 * attributes corresponds to the '{@code spouse}' property and, rather
 * than being the concrete value, it contains the name of the bean that will
 * be injected into that property.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SimplePropertyNamespaceHandler implements NamespaceHandler {

  private static final String REF_SUFFIX = "-ref";

  @Override
  @Nullable
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    parserContext.getReaderContext().error(
            "Class [" + getClass().getName() + "] does not support custom elements.", element);
    return null;
  }

  @Override
  public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
    if (node instanceof Attr attr) {
      String propertyName = parserContext.getDelegate().getLocalName(attr);
      String propertyValue = attr.getValue();
      PropertyValues pvs = definition.getBeanDefinition().getPropertyValues();
      if (pvs.contains(propertyName)) {
        parserContext.getReaderContext().error("Property '" + propertyName + "' is already defined using " +
                "both <property> and inline syntax. Only one approach may be used per property.", attr);
      }
      if (propertyName.endsWith(REF_SUFFIX)) {
        propertyName = propertyName.substring(0, propertyName.length() - REF_SUFFIX.length());
        pvs.add(Conventions.attributeNameToPropertyName(propertyName), new RuntimeBeanReference(propertyValue));
      }
      else {
        pvs.add(Conventions.attributeNameToPropertyName(propertyName), propertyValue);
      }
    }
    return definition;
  }

}
