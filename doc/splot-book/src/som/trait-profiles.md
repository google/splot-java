## Trait Profiles

A trait profile is a named, versioned set of required-to-implement
traits and an associated list of required-to-implement properties and
methods from those traits. When a thing indicates that it supports a
given trait profile, it is promising that it fully supports all of the
properties and methods that the trait profile requires.

Trait profiles provide a way for software to quickly classify the
functionality of a thing and ensure that certain types of things have
the appropriate minimal level of common functionality to ensure they
meet user expectations.

For example, a trait profile for a "Dimmable Lamp" might require the
[`Base`][], [`OnOff`][], [`Level`][], [`Energy`][], and [`Light`][] traits, additionally
requiring some additional subset of the optional properties from those
traits to be implemented. The trait profile for a "Full-Color Lamp"
would require the exact same traits but also include an expanded list
of required-to-implement properties from those traits. A thing can
implement more than one trait profile, and some trait profiles require
that other specific trait profiles also be supported.

[`Base`]: ../trait-def/core/base.md
[`OnOff`]: ../trait-def/std/on-off.md
[`Level`]: ../trait-def/std/level.md
[`Energy`]: ../trait-def/std/energy.md
[`Light`]: ../trait-def/std/light.md
