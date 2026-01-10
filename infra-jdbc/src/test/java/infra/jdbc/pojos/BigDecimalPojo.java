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

package infra.jdbc.pojos;

import java.math.BigDecimal;

/**
 * Created by IntelliJ IDEA. User: lars Date: 11/15/11 Time: 10:18 AM To change
 * this template use File | Settings | File Templates.
 */
public class BigDecimalPojo {
  public int id;

  public BigDecimal val1;

  public BigDecimal val2;

  public void setId(int id) {
    this.id = id;
  }

  public void setVal1(BigDecimal val1) {
    this.val1 = val1;
  }

  public void setVal2(BigDecimal val2) {
    this.val2 = val2;
  }

  public int getId() {
    return id;
  }

  public BigDecimal getVal1() {
    return val1;
  }

  public BigDecimal getVal2() {
    return val2;
  }
}
