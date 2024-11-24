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

package infra.retry.policy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import infra.beans.BeanUtils;
import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.classify.SubclassClassifier;
import infra.context.annotation.ClassPathScanningCandidateComponentProvider;
import infra.core.type.filter.AssignableTypeFilter;
import infra.core.type.filter.RegexPatternTypeFilter;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.retry.RetryContext;
import infra.retry.RetryPolicy;
import infra.util.ClassUtils;
import infra.util.SerializationUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class RetryContextSerializationTests {

  private static Logger logger = LoggerFactory.getLogger(RetryContextSerializationTests.class);

  @SuppressWarnings("deprecation")
  public static List<Object[]> policies() {
    List<Object[]> result = new ArrayList<>();
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
    scanner.addIncludeFilter(new AssignableTypeFilter(RetryPolicy.class));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Test.*")));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Mock.*")));
    Set<AnnotatedBeanDefinition> candidates = scanner.findCandidateComponents("infra.retry.policy");
    for (BeanDefinition beanDefinition : candidates) {
      try {
        result.add(new Object[] {
                BeanUtils.newInstance(ClassUtils.resolveClassName(beanDefinition.getBeanClassName(), null)) });
      }
      catch (Exception e) {
        logger.warn("Cannot create instance of " + beanDefinition.getBeanClassName(), e);
      }
    }
    ExceptionClassifierRetryPolicy extra = new ExceptionClassifierRetryPolicy();
    extra.setExceptionClassifier(new SubclassClassifier<>(new AlwaysRetryPolicy()));
    result.add(new Object[] { extra });
    return result;
  }

  @SuppressWarnings("deprecation")
  @ParameterizedTest
  @MethodSource("policies")
  public void testSerializationCycleForContext(RetryPolicy policy) {
    RetryContext context = policy.open(null);
    assertThat(context.getRetryCount()).isEqualTo(0);
    policy.registerThrowable(context, new RuntimeException());
    assertThat(context.getRetryCount()).isEqualTo(1);
    assertThat(
            ((RetryContext) SerializationUtils.deserialize(SerializationUtils.serialize(context))).getRetryCount())
            .isEqualTo(1);
  }

  @ParameterizedTest
  @MethodSource("policies")
  @SuppressWarnings("deprecation")
  public void testSerializationCycleForPolicy(RetryPolicy policy) {
    assertThat(SerializationUtils.deserialize(SerializationUtils.serialize(policy)) instanceof RetryPolicy)
            .isTrue();
  }

}
