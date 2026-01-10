/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.codec.support;

import infra.http.codec.ClientCodecConfigurer;

/**
 * Default implementation of {@link ClientCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultClientCodecConfigurer extends BaseCodecConfigurer implements ClientCodecConfigurer {

  public DefaultClientCodecConfigurer() {
    super(new ClientDefaultCodecsImpl());
  }

  private DefaultClientCodecConfigurer(DefaultClientCodecConfigurer other) {
    super(other);
  }

  @Override
  public ClientDefaultCodecs defaultCodecs() {
    return (ClientDefaultCodecs) super.defaultCodecs();
  }

  @Override
  public DefaultClientCodecConfigurer clone() {
    return new DefaultClientCodecConfigurer(this);
  }

  @Override
  protected BaseDefaultCodecs cloneDefaultCodecs() {
    return new ClientDefaultCodecsImpl((ClientDefaultCodecsImpl) defaultCodecs());
  }

}
