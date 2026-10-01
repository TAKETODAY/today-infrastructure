/* Copyright 2017 - 2026 the TODAY authors. */
'use strict'

const { test } = require('node:test')
const assert = require('node:assert/strict')
const { toProperties, register } = require('./configprops')

test('converts nested mappings, lists, scalar types and multiple documents', () => {
  assert.equal(toProperties('infra:\n  profiles:\n    include: [common, local]\n    validate: false\nport: 8080\nempty: null\n---\nname: prod'),
    'infra.profiles.include[0]=common\ninfra.profiles.include[1]=local\ninfra.profiles.validate=false\nport=8080\nempty=\n#---\nname=prod')
})

test('escapes Properties keys, multiline strings and backslashes', () => {
  assert.equal(toProperties('"a:b": " leading"\nmessage: "line1\\nline2"\npath: \'C:\\temp\''),
    'a\\:b=\\ leading\nmessage=line1\\nline2\npath=C:\\\\temp')
})

test('rejects invalid roots and cyclic aliases', () => {
  assert.throws(() => toProperties('- item'), /mapping/)
  assert.throws(() => toProperties('node: &node\n  child: *node'), /cyclic/)
})

test('renders both tabs with the Asciidoctor tabs extension', () => {
  const asciidoctor = require('@asciidoctor/core')()
  const registry = asciidoctor.Extensions.create()
  require('@asciidoctor/tabs').register(registry)
  register(registry)
  const html = asciidoctor.convert('[configprops,yaml]\n----\ninfra:\n  profiles:\n    validate: false\n----',
    { extension_registry: registry })
  assert.match(html, /class="tabpanel"/)
  assert.match(html, /language-yaml/)
  assert.match(html, /language-properties/)
  assert.match(html, /infra\.profiles\.validate=false/)
})
