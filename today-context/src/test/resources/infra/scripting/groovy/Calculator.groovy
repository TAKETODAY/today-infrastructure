package infra.scripting.groovy;

import infra.scripting.Calculator

class GroovyCalculator implements Calculator {

	@Override
	int add(int x, int y) {
		return x + y;
	}

}
