# Splot Object Model

The Splot Object Model is a collection of interfaces for monitoring
and controlling things. It defines the general mechanisms and nomenclature
for how network-connected things can be described, configured, monitored,
and automated.

Keep in mind that while the SOM informs the design of Splot-based APIs
and protocols, it is only an object model: It defines vocabulary,
concepts, and object relationships. It is not an IoT application
protocol; it is a framework for describing and interacting with IoT
devices in a uniform way.

The SOM-native application protocol, [SMCP](../smcp/intro.md), is defined in a
separate chapter, but other IoT application protocols can be adapted
to be monitored, controlled, and automated in terms of the SOM.
