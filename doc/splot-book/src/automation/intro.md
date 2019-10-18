# Automation #

One of the key design goals for Splot was to support the use of
in-band-configurable automation primitives, enabling complex
device-to-device relationships that have no external dependencies once
configured.

Automation primitives can act like virtual wires, making
properties on different devices dependent on each other, or they can
act as scheduled timers that trigger actions at programmable times of
the week.

Additionally, since these automation primitives are defined
in terms of simple JSON/[CBOR][] values and REST-ful actions on URLs, they
can even be used to automate devices which have a RESTful interface
but don’t use the Splot Object Model.

[CBOR]: https://tools.ietf.org/html/rfc7049
