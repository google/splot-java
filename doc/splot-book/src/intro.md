# Introduction

An ongoing problem in the IoT industry is early unexpected
obsolescence: there are many documented cases where shortly after a
product’s release the manufacturer either got acquired, went out of
business, or simply decided to no longer maintain the infrastructure
required to support a product: resulting in all of the devices being
rendered effectively useless. In some cases this results in
significant financial burden if the company wants to maintain goodwill
in the brand by buying-back the useless devices. In all cases it
represents an additional electronic-waste disposal burden.

Even when the device is fully functional on a well-supported
ecosystem, end-users may want to use their devices in ways that the
manufacturer did not anticipate. For example, ZigBee Light-Link
remote-controls are only capable of controlling things that describe
themselves as ZigBee Light-Link lights—in general, you cannot
repurpose such a controller to instead adjust your television volume,
for example.

In much the same way that a manufacturer of power switches does not
specify what kind of device it can power on or off, *Splot* represents
an exploratory effort to improve the long-term value of IoT devices by
allowing them to be used in ways not limited by the imagination of
the device manufacturer or software developer. Stated more succinctly:

> IoT device software should not need to know anything specific about
> other IoT devices in order to be configured to usefully interact.

This document elaborates on the implications of this precept as
"Splot", along with all of the related pieces to turn it into a workable
collection of standards.

"Splot" is a Polish noun meaning "weave" or "tangle".
