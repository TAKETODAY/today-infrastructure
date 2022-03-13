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

import cn.taketoday.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code NamespaceHandler} allowing for the configuration of
 * declarative transaction management using either XML or using annotations.
 *
 * <p>This namespace handler is the central piece of functionality in the
 * Spring transaction management facilities and offers two approaches
 * to declaratively manage transactions.
 *
 * <p>One approach uses transaction semantics defined in XML using the
 * {@code <tx:advice>} elements, the other uses annotations
 * in combination with the {@code <tx:annotation-driven>} element.
 * Both approached are detailed to great extent in the Spring reference manual.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class TxNamespaceHandler extends NamespaceHandlerSupport {

  static final String TRANSACTION_MANAGER_ATTRIBUTE = "transaction-manager";

  static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";

  static String getTransactionManagerName(Element element) {
    return (element.hasAttribute(TRANSACTION_MANAGER_ATTRIBUTE) ?
            element.getAttribute(TRANSACTION_MANAGER_ATTRIBUTE) : DEFAULT_TRANSACTION_MANAGER_BEAN_NAME);
  }

  @Override
  public void init() {
    registerBeanDefinitionParser("advice", new TxAdviceBeanDefinitionParser());
    registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenBeanDefinitionParser());
    registerBeanDefinitionParser("jta-transaction-manager", new JtaTransactionManagerBeanDefinitionParser());
  }

}
