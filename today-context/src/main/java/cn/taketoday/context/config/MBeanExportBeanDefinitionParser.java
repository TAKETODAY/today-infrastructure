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

package cn.taketoday.context.config;

import org.w3c.dom.Element;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.xml.AbstractBeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.jmx.export.annotation.AnnotationMBeanExporter;
import cn.taketoday.jmx.support.RegistrationPolicy;
import cn.taketoday.util.StringUtils;

/**
 * Parser for the &lt;context:mbean-export/&gt; element.
 *
 * <p>Registers an instance of
 * {@link AnnotationMBeanExporter}
 * within the context.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @see AnnotationMBeanExporter
 * @since 4.0
 */
class MBeanExportBeanDefinitionParser extends AbstractBeanDefinitionParser {

  private static final String MBEAN_EXPORTER_BEAN_NAME = "mbeanExporter";

  private static final String DEFAULT_DOMAIN_ATTRIBUTE = "default-domain";

  private static final String SERVER_ATTRIBUTE = "server";

  private static final String REGISTRATION_ATTRIBUTE = "registration";

  private static final String REGISTRATION_IGNORE_EXISTING = "ignoreExisting";

  private static final String REGISTRATION_REPLACE_EXISTING = "replaceExisting";

  @Override
  protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
    return MBEAN_EXPORTER_BEAN_NAME;
  }

  @Override
  protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AnnotationMBeanExporter.class);

    // Mark as infrastructure bean and attach source location.
    builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));

    String defaultDomain = element.getAttribute(DEFAULT_DOMAIN_ATTRIBUTE);
    if (StringUtils.hasText(defaultDomain)) {
      builder.addPropertyValue("defaultDomain", defaultDomain);
    }

    String serverBeanName = element.getAttribute(SERVER_ATTRIBUTE);
    if (StringUtils.hasText(serverBeanName)) {
      builder.addPropertyReference("server", serverBeanName);
    }

    String registration = element.getAttribute(REGISTRATION_ATTRIBUTE);
    RegistrationPolicy registrationPolicy = RegistrationPolicy.FAIL_ON_EXISTING;
    if (REGISTRATION_IGNORE_EXISTING.equals(registration)) {
      registrationPolicy = RegistrationPolicy.IGNORE_EXISTING;
    }
    else if (REGISTRATION_REPLACE_EXISTING.equals(registration)) {
      registrationPolicy = RegistrationPolicy.REPLACE_EXISTING;
    }
    builder.addPropertyValue("registrationPolicy", registrationPolicy);

    return builder.getBeanDefinition();
  }

}
