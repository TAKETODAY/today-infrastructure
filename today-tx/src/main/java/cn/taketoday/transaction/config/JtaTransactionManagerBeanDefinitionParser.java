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

import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.transaction.jta.JtaTransactionManager;

/**
 * Parser for the &lt;tx:jta-transaction-manager/&gt; XML configuration element.
 *
 * @author Juergen Hoeller
 * @author Christian Dupuis
 * @since 4.0
 */
public class JtaTransactionManagerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

  @Override
  protected Class<?> getBeanClass(Element element) {
    return JtaTransactionManager.class;
  }

  @Override
  protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
    return TxNamespaceHandler.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME;
  }

}
