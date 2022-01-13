package cn.taketoday.context.annotation;

import cn.taketoday.context.Condition;
import cn.taketoday.context.loader.ConditionEvaluationContext;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

/**
 * Never match Condition
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/13 17:12
 */
public class NeverMatchedCondition implements Condition {

  @Override
  public boolean matches(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
    return false;
  }

}
