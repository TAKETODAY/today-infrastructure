/*
 * Copyright 2017 - 2023 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.service.invoker;

import java.util.Collections;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HttpExchangeAdapter} with stubbed responses.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
public class TestExchangeAdapter implements HttpExchangeAdapter {

  @Nullable
  private String invokedMethodName;

  @Nullable
  private HttpRequestValues requestValues;

  @Nullable
  private ParameterizedTypeReference<?> bodyType;

  public String getInvokedMethodName() {
    assertThat(this.invokedMethodName).isNotNull();
    return this.invokedMethodName;
  }

  public HttpRequestValues getRequestValues() {
    assertThat(this.requestValues).isNotNull();
    return this.requestValues;
  }

  @Nullable
  public ParameterizedTypeReference<?> getBodyType() {
    return this.bodyType;
  }

  @Override
  public void exchange(HttpRequestValues requestValues) {
    saveInput("exchange", requestValues, null);
  }

  @Override
  public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
    saveInput("exchangeForHeaders", requestValues, null);
    return HttpHeaders.forWritable();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    saveInput("exchangeForBody", requestValues, bodyType);
    return bodyType.getType().getTypeName().contains("List") ?
           (T) Collections.singletonList(getInvokedMethodName()) : (T) getInvokedMethodName();
  }

  @Override
  public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
    saveInput("exchangeForBodilessEntity", requestValues, null);
    return ResponseEntity.ok().build();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> ResponseEntity<T> exchangeForEntity(
          HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

    saveInput("exchangeForEntity", requestValues, bodyType);
    return (ResponseEntity<T>) ResponseEntity.ok(getInvokedMethodName());
  }

  @Override
  public boolean supportsRequestAttributes() {
    return true;
  }

  protected <T> void saveInput(
          String methodName, HttpRequestValues values, @Nullable ParameterizedTypeReference<T> bodyType) {

    this.invokedMethodName = methodName;
    this.requestValues = values;
    this.bodyType = bodyType;
  }

}
