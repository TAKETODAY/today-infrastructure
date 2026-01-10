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

package infra.oxm.xstream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/20 11:47
 */
class CatchAllConverterTests {

  @Test
  void marshal() {

    CatchAllConverter converter = new CatchAllConverter();

    assertThat(converter.canConvert(int.class)).isTrue();

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> converter.marshal(null, null, null))
            .withMessage("Marshalling not supported");

    assertThatThrownBy(() -> converter.unmarshal(null, null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Unmarshalling not supported");

  }

}