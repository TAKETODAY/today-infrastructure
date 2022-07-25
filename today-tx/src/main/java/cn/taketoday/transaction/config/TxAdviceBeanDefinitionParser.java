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

package cn.taketoday.transaction.config;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.config.TypedStringValue;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.ManagedMap;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.transaction.interceptor.NameMatchTransactionAttributeSource;
import cn.taketoday.transaction.interceptor.NoRollbackRuleAttribute;
import cn.taketoday.transaction.interceptor.RollbackRuleAttribute;
import cn.taketoday.transaction.interceptor.RuleBasedTransactionAttribute;
import cn.taketoday.transaction.interceptor.TransactionInterceptor;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.xml.DomUtils;

/**
 * {@link cn.taketoday.beans.factory.xml.BeanDefinitionParser
 * BeanDefinitionParser} for the {@code <tx:advice/>} tag.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @author Chris Beams
 * @since 4.0
 */
class TxAdviceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

  private static final String METHOD_ELEMENT = "method";

  private static final String METHOD_NAME_ATTRIBUTE = "name";

  private static final String ATTRIBUTES_ELEMENT = "attributes";

  private static final String TIMEOUT_ATTRIBUTE = "timeout";

  private static final String READ_ONLY_ATTRIBUTE = "read-only";

  private static final String PROPAGATION_ATTRIBUTE = "propagation";

  private static final String ISOLATION_ATTRIBUTE = "isolation";

  private static final String ROLLBACK_FOR_ATTRIBUTE = "rollback-for";

  private static final String NO_ROLLBACK_FOR_ATTRIBUTE = "no-rollback-for";

  @Override
  protected Class<?> getBeanClass(Element element) {
    return TransactionInterceptor.class;
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    builder.addPropertyReference("transactionManager", TxNamespaceHandler.getTransactionManagerName(element));

    List<Element> txAttributes = DomUtils.getChildElementsByTagName(element, ATTRIBUTES_ELEMENT);
    if (txAttributes.size() > 1) {
      parserContext.getReaderContext().error(
              "Element <attributes> is allowed at most once inside element <advice>", element);
    }
    else if (txAttributes.size() == 1) {
      // Using attributes source.
      Element attributeSourceElement = txAttributes.get(0);
      RootBeanDefinition attributeSourceDefinition = parseAttributeSource(attributeSourceElement, parserContext);
      builder.addPropertyValue("transactionAttributeSource", attributeSourceDefinition);
    }
    else {
      // Assume annotations source.
      builder.addPropertyValue("transactionAttributeSource",
              new RootBeanDefinition("cn.taketoday.transaction.annotation.AnnotationTransactionAttributeSource"));
    }
  }

  private RootBeanDefinition parseAttributeSource(Element attrEle, ParserContext parserContext) {
    List<Element> methods = DomUtils.getChildElementsByTagName(attrEle, METHOD_ELEMENT);
    ManagedMap<TypedStringValue, RuleBasedTransactionAttribute> transactionAttributeMap =
            new ManagedMap<>(methods.size());
    transactionAttributeMap.setSource(parserContext.extractSource(attrEle));

    for (Element methodEle : methods) {
      String name = methodEle.getAttribute(METHOD_NAME_ATTRIBUTE);
      TypedStringValue nameHolder = new TypedStringValue(name);
      nameHolder.setSource(parserContext.extractSource(methodEle));

      RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
      String propagation = methodEle.getAttribute(PROPAGATION_ATTRIBUTE);
      String isolation = methodEle.getAttribute(ISOLATION_ATTRIBUTE);
      String timeout = methodEle.getAttribute(TIMEOUT_ATTRIBUTE);
      String readOnly = methodEle.getAttribute(READ_ONLY_ATTRIBUTE);
      if (StringUtils.hasText(propagation)) {
        attribute.setPropagationBehaviorName(RuleBasedTransactionAttribute.PREFIX_PROPAGATION + propagation);
      }
      if (StringUtils.hasText(isolation)) {
        attribute.setIsolationLevelName(RuleBasedTransactionAttribute.PREFIX_ISOLATION + isolation);
      }
      if (StringUtils.hasText(timeout)) {
        attribute.setTimeoutString(timeout);
      }
      if (StringUtils.hasText(readOnly)) {
        attribute.setReadOnly(Boolean.parseBoolean(methodEle.getAttribute(READ_ONLY_ATTRIBUTE)));
      }

      List<RollbackRuleAttribute> rollbackRules = new ArrayList<>(1);
      if (methodEle.hasAttribute(ROLLBACK_FOR_ATTRIBUTE)) {
        String rollbackForValue = methodEle.getAttribute(ROLLBACK_FOR_ATTRIBUTE);
        addRollbackRuleAttributesTo(rollbackRules, rollbackForValue);
      }
      if (methodEle.hasAttribute(NO_ROLLBACK_FOR_ATTRIBUTE)) {
        String noRollbackForValue = methodEle.getAttribute(NO_ROLLBACK_FOR_ATTRIBUTE);
        addNoRollbackRuleAttributesTo(rollbackRules, noRollbackForValue);
      }
      attribute.setRollbackRules(rollbackRules);

      transactionAttributeMap.put(nameHolder, attribute);
    }

    RootBeanDefinition attributeSourceDefinition = new RootBeanDefinition(NameMatchTransactionAttributeSource.class);
    attributeSourceDefinition.setSource(parserContext.extractSource(attrEle));
    attributeSourceDefinition.getPropertyValues().add("nameMap", transactionAttributeMap);
    return attributeSourceDefinition;
  }

  private void addRollbackRuleAttributesTo(List<RollbackRuleAttribute> rollbackRules, String rollbackForValue) {
    String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(rollbackForValue);
    for (String typeName : exceptionTypeNames) {
      rollbackRules.add(new RollbackRuleAttribute(typeName.strip()));
    }
  }

  private void addNoRollbackRuleAttributesTo(List<RollbackRuleAttribute> rollbackRules, String noRollbackForValue) {
    String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(noRollbackForValue);
    for (String typeName : exceptionTypeNames) {
      rollbackRules.add(new NoRollbackRuleAttribute(typeName.strip()));
    }
  }

}
