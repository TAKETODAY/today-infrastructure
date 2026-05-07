yieldUnescaped '<!DOCTYPE html>'
html {
    head {
        meta('http-equiv':'"Content-Type", content="text/html; charset=utf-8"')
        title('Infra Framework Groovy Template Support')
    }
    body {
        div(class:'test') {
			h1("Hello $name")
			p("Groovy Templates")
        }
    }
}