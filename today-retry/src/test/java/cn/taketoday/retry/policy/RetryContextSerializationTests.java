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

package cn.taketoday.retry.policy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.classify.SubclassClassifier;
import cn.taketoday.context.loader.ClassPathScanningCandidateComponentProvider;
import cn.taketoday.core.type.filter.AssignableTypeFilter;
import cn.taketoday.core.type.filter.RegexPatternTypeFilter;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.SerializationUtils;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 */
@RunWith(Parameterized.class)
public class RetryContextSerializationTests {

  private static Logger logger = LoggerFactory.getLogger(RetryContextSerializationTests.class);

  private RetryPolicy policy;

  @Parameters(name = "{index}: {0}")
  public static List<Object[]> policies() {
    List<Object[]> result = new ArrayList<Object[]>();
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
    scanner.addIncludeFilter(new AssignableTypeFilter(RetryPolicy.class));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Test.*")));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Mock.*")));
    Set<BeanDefinition> candidates = scanner.findCandidateComponents("cn.taketoday.retry.policy");
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

  public RetryContextSerializationTests(RetryPolicy policy) {
    this.policy = policy;
  }

  @Test
  public void testSerializationCycleForContext() {
    RetryContext context = policy.open(null);
    assertEquals(0, context.getRetryCount());
    policy.registerThrowable(context, new RuntimeException());
    assertEquals(1, context.getRetryCount());
    assertEquals(1,
            ((RetryContext) SerializationUtils.deserialize(SerializationUtils.serialize(context))).getRetryCount());
  }

  @Test
  public void testSerializationCycleForPolicy() {
    assertTrue(SerializationUtils.deserialize(SerializationUtils.serialize(policy)) instanceof RetryPolicy);
  }

}
