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

package infra.persistence.sql;



import infra.persistence.platform.Platform;

/**
 * A restriction that appends a caller-provided SQL fragment unchanged.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Restrictions#plain(CharSequence)
 * @since 5.0
 */
final class PlainRestriction implements Restriction {

  private final CharSequence sequence;

  PlainRestriction(CharSequence sequence) {
    this.sequence = sequence;
  }

  @Override
  public void render(Platform platform, StringBuilder sqlBuffer) {
    sqlBuffer.append(sequence);
  }

}
