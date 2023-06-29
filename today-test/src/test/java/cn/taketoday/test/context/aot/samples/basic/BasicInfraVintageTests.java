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

package cn.taketoday.test.context.aot.samples.basic;

import org.junit.runner.RunWith;

import cn.taketoday.aot.AotDetector;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.BootstrapWith;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.aot.AotTestAttributes;
import cn.taketoday.test.context.aot.samples.common.MessageService;
import cn.taketoday.test.context.junit4.InfraRunner;
import cn.taketoday.test.context.support.AnnotationConfigContextLoader;
import cn.taketoday.test.context.support.DefaultTestContextBootstrapper;
import cn.taketoday.test.context.support.GenericXmlContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@BootstrapWith(BasicInfraVintageTests.CustomXmlBootstrapper.class)
@RunWith(InfraRunner.class)
// Override the default loader configured by the CustomXmlBootstrapper
@ContextConfiguration(classes = BasicTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
@TestPropertySource
public class BasicInfraVintageTests {

  @Autowired
  ApplicationContext context;

  @Autowired
  MessageService messageService;

  @Value("${test.engine}")
  String testEngine;

  @org.junit.Test
  public void test() {
    assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
    assertThat(testEngine).isEqualTo("vintage");
    assertThat(context.getEnvironment().getProperty("test.engine"))
            .as("@TestPropertySource").isEqualTo("vintage");
  }

  public static class CustomXmlBootstrapper extends DefaultTestContextBootstrapper {

    @Override
    protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
      return GenericXmlContextLoader.class;
    }

    @Override
    protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
      String stringKey = "@InfraConfiguration-" + mergedConfig.getTestClass().getName();
      String booleanKey1 = "@InfraConfiguration-" + mergedConfig.getTestClass().getName() + "-active1";
      String booleanKey2 = "@InfraConfiguration-" + mergedConfig.getTestClass().getName() + "-active2";
      AotTestAttributes aotAttributes = AotTestAttributes.getInstance();
      if (AotDetector.useGeneratedArtifacts()) {
        assertThat(aotAttributes.getString(stringKey))
                .as("AOT String attribute must already be present during AOT run-time execution")
                .isEqualTo("org.example.Main");
        assertThat(aotAttributes.getBoolean(booleanKey1))
                .as("AOT boolean attribute 1 must already be present during AOT run-time execution")
                .isTrue();
        assertThat(aotAttributes.getBoolean(booleanKey2))
                .as("AOT boolean attribute 2 must already be present during AOT run-time execution")
                .isTrue();
      }
      else {
        // Set AOT attributes during AOT build-time processing
        aotAttributes.setAttribute(stringKey, "org.example.Main");
        aotAttributes.setAttribute(booleanKey1, "TrUe");
        aotAttributes.setAttribute(booleanKey2, true);
      }
      return mergedConfig;
    }

  }

}
