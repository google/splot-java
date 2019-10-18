# Design Goals

An ideal IoT application technology would satisfy the following goals:

*   Flexible enough to be considered relatively “future-proof”
*   Automatable without requiring extra hardware or internet access
*   Defined as an open standard with at least one open-source
    reference implementation
*   Simple enough that can be implemented on highly-constrained devices
*   Secure, honoring the principle of least privilege.
*   Suitable for residential, commercial, and industrial environments
*   Include proper support for groups, scenes, and smooth transitions (where
    appropriate)

Splot is an exploratory attempt to bridge these requirements into a single
cohesive technology and application protocol with the following goals:

*   Secure, low-latency monitoring and control of (potentially
    constrained) networked devices
*   Elegant support for scenes, groups/rooms, and smooth transitions
*   Expressive and reliable device-to-device automation

In addition satisfying these goals, the [SOM][] was designed, to the extent
practical, to express a superset of functionality commonly provided
by other existing IoT protocols, making it reasonably efficient to
monitor and control non-SOM-based using a SOM-based API. This
feature allows for the straightforward implementation of adaptation
layers and enables cross-protocol automation.

[SOM]: ./som/intro.md

<!--
One of the key design mandates of Splot is that IoT device software
should not have to know anything about other IoT devices in order to
be configured to usefully interact with them. Splot delegates that
job of to the *management application*.

For advanced users and professionals, Splot devices can be configured
manually (by hand) using the appropriate low-level utility applications.
However, most users will use a *management application* (either on a
smart phone or via the cloud) to commission, configure, monitor and
control Splot devices.
-->
