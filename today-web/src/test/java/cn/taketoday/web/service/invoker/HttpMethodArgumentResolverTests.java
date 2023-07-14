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

import org.junit.jupiter.api.Test;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.service.annotation.GetExchange;
import cn.taketoday.web.service.annotation.HttpExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link HttpMethodArgumentResolver}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 */
class HttpMethodArgumentResolverTests {

  private final TestExchangeAdapter client = new TestExchangeAdapter();

  private final Service service =
          HttpServiceProxyFactory.forAdapter(this.client).build().createClient(Service.class);

  @Test
  void httpMethodFromArgument() {
    this.service.execute(HttpMethod.POST);
    assertThat(getActualMethod()).isEqualTo(HttpMethod.POST);
  }

  @Test
  void httpMethodFromAnnotation() {
    this.service.executeHttpHead();
    assertThat(getActualMethod()).isEqualTo(HttpMethod.HEAD);
  }

  @Test
  void notHttpMethod() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.service.executeNotHttpMethod("test"))
            .withMessage("Could not resolve parameter [0] in " +
                    "public abstract void cn.taketoday.web.service.invoker." +
                    "HttpMethodArgumentResolverTests$Service.executeNotHttpMethod(java.lang.String): " +
                    "No suitable resolver");
  }

  @Test
  void nullHttpMethod() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.service.execute(null));
  }

  @Nullable
  private HttpMethod getActualMethod() {
    return this.client.getRequestValues().getHttpMethod();
  }

  private interface Service {

    @HttpExchange
    void execute(HttpMethod method);

    @HttpExchange(method = "HEAD")
    void executeHttpHead();

    @GetExchange
    void executeNotHttpMethod(String test);

  }

}
