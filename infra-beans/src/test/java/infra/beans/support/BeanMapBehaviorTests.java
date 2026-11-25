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

package infra.beans.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BeanMapBehaviorTests {

  @Test
  void writingSetterOnlyPropertyReturnsNullAsPreviousValue() {
    var bean = new SetterOnlyBean();
    var map = BeanMap.forInstance(bean);
    assertThat(map.containsKey("name")).isTrue();
    assertThat(map.containsKey(1)).isFalse();
    assertThat(map.containsKey("missing")).isFalse();
    assertThat(map.put("name", "first")).isNull();
    assertThat(bean.storedName).isEqualTo("first");
    assertThat(map.put("name", "second")).isNull();
    assertThat(bean.storedName).isEqualTo("second");
  }

  @Test
  void beanWithoutDeclaredPropertiesStillExposesClassProperty() {
    var map = BeanMap.forInstance(new EmptyBean());
    assertThat(map.isEmpty()).isFalse();
    assertThat(map.keySet()).containsExactly("class");
    assertThat(map.get("class")).isEqualTo(EmptyBean.class);
  }

  @Test
  void unchangedReadOnlyPolicyReusesMap() {
    var map = BeanMap.forInstance(new SetterOnlyBean());
    assertThat(map.withIgnoreReadOnly(false)).isSameAs(map);
    var ignoring = map.withIgnoreReadOnly(true);
    assertThat(ignoring).isNotSameAs(map);
    assertThat(ignoring.getTarget()).isSameAs(map.getTarget());
    assertThat(ignoring.withIgnoreReadOnly(true)).isSameAs(ignoring);
  }

  static class SetterOnlyBean {

    private String storedName;

    public void setName(String name) {
      this.storedName = name;
    }

  }

  static class EmptyBean {
  }

}
