# Technologies #

A Technology is a type of object that enables a software program to
do one or more of the following fundamental operations:

*   Discover new things
*   Host things for other things/devices to use
*   Manage groups, automation primitives, and security contexts

A Technology instance could be thought of as a Splot-colored window
into the world of a specific IoT application protocol.

For example, using a SMCP-based technology would allow a software
program to discover, monitor, and control SMCP-based devices.
Likewise, using a ZigBee-based Technology instance will allow a
software program to discover, monitor and control ZigBee-based
devices, providing a SOM-compliant interface to the ZigBee world.

While not a fundamental SOM type, the concept of a Technology is
nonetheless an important concept to understand since groups,
automation primitives, security contexts, and UIDs are all
technology-scoped.

The Things discovered using a Technology instance and things hosted
by that Technology instance are in the same URL-space.

From a software perspective, a *Thing* is just an interface to an
object. Consider, for example, a fake on/off switch that implemented
the *thing* interface. It wouldn't need anything else to be usable
*within the process that created it*.

Things that implement automation primitives are different. They need
a window into the world around them because some of their properties
contain URLs that they need to monitor and/or change. Thus, automation
primitives cannot exist in a vacuum, instances are always be associated
with a technology instance. Specifically, they need a way to perform REST
operations on arbitrary URLs.

Group things are even more tightly related to technologies: they don't
ideally just exist on one device, they exist across all devices that
have things in them, and potentially many more.

Taking a thing that is discovered from one
technology instance and then hosting that on a separate technology
is called "bridging".



<!--
Things alone have no concept of a URL, either their own or how to
look-up the value of a resource that a URL might refer to.

A thing, created locally in a process, can be
monitored and controlled within the process that created it,
but the thing itself has no URL until it is *hosted*.

For programs where the intent isn't to host but to monitor and
control, some owning object instance that you
can use to discover other things.


The object
that hosts a thing so that it may be used by other devices is
called a *Technology*.

From the perspective of a software developer, a Technology
is a type of object that enables a software program to do
one or more of the following:

Whereas *things* are objects that you directly interact
with using a IoT protocol, a technology object represents the actual
implementation of that protocol.



In the SOM, technologies are considered separate from the things
that they provide or host. For example, Let's say we had a
technology instance (SmcpTechnology) which enables us to use
things that are hosted using SMCP on other devices as
well as enabling us to host our own things for other
devices to use.

To host a thing for other devices to use, you simply
command the technology to host a given thing instance.
Before that moment, the two objects were entirely unrelated. The key
point is that it doesn't matter where that thing
instance came from: it could be a locally created thing
or it could be a thing that was discovered by a
different technology.



The relationship between any given thing and a given
technology can be described by one of the following terms: *native*,
*hosted*, or *unrelated*:

*   A **native** thing is owned and managed entirely
    within a technology, and often represents functionality which is
    implemented on a different device.
*   A **hosted** thing is not owned or managed by the
    technology. Instead, the technology "hosts" the functionality of
    the thing for other devices to use.
*   An **unrelated** thing has no relationship with the
    technology. Such a thing cannot be used by the
    technology or participate in groups hosted by the technology.

If a thing is either hosted-by or native-to a
technology, it is said to be "associated" with that technology.

In most cases, a thing is described by only one of the
above three labels. However, things representing groups
are *always* native to a Technology, but are are often also hosted by
the same Technology. This is because groups logically exist on more
than one device, so they can be both native and (if any hosted
things are members) hosted.
-->
