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

package cn.taketoday.beans.factory.parsing;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static cn.taketoday.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @since 2.0
 */
public class CustomProblemReporterTests {

  private CollatingProblemReporter problemReporter;

  private StandardBeanFactory beanFactory;

  private XmlBeanDefinitionReader reader;

  @BeforeEach
  public void setup() {
    this.problemReporter = new CollatingProblemReporter();
    this.beanFactory = new StandardBeanFactory();
    this.reader = new XmlBeanDefinitionReader(this.beanFactory);
    this.reader.setProblemReporter(this.problemReporter);
  }

  @Test
  public void testErrorsAreCollated() {
    this.reader.loadBeanDefinitions(qualifiedResource(CustomProblemReporterTests.class, "context.xml"));
    assertThat(this.problemReporter.getErrors().length).as("Incorrect number of errors collated").isEqualTo(4);

    TestBean bean = (TestBean) this.beanFactory.getBean("validBean");
    assertThat(bean).isNotNull();
  }

  private static class CollatingProblemReporter implements ProblemReporter {

    private final List<Problem> errors = new ArrayList<>();

    private final List<Problem> warnings = new ArrayList<>();

    @Override
    public void fatal(Problem problem) {
      throw new BeanDefinitionParsingException(problem);
    }

    @Override
    public void error(Problem problem) {
      this.errors.add(problem);
    }

    public Problem[] getErrors() {
      return this.errors.toArray(new Problem[this.errors.size()]);
    }

    @Override
    public void warning(Problem problem) {
      this.warnings.add(problem);
    }
  }

}
