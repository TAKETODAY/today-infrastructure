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

package infra.beans.factory.xml;

import java.util.Properties;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/15 19:36
 */
public class MailSession {

  private Properties props;

  private MailSession() {
  }

  public void setProperties(Properties props) {
    this.props = props;
  }

  public static MailSession getDefaultInstance(Properties props) {
    MailSession session = new MailSession();
    session.setProperties(props);
    return session;
  }

  public Object getProperty(String key) {
    return this.props.get(key);
  }
}
