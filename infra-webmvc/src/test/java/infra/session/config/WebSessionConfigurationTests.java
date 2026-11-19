package infra.session.config;

import org.junit.jupiter.api.Test;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.session.PersistenceSessionRepository;
import infra.session.Session;
import infra.session.SessionEventDispatcher;
import infra.session.SessionPersister;
import infra.session.SessionRepository;
import infra.web.context.support.SessionScope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/31 11:30
 */
class WebSessionConfigurationTests {

  @Test
  void sessionScopedBeanRegistersCleanupOfItsDestructionCallback() {
    var configuration = new WebSessionConfiguration();
    var definition = new RootBeanDefinition(Object.class);
    definition.setScope(infra.web.context.annotation.SessionScope.NAME);
    configuration.postProcessMergedBeanDefinition(definition, Object.class, "scoped");
    configuration.postProcessMergedBeanDefinition(definition, Object.class, "other");

    var dispatcher = new SessionEventDispatcher();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.getBeanFactory().registerSingleton("dispatcher", dispatcher);
      context.getBeanFactory().registerSingleton("repository", mock(SessionRepository.class));
      context.refresh();
      configuration.afterSingletonsInstantiated(context.getBeanFactory());
    }

    Session session = mock(Session.class);
    dispatcher.attributeRemoved(session, "scoped", new Object());
    verify(session).removeAttribute(SessionScope.DESTRUCTION_CALLBACK_NAME_PREFIX + "scoped");
  }

  @Test
  void customPersisterEnablesPersistenceWithoutTheProperty() throws Exception {
    var persister = mock(SessionPersister.class);
    SessionRepository repository = WebSessionConfiguration.sessionRepository(new SessionProperties(),
            new SessionEventDispatcher(), () -> "id", persister, null);

    assertThat(repository).isInstanceOf(PersistenceSessionRepository.class);
    assertThat(repository.retrieveSession("id")).isNull();
    verify(persister).findById("id");
  }

  @Test
  void isEnableDependencyInjection() {
    try (var context = new AnnotationConfigApplicationContext(WebSessionConfiguration.class)) {
      BeanDefinition definition = context.getBeanDefinition(WebSessionConfiguration.class);
      assertThat(definition.isEnableDependencyInjection()).isFalse();
    }
  }

}
