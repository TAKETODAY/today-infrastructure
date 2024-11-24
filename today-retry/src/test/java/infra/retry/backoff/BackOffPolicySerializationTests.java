/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.retry.backoff;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import infra.beans.BeanUtils;
import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.ClassPathScanningCandidateComponentProvider;
import infra.core.type.filter.AssignableTypeFilter;
import infra.core.type.filter.RegexPatternTypeFilter;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.retry.context.RetryContextSupport;
import infra.util.ClassUtils;
import infra.util.SerializationUtils;

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
    Set<AnnotatedBeanDefinition> candidates = scanner.findCandidateComponents("infra.retry");
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
