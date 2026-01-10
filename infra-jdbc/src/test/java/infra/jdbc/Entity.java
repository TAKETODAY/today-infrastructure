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

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Created by IntelliJ IDEA. User: lars Date: 5/21/11 Time: 10:15 PM To change
 * this template use File | Settings | File Templates.
 */
public class Entity {

  public long id;
  public String text;
  public Date time;
  public Timestamp ts;
  public Integer aNumber;
  public Long aLongNumber;

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  public Timestamp getTs() {
    return ts;
  }

  public void setTs(Timestamp ts) {
    this.ts = ts;
  }

  public Integer getaNumber() {
    return aNumber;
  }

  public void setaNumber(Integer aNumber) {
    this.aNumber = aNumber;
  }
}
