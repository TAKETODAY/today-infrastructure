/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web;

import org.junit.jupiter.api.Test;

import infra.core.AttributeAccessor;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/19 11:44
 */
class RedirectModelTests {

  @Test
  void emptyRedirectModelHasNoPathOrParams() {
    RedirectModel model = new RedirectModel();
    assertThat(model.getTargetRequestPath()).isNull();
    assertThat(model.getTargetRequestParams()).isEmpty();
  }

  @Test
  void constructorWithAttributeStoresValue() {
    RedirectModel model = new RedirectModel("name", "value");
    assertThat(model.getAttribute("name")).isEqualTo("value");
  }

  @Test
  void addTargetRequestParamsWithEmptyValuesAreSkipped() {
    RedirectModel model = new RedirectModel();
    model.addTargetRequestParam("", "value");
    model.addTargetRequestParam("name", "");

    assertThat(model.getTargetRequestParams()).isEmpty();
  }

  @Test
  void compareToPreferredModelWithPath() {
    RedirectModel model1 = new RedirectModel();
    RedirectModel model2 = new RedirectModel();

    model1.setTargetRequestPath("/path");

    assertThat(model1.compareTo(model2)).isLessThan(0);
    assertThat(model2.compareTo(model1)).isGreaterThan(0);
  }

  @Test
  void compareToPreferredModelWithMoreParams() {
    RedirectModel model1 = new RedirectModel();
    RedirectModel model2 = new RedirectModel();

    model1.addTargetRequestParam("param1", "value1");
    model1.addTargetRequestParam("param2", "value2");
    model2.addTargetRequestParam("param1", "value1");

    assertThat(model1.compareTo(model2)).isLessThan(0);
  }

  @Test
  void expirationTimeTracking() throws InterruptedException {
    RedirectModel model = new RedirectModel();
    assertThat(model.isExpired()).isFalse();

    model.startExpirationPeriod(1);
    assertThat(model.getExpirationTime()).isGreaterThan(System.currentTimeMillis());

    sleep(1100);
    assertThat(model.isExpired()).isTrue();
  }

  @Test
  void findOutputModelReturnsNullForMissingAttribute() {
    AttributeAccessor accessor = mock(AttributeAccessor.class);
    when(accessor.getAttribute(RedirectModel.OUTPUT_ATTRIBUTE)).thenReturn(null);

    RedirectModel outputModel = RedirectModel.findOutputModel(accessor);
    assertThat((Object) outputModel).isNull();
  }

  @Test
  void equalsAndHashCodeConsistency() {
    RedirectModel model1 = new RedirectModel();
    model1.setTargetRequestPath("/path");
    model1.addTargetRequestParam("param", "value");
    model1.setAttribute("attr", "value");

    RedirectModel model2 = new RedirectModel();
    model2.setTargetRequestPath("/path");
    model2.addTargetRequestParam("param", "value");
    model2.setAttribute("attr", "value");

    assertThat((Object) model1).isEqualTo(model2);
    assertThat(model1.hashCode()).isEqualTo(model2.hashCode());
  }

}