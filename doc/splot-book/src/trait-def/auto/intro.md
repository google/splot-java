# Automation Traits

The `automation` Splot traits are traits for implementing and managing
[automation primitives](../../automation/primitives.md).

 * [`Pairing`](./pairing.md): Trait for automation pairings, which are automation primitives
   that link the value of two different resources.
 * [`PairingManager`](./pairing-manager.md): Trait for things that create or delete
   automation pairings.
 * [`Actionable`](./actionable.md): Trait for automation primitives that can
   be triggered to perform specific actions. Implemented by Rules and Timers.
 * [`Rule`](./rule.md): Trait for automation rules, which are [actionable](./actionable.md)
   automation primitives that trigger once one or all of the configured conditions
   have been satisfied.
 * [`RuleManager`](./rule-manager.md): Trait for things that create or delete
    automation rules.
 * [`Timer`](./timer.md): Trait for automation timers, which are [actionable](./actionable.md)
   automation primitives that trigger after a delay, at regular intervals, or at specific
   times or dates.
 * [`TimerManager`](./timer-manager.md): Trait for things that create or delete
    automation timers.
