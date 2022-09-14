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

package cn.taketoday.context.expression;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanIsNotAFactoryException;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.BeanResolver;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for expressions accessing beans and factory beans.
 *
 * @author Andy Clement
 */
public class FactoryBeanAccessTests {

  @Test
  public void factoryBeanAccess() { // SPR9511
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setBeanResolver(new SimpleBeanResolver());
    Expression expr = new SpelExpressionParser().parseRaw("@car.colour");
    assertThat(expr.getValue(context)).isEqualTo("red");
    expr = new SpelExpressionParser().parseRaw("&car.class.name");
    assertThat(expr.getValue(context)).isEqualTo(SimpleBeanResolver.CarFactoryBean.class.getName());

    expr = new SpelExpressionParser().parseRaw("@boat.colour");
    assertThat(expr.getValue(context)).isEqualTo("blue");
    Expression notFactoryExpr = new SpelExpressionParser().parseRaw("&boat.class.name");
    assertThatExceptionOfType(BeanIsNotAFactoryException.class).isThrownBy(() ->
            notFactoryExpr.getValue(context));

    // No such bean
    Expression noBeanExpr = new SpelExpressionParser().parseRaw("@truck");
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
            noBeanExpr.getValue(context));

    // No such factory bean
    Expression noFactoryBeanExpr = new SpelExpressionParser().parseRaw("&truck");
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
            noFactoryBeanExpr.getValue(context));
  }

  static class SimpleBeanResolver implements BeanResolver {

    static class Car {

      public String getColour() {
        return "red";
      }
    }

    static class CarFactoryBean implements FactoryBean<Car> {

      @Override
      public Car getObject() {
        return new Car();
      }

      @Override
      public Class<Car> getObjectType() {
        return Car.class;
      }

      @Override
      public boolean isSingleton() {
        return false;
      }

    }

    static class Boat {

      public String getColour() {
        return "blue";
      }

    }

    StaticApplicationContext ac = new StaticApplicationContext();

    public SimpleBeanResolver() {
      ac.registerSingleton("car", CarFactoryBean.class);
      ac.registerSingleton("boat", Boat.class);
    }

    @Override
    public Object resolve(EvaluationContext context, String beanName)
            throws AccessException {
      return ac.getBean(beanName);
    }
  }

}
