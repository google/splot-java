# Standard Traits

The `standard` Splot traits are traits that represent common thing behaviors.
For example, lots of things have the ability to turn them on or off, so this
collection defines the [`OnOff`](on-off.md) trait.

 * [`OnOff`](./on-off.md): Trait for things that can be turned on or off.
 * [`Level`](./level.md): Trait for things that have a continuous value between 0% and 100%,
   such as lights, shades, or speed controllers.
 * [`Light`](./light.md): Trait that identifies lights, including full-color lights.
 * [`Enabled`](./enabled.md): Trait for things that can be enabled or disabled.
 * [`Energy`](./energy.md): Trait for things that need to describe, keep track of, or provide
   limits on energy consumption.
 * [`Battery`](./battery.md): Trait for things that have batteries/cells.
 * [`AmbientEnvironment`](./ambient-environment.md): Trait for things that monitor temperature,
   pressure, humidity, and/or light level.
