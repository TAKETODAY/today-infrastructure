/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.expression.spel.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.Operation;
import cn.taketoday.expression.OperatorOverloader;
import cn.taketoday.expression.TypeComparator;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.expression.TypeLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class StandardComponentsTests {

  @Test
  public void testStandardEvaluationContext() {
    StandardEvaluationContext context = new StandardEvaluationContext();
    assertThat(context.getTypeComparator()).isNotNull();

    TypeComparator tc = new StandardTypeComparator();
    context.setTypeComparator(tc);
    assertThat(context.getTypeComparator()).isEqualTo(tc);

    TypeLocator tl = new StandardTypeLocator();
    context.setTypeLocator(tl);
    assertThat(context.getTypeLocator()).isEqualTo(tl);
  }

  @Test
  public void testStandardOperatorOverloader() throws EvaluationException {
    OperatorOverloader oo = new StandardOperatorOverloader();
    assertThat(oo.overridesOperation(Operation.ADD, null, null)).isFalse();
    assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
            oo.operate(Operation.ADD, 2, 3));
  }

  @Test
  public void testStandardTypeLocator() {
    StandardTypeLocator tl = new StandardTypeLocator();
    List<String> prefixes = tl.getImportPrefixes();
    assertThat(prefixes.size()).isEqualTo(1);
    tl.registerImport("java.util");
    prefixes = tl.getImportPrefixes();
    assertThat(prefixes.size()).isEqualTo(2);
    tl.removeImport("java.util");
    prefixes = tl.getImportPrefixes();
    assertThat(prefixes.size()).isEqualTo(1);
  }

  @Test
  public void testStandardTypeConverter() throws EvaluationException {
    TypeConverter tc = new StandardTypeConverter();
    tc.convertValue(3, TypeDescriptor.forObject(3), TypeDescriptor.valueOf(Double.class));
  }

}
