/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.retry.backoff;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.annotation.ClassPathScanningCandidateComponentProvider;
import cn.taketoday.core.type.filter.AssignableTypeFilter;
import cn.taketoday.core.type.filter.RegexPatternTypeFilter;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.retry.context.RetryContextSupport;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.SerializationUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class BackOffPolicySerializationTests {

  private static Logger logger = LoggerFactory.getLogger(BackOffPolicySerializationTests.class);

  @SuppressWarnings("deprecation")
  public static Stream<Object[]> policies() {
    List<Object[]> result = new ArrayList<>();
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
    scanner.addIncludeFilter(new AssignableTypeFilter(BackOffPolicy.class));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Test.*")));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Mock.*")));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Configuration.*")));
    Set<AnnotatedBeanDefinition> candidates = scanner.findCandidateComponents("cn.taketoday.retry");
    for (BeanDefinition beanDefinition : candidates) {
      try {
        result.add(new Object[] {
                BeanUtils.newInstance(ClassUtils.resolveClassName(beanDefinition.getBeanClassName(), null)) });
      }
      catch (Exception e) {
        logger.warn("Cannot create instance of " + beanDefinition.getBeanClassName());
      }
    }
    return result.stream();
  }

  @ParameterizedTest
  @MethodSource("policies")
  @SuppressWarnings("deprecation")
  public void testSerializationCycleForContext(BackOffPolicy policy) {
    BackOffContext context = policy.start(new RetryContextSupport(null));
    if (context != null) {
      assertTrue(SerializationUtils.deserialize(SerializationUtils.serialize(context)) instanceof BackOffContext);
    }
  }

}
