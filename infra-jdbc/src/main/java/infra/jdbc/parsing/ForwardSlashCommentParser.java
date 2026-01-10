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

package infra.jdbc.parsing;

/**
 * Created by lars on 22.09.2014.
 */
public class ForwardSlashCommentParser extends AbstractCommentParser {

  private boolean commentAlmostEnded;

  @Override
  protected void startParsing() {
    commentAlmostEnded = false;
  }

  @Override
  public boolean supports(char c, String sql, int idx) {
    return sql.length() > idx + 1 && c == '/' && sql.charAt(idx + 1) == '*';
  }

  @Override
  public boolean isEndComment(char c) {
    if (commentAlmostEnded && c == '/') {
      return true;
    }
    commentAlmostEnded = c == '*';
    return false;
  }
}
