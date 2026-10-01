/* Copyright 2017 - 2026 the TODAY authors. */
'use strict'

const yaml = require('js-yaml')

function escapeProperty (value, key = false) {
  return String(value).replace(/\\/g, '\\\\').replace(/\r/g, '\\r').replace(/\n/g, '\\n')
    .replace(/\t/g, '\\t').replace(key ? /[ =:#!]/g : /^ /g, '\\$&')
}

function toProperties (source) {
  const lines = []
  yaml.loadAll(source, (document) => {
    if (document == null) return
    if (typeof document !== 'object' || Array.isArray(document)) {
      throw new Error('configprops YAML must contain a mapping at the document root')
    }
    if (lines.length) lines.push('#---')
    const ancestors = new Set()
    function flatten (value, path) {
      if (value !== null && typeof value === 'object') {
        if (ancestors.has(value)) throw new Error('configprops YAML cannot contain cyclic aliases')
        ancestors.add(value)
        for (const [key, child] of Object.entries(value)) {
          flatten(child, Array.isArray(value) ? `${path}[${key}]` : path ? `${path}.${key}` : key)
        }
        ancestors.delete(value)
      } else {
        lines.push(`${escapeProperty(path, true)}=${value == null ? '' : escapeProperty(value)}`)
      }
    }
    flatten(document, '')
  }, { schema: yaml.JSON_SCHEMA })
  return lines.join('\n')
}

function register (registry) {
  registry.block('configprops', function () {
    this.onContext('listing')
    this.parseContentAs('raw')
    this.process(function (parent, reader, attributes) {
      const language = attributes[2] || 'yaml'
      if (language !== 'yaml') throw new Error('configprops currently requires YAML input: [configprops,yaml]')
      const source = reader.getLines().join('\n')
      const properties = toProperties(source)
      const block = this.createBlock(parent, 'open', [], { ...attributes, style: undefined })
      this.parseContent(block, [
        '[tabs]', '======', 'YAML::', '+', '[source,yaml]', '----', source, '----',
        'Properties::', '+', '[source,properties]', '----', properties, '----', '======'
      ].join('\n').split('\n'))
      return block
    })
  })
}

module.exports = { register, toProperties }
