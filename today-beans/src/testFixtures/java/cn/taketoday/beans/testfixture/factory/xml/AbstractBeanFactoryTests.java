/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.beans.testfixture.factory.xml;

import org.junit.jupiter.api.Test;

import java.beans.PropertyEditorSupport;
import java.util.StringTokenizer;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanIsNotAFactoryException;
import cn.taketoday.beans.factory.BeanNotOfRequiredTypeException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.testfixture.beans.LifecycleBean;
import cn.taketoday.beans.testfixture.beans.MustBeInitialized;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.beans.testfixture.beans.factory.DummyFactory;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/24 09:02
 */
public abstract class AbstractBeanFactoryTests {

	protected abstract BeanFactory getBeanFactory();

	/**
	 * Subclasses can override this.
	 */
	@Test
	public void count() {
		assertCount(13);
	}

	protected final void assertCount(int count) {
		String[] defnames = getBeanFactory().getBeanDefinitionNames();
		assertThat(defnames.length == count).as("We should have " + count + " beans, not " + defnames.length).isTrue();
	}

	protected void assertTestBeanCount(int count) {
		String[] defNames = StringUtils.toStringArray(getBeanFactory().getBeanNamesForType(TestBean.class, true, false));
		assertThat(defNames.length == count).as("We should have " + count + " beans for class cn.taketoday.beans.testfixture.beans.TestBean, not " +
						defNames.length).isTrue();

		int countIncludingFactoryBeans = count + 2;
		String[] names = StringUtils.toStringArray(getBeanFactory().getBeanNamesForType(TestBean.class, true, true));
		assertThat(names.length == countIncludingFactoryBeans).as("We should have " + countIncludingFactoryBeans +
						" beans for class cn.taketoday.beans.testfixture.beans.TestBean, not " + names.length).isTrue();
	}

	/**
	 * Roderick bean inherits from rod, overriding name only.
	 */
	@Test
	public void inheritance() {
		assertThat(getBeanFactory().containsBean("rod")).isTrue();
		assertThat(getBeanFactory().containsBean("roderick")).isTrue();
		TestBean rod = (TestBean) getBeanFactory().getBean("rod");
		TestBean roderick = (TestBean) getBeanFactory().getBean("roderick");
		assertThat(rod != roderick).as("not == ").isTrue();
		assertThat(rod.getName().equals("Rod")).as("rod.name is Rod").isTrue();
		assertThat(rod.getAge() == 31).as("rod.age is 31").isTrue();
		assertThat(roderick.getName().equals("Roderick")).as("roderick.name is Roderick").isTrue();
		assertThat(roderick.getAge() == rod.getAge()).as("roderick.age was inherited").isTrue();
	}

	@Test
	public void getBeanWithNullArg() {
		assertThatIllegalArgumentException().isThrownBy(() ->
						getBeanFactory().getBean((String) null));
	}

	/**
	 * Test that InitializingBean objects receive the afterPropertiesSet() callback
	 */
	@Test
	public void initializingBeanCallback() {
		MustBeInitialized mbi = (MustBeInitialized) getBeanFactory().getBean("mustBeInitialized");
		// The dummy business method will throw an exception if the
		// afterPropertiesSet() callback wasn't invoked
		mbi.businessMethod();
	}

	/**
	 * Test that InitializingBean/BeanFactoryAware/DisposableBean objects receive the
	 * afterPropertiesSet() callback before BeanFactoryAware callbacks
	 */
	@Test
	public void lifecycleCallbacks() {
		LifecycleBean lb = (LifecycleBean) getBeanFactory().getBean("lifecycle");
		assertThat(lb.getBeanName()).isEqualTo("lifecycle");
		// The dummy business method will throw an exception if the
		// necessary callbacks weren't invoked in the right order.
		lb.businessMethod();
		boolean condition = !lb.isDestroyed();
		assertThat(condition).as("Not destroyed").isTrue();
	}

	@Test
	public void findsValidInstance() {
		Object o = getBeanFactory().getBean("rod");
		boolean condition = o instanceof TestBean;
		assertThat(condition).as("Rod bean is a TestBean").isTrue();
		TestBean rod = (TestBean) o;
		assertThat(rod.getName().equals("Rod")).as("rod.name is Rod").isTrue();
		assertThat(rod.getAge() == 31).as("rod.age is 31").isTrue();
	}

	@Test
	public void getInstanceByMatchingClass() {
		Object o = getBeanFactory().getBean("rod", TestBean.class);
		boolean condition = o instanceof TestBean;
		assertThat(condition).as("Rod bean is a TestBean").isTrue();
	}

	@Test
	public void getInstanceByNonmatchingClass() {
		assertThatExceptionOfType(BeanNotOfRequiredTypeException.class).isThrownBy(() ->
										getBeanFactory().getBean("rod", BeanFactory.class))
						.satisfies(ex -> {
							assertThat(ex.getBeanName()).isEqualTo("rod");
							assertThat(ex.getRequiredType()).isEqualTo(BeanFactory.class);
							assertThat(ex.getActualType()).isEqualTo(TestBean.class).isEqualTo(getBeanFactory().getBean("rod").getClass());
						});
	}

	@Test
	public void getSharedInstanceByMatchingClass() {
		Object o = getBeanFactory().getBean("rod", TestBean.class);
		boolean condition = o instanceof TestBean;
		assertThat(condition).as("Rod bean is a TestBean").isTrue();
	}

	@Test
	public void getSharedInstanceByMatchingClassNoCatch() {
		Object o = getBeanFactory().getBean("rod", TestBean.class);
		boolean condition = o instanceof TestBean;
		assertThat(condition).as("Rod bean is a TestBean").isTrue();
	}

	@Test
	public void getSharedInstanceByNonmatchingClass() {
		assertThatExceptionOfType(BeanNotOfRequiredTypeException.class).isThrownBy(() ->
										getBeanFactory().getBean("rod", BeanFactory.class))
						.satisfies(ex -> {
							assertThat(ex.getBeanName()).isEqualTo("rod");
							assertThat(ex.getRequiredType()).isEqualTo(BeanFactory.class);
							assertThat(ex.getActualType()).isEqualTo(TestBean.class);
						});
	}

	@Test
	public void sharedInstancesAreEqual() {
		Object o = getBeanFactory().getBean("rod");
		boolean condition1 = o instanceof TestBean;
		assertThat(condition1).as("Rod bean1 is a TestBean").isTrue();
		Object o1 = getBeanFactory().getBean("rod");
		boolean condition = o1 instanceof TestBean;
		assertThat(condition).as("Rod bean2 is a TestBean").isTrue();
		assertThat(o == o1).as("Object equals applies").isTrue();
	}

	@Test
	public void prototypeInstancesAreIndependent() {
		TestBean tb1 = (TestBean) getBeanFactory().getBean("kathy");
		TestBean tb2 = (TestBean) getBeanFactory().getBean("kathy");
		assertThat(tb1 != tb2).as("ref equal DOES NOT apply").isTrue();
		assertThat(tb1.equals(tb2)).as("object equal true").isTrue();
		tb1.setAge(1);
		tb2.setAge(2);
		assertThat(tb1.getAge() == 1).as("1 age independent = 1").isTrue();
		assertThat(tb2.getAge() == 2).as("2 age independent = 2").isTrue();
		boolean condition = !tb1.equals(tb2);
		assertThat(condition).as("object equal now false").isTrue();
	}

	@Test
	public void notThere() {
		assertThat(getBeanFactory().containsBean("Mr Squiggle")).isFalse();
		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
						getBeanFactory().getBean("Mr Squiggle"));
	}

	@Test
	public void validEmpty() {
		Object o = getBeanFactory().getBean("validEmpty");
		boolean condition = o instanceof TestBean;
		assertThat(condition).as("validEmpty bean is a TestBean").isTrue();
		TestBean ve = (TestBean) o;
		assertThat(ve.getName() == null && ve.getAge() == 0 && ve.getSpouse() == null).as("Valid empty has defaults").isTrue();
	}

	@Test
	public void typeMismatch() {
		assertThatExceptionOfType(BeanCreationException.class)
						.isThrownBy(() -> getBeanFactory().getBean("typeMismatch"))
						.withCauseInstanceOf(TypeMismatchException.class);
	}

	@Test
	public void grandparentDefinitionFoundInBeanFactory() throws Exception {
		TestBean dad = (TestBean) getBeanFactory().getBean("father");
		assertThat(dad.getName().equals("Albert")).as("Dad has correct name").isTrue();
	}

	@Test
	public void factorySingleton() throws Exception {
		assertThat(getBeanFactory().isSingleton("&singletonFactory")).isTrue();
		assertThat(getBeanFactory().isSingleton("singletonFactory")).isTrue();
		TestBean tb = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertThat(tb.getName().equals(DummyFactory.SINGLETON_NAME)).as("Singleton from factory has correct name, not " + tb.getName()).isTrue();
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
		TestBean tb2 = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertThat(tb == tb2).as("Singleton references ==").isTrue();
		assertThat(factory.getBeanFactory() != null).as("FactoryBean is BeanFactoryAware").isTrue();
	}

	@Test
	public void factoryPrototype() throws Exception {
		assertThat(getBeanFactory().isSingleton("&prototypeFactory")).isTrue();
		assertThat(getBeanFactory().isSingleton("prototypeFactory")).isFalse();
		TestBean tb = (TestBean) getBeanFactory().getBean("prototypeFactory");
		boolean condition = !tb.getName().equals(DummyFactory.SINGLETON_NAME);
		assertThat(condition).isTrue();
		TestBean tb2 = (TestBean) getBeanFactory().getBean("prototypeFactory");
		assertThat(tb != tb2).as("Prototype references !=").isTrue();
	}

	/**
	 * Check that we can get the factory bean itself.
	 * This is only possible if we're dealing with a factory
	 */
	@Test
	public void getFactoryItself() throws Exception {
		assertThat(getBeanFactory().getBean("&singletonFactory")).isNotNull();
	}

	/**
	 * Check that afterPropertiesSet gets called on factory
	 */
	@Test
	public void factoryIsInitialized() throws Exception {
		TestBean tb = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertThat(tb).isNotNull();
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
		assertThat(factory.wasInitialized()).as("Factory was initialized because it implemented InitializingBean").isTrue();
	}

	/**
	 * It should be illegal to dereference a normal bean as a factory.
	 */
	@Test
	public void rejectsFactoryGetOnNormalBean() {
		assertThatExceptionOfType(BeanIsNotAFactoryException.class).isThrownBy(() ->
						getBeanFactory().getBean("&rod"));
	}

	// TODO: refactor in AbstractBeanFactory (tests for AbstractBeanFactory)
	// and rename this class
	@Test
	public void aliasing() {
		BeanFactory bf = getBeanFactory();
		if (!(bf instanceof ConfigurableBeanFactory cbf)) {
			return;
		}
		String alias = "rods alias";

		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
										cbf.getBean(alias))
						.satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo(alias));

		// Create alias
		cbf.registerAlias("rod", alias);
		Object rod = getBeanFactory().getBean("rod");
		Object aliasRod = getBeanFactory().getBean(alias);
		assertThat(rod == aliasRod).isTrue();
	}

	public static class TestBeanEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) {
			TestBean tb = new TestBean();
			StringTokenizer st = new StringTokenizer(text, "_");
			tb.setName(st.nextToken());
			tb.setAge(Integer.parseInt(st.nextToken()));
			setValue(tb);
		}
	}

}
