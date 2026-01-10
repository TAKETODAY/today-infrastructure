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

package infra.web.mock;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/26 15:07
 */
class MockUtilsTests {

  @Test
  public void findParameterValue() {
    Map<String, Object> params = new HashMap<>();
    params.put("myKey1", "myValue1");
    params.put("myKey2_myValue2", "xxx");
    params.put("myKey3_myValue3.x", "xxx");
    params.put("myKey4_myValue4.y", new String[] { "yyy" });

    assertThat(MockUtils.findParameterValue(params, "myKey0")).isNull();
    assertThat(MockUtils.findParameterValue(params, "myKey1")).isEqualTo("myValue1");
    assertThat(MockUtils.findParameterValue(params, "myKey2")).isEqualTo("myValue2");
    assertThat(MockUtils.findParameterValue(params, "myKey3")).isEqualTo("myValue3");
    assertThat(MockUtils.findParameterValue(params, "myKey4")).isEqualTo("myValue4");
  }

}
