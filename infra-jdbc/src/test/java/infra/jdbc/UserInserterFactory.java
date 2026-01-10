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

package infra.jdbc;

/**
 * Created by zsoltjanos on 01/08/15.
 */
public class UserInserterFactory {

  public static UserInserter buildUserInserter(boolean useBind) {
    if (useBind) {
      return new BindUserInserter();
    }
    else {
      return new PlainUserInserter();
    }
  }

  private static class BindUserInserter implements UserInserter {

    @Override
    public void insertUser(NamedQuery insertQuery, int idx) {
      User user = new User();
      user.name = "a name " + idx;
      user.setEmail(String.format("test%s@email.com", idx));
      user.text = "some text";
      insertQuery.bind(user).addToBatch();
    }
  }

  private static class PlainUserInserter implements UserInserter {

    @Override
    public void insertUser(NamedQuery insertQuery, int idx) {
      insertQuery.addParameter("name", "a name " + idx)
              .addParameter("email", String.format("test%s@email.com", idx))
              .addParameter("text", "some text").addToBatch();
    }
  }
}
