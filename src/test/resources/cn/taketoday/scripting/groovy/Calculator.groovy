package cn.taketoday.scripting.groovy;

import cn.taketoday.scripting.Calculator

class GroovyCalculator implements Calculator {

	@Override
	int add(int x, int y) {
		return x + y;
	}

}
