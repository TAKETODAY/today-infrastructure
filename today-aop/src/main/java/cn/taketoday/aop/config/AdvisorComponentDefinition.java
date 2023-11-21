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

package cn.taketoday.aop.config;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanReference;
import cn.taketoday.beans.factory.parsing.AbstractComponentDefinition;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link cn.taketoday.beans.factory.parsing.ComponentDefinition}
 * that bridges the gap between the advisor bean definition configured
 * by the {@code <aop:advisor>} tag and the component definition
 * infrastructure.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class AdvisorComponentDefinition extends AbstractComponentDefinition {

  private final String advisorBeanName;

  private final BeanDefinition advisorDefinition;

  private final String description;

  private final BeanReference[] beanReferences;

  private final BeanDefinition[] beanDefinitions;

  public AdvisorComponentDefinition(String advisorBeanName, BeanDefinition advisorDefinition) {
    this(advisorBeanName, advisorDefinition, null);
  }

  public AdvisorComponentDefinition(
          String advisorBeanName, BeanDefinition advisorDefinition, @Nullable BeanDefinition pointcutDefinition) {

    Assert.notNull(advisorBeanName, "'advisorBeanName' is required");
    Assert.notNull(advisorDefinition, "'advisorDefinition' is required");
    this.advisorBeanName = advisorBeanName;
    this.advisorDefinition = advisorDefinition;

    PropertyValues pvs = advisorDefinition.getPropertyValues();
    BeanReference adviceReference = (BeanReference) pvs.getPropertyValue("adviceBeanName");
    Assert.state(adviceReference != null, "Missing 'adviceBeanName' property");

    if (pointcutDefinition != null) {
      this.beanReferences = new BeanReference[] { adviceReference };
      this.beanDefinitions = new BeanDefinition[] { advisorDefinition, pointcutDefinition };
      this.description = buildDescription(adviceReference, pointcutDefinition);
    }
    else {
      BeanReference pointcutReference = (BeanReference) pvs.getPropertyValue("pointcut");
      Assert.state(pointcutReference != null, "Missing 'pointcut' property");
      this.beanReferences = new BeanReference[] { adviceReference, pointcutReference };
      this.beanDefinitions = new BeanDefinition[] { advisorDefinition };
      this.description = buildDescription(adviceReference, pointcutReference);
    }
  }

  private String buildDescription(BeanReference adviceReference, BeanDefinition pointcutDefinition) {
    return "Advisor <advice(ref)='" +
            adviceReference.getBeanName() + "', pointcut(expression)=[" +
            pointcutDefinition.getPropertyValues().getPropertyValue("expression") + "]>";
  }

  private String buildDescription(BeanReference adviceReference, BeanReference pointcutReference) {
    return "Advisor <advice(ref)='" +
            adviceReference.getBeanName() + "', pointcut(ref)='" +
            pointcutReference.getBeanName() + "'>";
  }

  @Override
  public String getName() {
    return this.advisorBeanName;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public BeanDefinition[] getBeanDefinitions() {
    return this.beanDefinitions;
  }

  @Override
  public BeanReference[] getBeanReferences() {
    return this.beanReferences;
  }

  @Override
  @Nullable
  public Object getSource() {
    return this.advisorDefinition.getSource();
  }

}
