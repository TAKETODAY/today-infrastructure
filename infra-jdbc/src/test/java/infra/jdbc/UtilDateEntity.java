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

import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: lars Date: 8/30/11 Time: 10:59 AM To change
 * this template use File | Settings | File Templates.
 */
public class UtilDateEntity {

  public int id;
  public Date d1;
  private Date d2;
  private Date d3;

  public Date getD2() {
    return d2;
  }

  public void setD2(Date d2) {
    this.d2 = d2;
  }

  public Date getD3() {
    return d3;
  }

  public void setD3(Date d3) {
    this.d3 = d3;
  }
}
