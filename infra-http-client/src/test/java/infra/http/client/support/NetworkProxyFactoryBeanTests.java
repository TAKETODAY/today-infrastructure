/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.http.client.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.net.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class NetworkProxyFactoryBeanTests {

  @Test
  void exposesASingletonOfTheConfiguredProxyType() {
    var factory = new NetworkProxyFactoryBean();
    factory.setType(Proxy.Type.SOCKS);
    factory.setHostname("127.0.0.1");
    factory.setPort(1080);

    factory.afterPropertiesSet();

    var proxy = factory.getObject();
    assertThat(proxy).isNotNull().isSameAs(factory.getObject());
    assertThat(proxy.type()).isEqualTo(Proxy.Type.SOCKS);
    assertThat(proxy.address()).isEqualTo(new InetSocketAddress("127.0.0.1", 1080));
    assertThat(factory.getObjectType()).isEqualTo(Proxy.class);
    assertThat(factory.isSingleton()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = { -1, 65536 })
  void rejectsPortsOutsideTheValidRange(int port) {
    var factory = new NetworkProxyFactoryBean();
    factory.setHostname("127.0.0.1");
    factory.setPort(port);

    assertThatIllegalArgumentException().isThrownBy(factory::afterPropertiesSet)
            .withMessage("Property 'port' value out of range: " + port);
  }

}
