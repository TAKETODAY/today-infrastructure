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

package infra.samples;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import infra.http.HttpHeaders;
import org.jspecify.annotations.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ExceptionUtils;
import infra.util.concurrent.Future;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.annotation.GET;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.async.DeferredResult;
import infra.web.async.WebAsyncTask;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/5 20:55
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/async")
class AsyncController {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @GET("/deferred-result")
  public DeferredResult<String> deferredResult(@Nullable Long timeout) {
    DeferredResult<String> result = new DeferredResult<>(timeout, "Timeout");
    Future.run(() -> {
      logger.info("异步任务开始执行");

      ExceptionUtils.sneakyThrow(() -> TimeUnit.SECONDS.sleep(2));

      logger.info("异步任务执行结束，开始返回");

      if (result.setResult("result from " + Thread.currentThread().getName())) {
        logger.info("返回结果设置成功");
      }
      else {
        logger.info("返回结果设置失败，已经返回任务可能超时");
      }

      logger.info("异步任务执行结束");
    });

    result.onTimeout(() ->
            logger.warn("任务执行超时了"));

    result.onCompletion(() ->
            logger.info("结束回调"));

    result.onError(throwable ->
            logger.error("出现异常", throwable));

    return result;
  }

  @GET("/callable")
  public Callable<String> callable() {
    return () -> {
      // 可以获取 RequestContext
      RequestContext request = RequestContextHolder.getRequired();
      logger.info("异步任务开始执行");
//        TimeUnit.SECONDS.sleep(2);
      LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
      HttpHeaders headers = request.getHeaders();
      logger.info(headers.toString());
      logger.info("异步任务执行结束，开始返回");
      return "result from " + Thread.currentThread().getName();
    };
  }

  @GET("/web-async-task")
  public WebAsyncTask<String> webAsyncTask(@Nullable Long timeout) {
    return new WebAsyncTask<>(timeout, callable());
  }

}
