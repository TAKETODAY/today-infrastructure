/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.aot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.beans.factory.aot.AotServices;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationCode;
import cn.taketoday.beans.factory.support.StandardBeanFactory;

/**
 * A collection of {@link BeanFactoryInitializationAotContribution AOT
 * contributions} obtained from {@link BeanFactoryInitializationAotProcessor AOT
 * processors}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BeanFactoryInitializationAotContributions {

  private final List<BeanFactoryInitializationAotContribution> contributions;

  BeanFactoryInitializationAotContributions(StandardBeanFactory beanFactory) {
    this(beanFactory, AotServices.factoriesAndBeans(beanFactory));
  }

  BeanFactoryInitializationAotContributions(StandardBeanFactory beanFactory,
          AotServices.Loader loader) {
    this.contributions = getContributions(beanFactory, getProcessors(loader));
  }

  private static List<BeanFactoryInitializationAotProcessor> getProcessors(
          AotServices.Loader loader) {
    var processors = new ArrayList<>(loader.load(BeanFactoryInitializationAotProcessor.class).asList());
    processors.add(new RuntimeHintsBeanFactoryInitializationAotProcessor());
    return Collections.unmodifiableList(processors);
  }

  private List<BeanFactoryInitializationAotContribution> getContributions(
          StandardBeanFactory beanFactory, List<BeanFactoryInitializationAotProcessor> processors) {
    var contributions = new ArrayList<BeanFactoryInitializationAotContribution>();
    for (BeanFactoryInitializationAotProcessor processor : processors) {
      var contribution = processor.processAheadOfTime(beanFactory);
      if (contribution != null) {
        contributions.add(contribution);
      }
    }
    return Collections.unmodifiableList(contributions);
  }

  void applyTo(GenerationContext generationContext,
          BeanFactoryInitializationCode beanFactoryInitializationCode) {
    for (BeanFactoryInitializationAotContribution contribution : this.contributions) {
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
    }
  }

}
