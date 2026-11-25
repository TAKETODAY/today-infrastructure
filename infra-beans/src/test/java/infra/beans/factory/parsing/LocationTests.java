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

package infra.beans.factory.parsing;

import org.junit.jupiter.api.Test;

import infra.core.io.DescriptiveResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LocationTests {

  @Test
  void resourceOnlyLocationHasNoSource() {
    var resource = new DescriptiveResource("configuration.xml");
    var location = new Location(resource);
    assertThat(location.getResource()).isSameAs(resource);
    assertThat(location.getSource()).isNull();
  }

  @Test
  void locationRetainsResourceAndSource() {
    var resource = new DescriptiveResource("configuration.xml");
    var source = new Object();
    var location = new Location(resource, source);
    assertThat(location.getResource()).isSameAs(resource);
    assertThat(location.getSource()).isSameAs(source);
  }

  @Test
  void resourceIsRequired() {
    assertThatIllegalArgumentException().isThrownBy(() -> new Location(null));
  }

}
