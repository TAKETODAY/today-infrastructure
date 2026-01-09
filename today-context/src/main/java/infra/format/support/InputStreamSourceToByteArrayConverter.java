/*
 * Copyright 2012-present the original author or authors.
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

package infra.format.support;

import java.io.IOException;

import infra.core.conversion.Converter;
import infra.core.io.InputStreamSource;
import infra.core.io.Resource;
import infra.util.FileCopyUtils;

/**
 * {@link Converter} to convert from an {@link InputStreamSource} to a {@code byte[]}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class InputStreamSourceToByteArrayConverter implements Converter<InputStreamSource, byte[]> {

  @Override
  public byte[] convert(InputStreamSource source) {
    try {
      return FileCopyUtils.copyToByteArray(source.getInputStream());
    }
    catch (IOException ex) {
      throw new IllegalStateException("Unable to read from " + getName(source), ex);
    }
  }

  private String getName(InputStreamSource source) {
    if (source instanceof Resource) {
      return source.toString();
    }
    return "input stream source";
  }

}
