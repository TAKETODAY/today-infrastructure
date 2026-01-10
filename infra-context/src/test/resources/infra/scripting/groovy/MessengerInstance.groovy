package infra.scripting.groovy;

import infra.scripting.Messenger

class GroovyMessenger implements Messenger {

	GroovyMessenger() {
		println "GroovyMessenger"
	}

	def String message;
}

return new GroovyMessenger();
