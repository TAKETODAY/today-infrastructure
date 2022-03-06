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

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collection;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.ConstructorArgumentValues;
import cn.taketoday.beans.factory.support.ConstructorArgumentValues.ValueHolder;
import cn.taketoday.beans.factory.support.RuntimeBeanReference;
import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Simple {@code NamespaceHandler} implementation that maps custom
 * attributes directly through to bean properties. An important point to note is
 * that this {@code NamespaceHandler} does not have a corresponding schema
 * since there is no way to know in advance all possible attribute names.
 *
 * <p>An example of the usage of this {@code NamespaceHandler} is shown below:
 *
 * <pre class="code">
 * &lt;bean id=&quot;author&quot; class=&quot;..TestBean&quot; c:name=&quot;Enescu&quot; c:work-ref=&quot;compositions&quot;/&gt;
 * </pre>
 *
 * Here the '{@code c:name}' corresponds directly to the '{@code name}
 * ' argument declared on the constructor of class '{@code TestBean}'. The
 * '{@code c:work-ref}' attributes corresponds to the '{@code work}'
 * argument and, rather than being the concrete value, it contains the name of
 * the bean that will be considered as a parameter.
 *
 * <b>Note</b>: This implementation supports only named parameters - there is no
 * support for indexes or types. Further more, the names are used as hints by
 * the container which, by default, does type introspection.
 *
 * @author Costin Leau
 * @see SimplePropertyNamespaceHandler
 * @since 4.0
 */
public class SimpleConstructorNamespaceHandler implements NamespaceHandler {

  private static final String REF_SUFFIX = "-ref";

  private static final String DELIMITER_PREFIX = "_";

  @Override
  public void init() {
  }

  @Override
  @Nullable
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    parserContext.getReaderContext().error(
            "Class [" + getClass().getName() + "] does not support custom elements.", element);
    return null;
  }

  @Override
  public BeanDefinition decorate(Node node, BeanDefinition definition, ParserContext parserContext) {
    if (node instanceof Attr attr) {
      String argName = parserContext.getDelegate().getLocalName(attr).strip();
      String argValue = attr.getValue().strip();

      ConstructorArgumentValues cvs = definition.getConstructorArgumentValues();
      boolean ref = false;

      // handle -ref arguments
      if (argName.endsWith(REF_SUFFIX)) {
        ref = true;
        argName = argName.substring(0, argName.length() - REF_SUFFIX.length());
      }

      ValueHolder valueHolder = new ValueHolder(ref ? new RuntimeBeanReference(argValue) : argValue);
      valueHolder.setSource(parserContext.getReaderContext().extractSource(attr));

      // handle "escaped"/"_" arguments
      if (argName.startsWith(DELIMITER_PREFIX)) {
        String arg = argName.substring(1).trim();

        // fast default check
        if (!StringUtils.hasText(arg)) {
          cvs.addGenericArgumentValue(valueHolder);
        }
        // assume an index otherwise
        else {
          int index = -1;
          try {
            index = Integer.parseInt(arg);
          }
          catch (NumberFormatException ex) {
            parserContext.getReaderContext().error(
                    "Constructor argument '" + argName + "' specifies an invalid integer", attr);
          }
          if (index < 0) {
            parserContext.getReaderContext().error(
                    "Constructor argument '" + argName + "' specifies a negative index", attr);
          }

          if (cvs.hasIndexedArgumentValue(index)) {
            parserContext.getReaderContext().error(
                    "Constructor argument '" + argName + "' with index " + index + " already defined using <constructor-arg>." +
                            " Only one approach may be used per argument.", attr);
          }

          cvs.addIndexedArgumentValue(index, valueHolder);
        }
      }
      // no escaping -> ctr name
      else {
        String name = Conventions.attributeNameToPropertyName(argName);
        if (containsArgWithName(name, cvs)) {
          parserContext.getReaderContext().error(
                  "Constructor argument '" + argName + "' already defined using <constructor-arg>." +
                          " Only one approach may be used per argument.", attr);
        }
        valueHolder.setName(Conventions.attributeNameToPropertyName(argName));
        cvs.addGenericArgumentValue(valueHolder);
      }
    }
    return definition;
  }

  private boolean containsArgWithName(String name, ConstructorArgumentValues cvs) {
    return (checkName(name, cvs.getGenericArgumentValues()) ||
            checkName(name, cvs.getIndexedArgumentValues().values()));
  }

  private boolean checkName(String name, Collection<ValueHolder> values) {
    for (ValueHolder holder : values) {
      if (name.equals(holder.getName())) {
        return true;
      }
    }
    return false;
  }

}
