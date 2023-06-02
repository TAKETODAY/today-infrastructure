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

package cn.taketoday.transaction.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.annotation.RepeatableContainers;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.interceptor.NoRollbackRuleAttribute;
import cn.taketoday.transaction.interceptor.RollbackRuleAttribute;
import cn.taketoday.transaction.interceptor.RuleBasedTransactionAttribute;
import cn.taketoday.transaction.interceptor.TransactionAttribute;
import cn.taketoday.util.StringUtils;

/**
 * Strategy implementation for parsing Framework's {@link Transactional} annotation.
 *
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @since 4.0
 */
@SuppressWarnings("serial")
public class TransactionalAnnotationParser implements TransactionAnnotationParser, Serializable {

  @Override
  public boolean isCandidateClass(Class<?> targetClass) {
    return AnnotationUtils.isCandidateClass(targetClass, Transactional.class);
  }

  @Override
  @Nullable
  public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
    MergedAnnotation<Transactional> attributes = MergedAnnotations.from(
            element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none()).get(Transactional.class);
    if (attributes.isPresent()) {
      return parseTransactionAnnotation(attributes);
    }
    else {
      return null;
    }
  }

  public TransactionAttribute parseTransactionAnnotation(Transactional ann) {
    return parseTransactionAnnotation(MergedAnnotation.from(ann));
  }

  protected TransactionAttribute parseTransactionAnnotation(MergedAnnotation<Transactional> attributes) {
    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();

    Propagation propagation = attributes.getEnum("propagation", Propagation.class);
    rbta.setPropagationBehavior(propagation.value());
    Isolation isolation = attributes.getEnum("isolation", Isolation.class);
    rbta.setIsolationLevel(isolation.value());

    rbta.setTimeout(attributes.getInt("timeout"));
    String timeoutString = attributes.getString("timeoutString");
    Assert.isTrue(StringUtils.isBlank(timeoutString) || rbta.getTimeout() < 0,
            "Specify 'timeout' or 'timeoutString', not both");
    rbta.setTimeoutString(timeoutString);

    rbta.setReadOnly(attributes.getBoolean("readOnly"));
    rbta.setQualifier(attributes.getString("value"));
    rbta.setLabels(Arrays.asList(attributes.getStringArray("label")));

    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();
    for (Class<?> rbRule : attributes.getClassArray("rollbackFor")) {
      rollbackRules.add(new RollbackRuleAttribute(rbRule));
    }
    for (String rbRule : attributes.getStringArray("rollbackForClassName")) {
      rollbackRules.add(new RollbackRuleAttribute(rbRule));
    }
    for (Class<?> rbRule : attributes.getClassArray("noRollbackFor")) {
      rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
    }
    for (String rbRule : attributes.getStringArray("noRollbackForClassName")) {
      rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
    }
    rbta.setRollbackRules(rollbackRules);

    return rbta;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (other instanceof TransactionalAnnotationParser);
  }

  @Override
  public int hashCode() {
    return TransactionalAnnotationParser.class.hashCode();
  }

}
