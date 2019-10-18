## Terminology

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
"SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this
document are to be interpreted as described in [RFC2119][].

[RFC2119]: https://tools.ietf.org/html/rfc2119

This specification makes use of the following terminology:

action
: A behavior triggered by some criteria that manipulates a resource
  in the configured way.

group
: A collection of *things* that can be controlled as though it
  were a single thing.

management application
: An application on a computer, smart-phone, or the cloud, that provides
  a user-friendly interface for monitoring, controlling, configuring, and
  automating Things.

method
: A named function/action that a *thing* can be directed to perform,
  optionally taking a set of named arguments and returning a value.
  Each method is defined by a *trait*.

property
: A named value associated with a *thing* that can be
  fetched, monitored and/or controlled. Each property is defined by
  a *trait*.

resource
: The underlying object identified by a URI.

section
: *Properties* are categorized into sections, similar to how one might
  categorize files into folders. Each property belongs to a single
  section, of which there are three: *state*, *config*, and *metadata*.

SMCP
: Abbreviation for Splot Monitoring and Control Protocol, an application
  protocol that is based on the SOM (Splot Object Model).

SOM
: Abbreviation for Splot Object Model.

technology
: *WRITEME*

thing
: An abstraction of a physical or virtual mechanism, sensor, or
  automation. Physical devices can host one or more *things*.

trait
: A named collection of related *property* and *method* definitions.
  *Things* implement properties and methods that are defined by traits.

trait-profile
: A named collection of *properties* and/or *methods* that a
  thing can advertise itself as implementing. These *properties* and
  *methods* can belong to one or more *traits*.

UID
: Unique ID. A short, administratively-assigned, opaque identifier that
  uniquely identifies a Thing in an administrative scope.