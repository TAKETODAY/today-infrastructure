/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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