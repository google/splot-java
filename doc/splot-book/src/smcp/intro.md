# Splot Monitoring and Control Protocol

The Splot Monitoring and Control Protocol (SMCP) is an experimental
RESTful machine-to-machine/thing-to-thing
protocol designed for monitoring and controlling networked devices, as
well as automating direct machine-to-machine interactions. SMCP can be
used via either CoAP or HTTP, although CoAP is recommended.

SMCP was designed simultaneously with the [Splot Object Model
(SOM)][SOM]. Thus, it shares much of the same
nomenclature: Things, Traits, Properties, and Methods
are all fundamental concepts in SMCP. However, whereas the Splot
Object Model defines the process and interfaces for how you can
discover, manipulate, and automate things, SMCP defines what that can
actually look like on the wire.

SMCP is still in the experimental and developmental stages, but SMCP
is ultimately intended to be used for monitoring and controlling a large
variety of device types:

*   Consumer IoT equipment: Temperature sensors, smart light
    bulbs, smart buttons, and network cameras.
*   Networking equipment: Smart switches, routers, and
    workstations.
*   Industrial equipment: Vehicle traffic signals, street lights, power
    distribution management.

[SOM]: ../som/intro.md
