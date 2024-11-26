/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package example.scannable;

import java.util.Comparator;
import java.util.List;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Lookup;
import infra.context.ApplicationContext;
import infra.context.ApplicationEventPublisher;
import infra.context.ConfigurableApplicationContext;
import infra.context.MessageSource;
import infra.context.annotation.DependsOn;
import infra.context.annotation.Lazy;
import infra.context.support.AbstractApplicationContext;
import infra.core.io.PatternResourceLoader;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.stereotype.Service;
import infra.util.concurrent.Future;
import jakarta.annotation.PostConstruct;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
@Service
@Lazy
@DependsOn("myNamedComponent")
public abstract class FooServiceImpl implements FooService {

  // Just to test ASM5's bytecode parsing of INVOKESPECIAL/STATIC on interfaces
  @SuppressWarnings("unused")
  private static final Comparator<MessageBean> COMPARATOR_BY_MESSAGE = Comparator.comparing(MessageBean::getMessage);

  @Autowired
  private FooDao fooDao;

  @Autowired
  public BeanFactory beanFactory;

  @Autowired
  public List<BeanFactory> listableBeanFactory;

  @Autowired
  public ResourceLoader resourceLoader;

  @Autowired
  public PatternResourceLoader resourcePatternResolver;

  @Autowired
  public ApplicationEventPublisher eventPublisher;

  @Autowired
  public MessageSource messageSource;

  @Autowired
  public ApplicationContext context;

  @Autowired
  public ConfigurableApplicationContext[] configurableContext;

  @Autowired
  public AbstractApplicationContext genericContext;

  private boolean initCalled = false;

  @PostConstruct
  private void init() {
    if (this.initCalled) {
      throw new IllegalStateException("Init already called");
    }
    this.initCalled = true;
  }

  @Override
  public String foo(int id) {
    return this.fooDao.findFoo(id);
  }

  public String lookupFoo(int id) {
    return fooDao().findFoo(id);
  }

  @Override
  public Future<String> asyncFoo(int id) {
    System.out.println(Thread.currentThread().getName());
    Assert.state(ServiceInvocationCounter.getThreadLocalCount() != null, "Thread-local counter not exposed");
    return Future.ok(fooDao().findFoo(id));
  }

  @Override
  public boolean isInitCalled() {
    return this.initCalled;
  }

  @Lookup
  protected abstract FooDao fooDao();

}
