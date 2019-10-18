## Traits

Specific properties and methods are defined by *Traits*. A
*Thing* can implement properties and methods from several
traits. [Children](children.md) are also scoped to specific traits.

A Trait is a set of properties and methods that are related to a
purpose. For example, the [OnOff][] trait provides the concept of being
on or off, whereas the [Level][] trait provides the concept of a dimmer
switch or volume knob. These traits expose distinct behaviors that
are independent from the [Light][] trait which
provides additional properties for correlated color temperature and
sRGB values, but none that are suitable substitutes for the properties
provided by *OnOff* and *Level*: the Light trait is designed to be used
alongside these traits.

[OnOff]: ../trait-def/std/on-off.md
[Level]: ../trait-def/std/level.md
[Light]: ../trait-def/std/light.md

Some traits, like the [Scene][] trait, define children. In the case
of the Scene trait, each child *thing* represents a scene that can be
recalled, modified, or deleted. A "save" method is also provided by
the trait to allow the current state to be saved to a new or existing
scene.

[Scene]: ../trait-def/core/scene.md

Traits are scoped to be as general as practical so that they can be
applied to many different sorts of *things*. For example,
the [Level][] trait could be applied to a light bulb to control dimming,
or applied to a ceiling fan to control speed, or to a window
controller to control how far open the window is.

Each trait is defined to have both a human-readable name ([OnOff][],
[Level][], [Keychain][]), a short trait-id ("`onof`", "`levl`",
"`keyc`"), and a unique URI identifier.

[Keychain]: ../trait-def/sec/keychain.md

## Trait Definitions

Traits definitions are specified in a JSON format. This trait
definition can then be translated into either markdown documentation,
Rust, or Java using the `som-trait-conv` tool.

Splot provides the following collections of trait definitions:

* [*Core Traits*](../trait-def/core/intro.md) (`core`): Traits which are fundamental to the Splot Object Model.
* [*Standard Traits*](../trait-def/std/intro.md) (`std`): Traits which represent behaviors which are common
  enough to be standardized.
* [*Automation Traits*](../trait-def/auto/intro.md) (`auto`): Traits related to automation.
* [*Security Traits*](../trait-def/sec/intro.md) (`sec`): Traits related to the Splot Security Model.
