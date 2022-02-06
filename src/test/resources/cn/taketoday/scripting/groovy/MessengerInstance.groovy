package cn.taketoday.scripting.groovy;

import cn.taketoday.scripting.Messenger

class GroovyMessenger implements Messenger {

	GroovyMessenger() {
		println "GroovyMessenger"
	}

	def String message;
}

return new GroovyMessenger();
