SMCP Example Server
===================

This module implements an example SMCP server, hosting an
automation manager, a light bulb, and system info. It demonstrates
the following concepts:

 * How to set up a Technology instance
 * How to support non-volatile storage/retrieval of parameters using FilePersistentStateManager.
 * How to implement a Functional Endpoint (MyLightBulb, SystemInfo)
 * How to host functional endpoints on a Technology

## Building and Running ##

Building and running the example server is fairly simple:

    $ cd smcp-example-server
    $ mvn compile exec:java

Note that if you are not working from an official release, you may
need to install the HEAD-SNAPSHOT libraries to your local maven
repository first:

    $ cd ..
    $ mvn install

## Usage ##

Once the server is running, you will find it on the default CoAP
port 5683 on the local host. You can then interact with it using
any CoAP tool, such as [nyocictl](https://github.com/darconeous/libnyoci#nyocictl)
or [coap-client](http://manpages.ubuntu.com/manpages/cosmic/man5/coap-client.5.html).

It has the following structure:

 * `/g/`: Group interface. All groups live here.
    * `/g/?add[&ep=xxx]`: Create/add a new group, optionally specifying the group id.
    * `/g/...`: Created groups
 * `/1/`: Local Automation Manager.
    * `/1/f/pmgr?create`: Command to create a new pairing
    * `/1/f/pmgr/...`: Created pairings
    * `/1/f/rmgr?create`: Command to create a new rule
    * `/1/f/rmgr/...`: Created rules
    * `/1/f/tmgr?create`: Command to create a new timer
    * `/1/f/tmgr/...`: Created timers
 * `/2/`: Fake light bulb. Supports transitions and scenes.
    * `/2/s/onof/v`: On/off. Observable, writable.
    * `/2/s/levl/v`: Light level. 0.0-1.0. Observable, writable.
 * `/3/`: System info.
    * `/3/s/syst/load`: 1-minute load average. Observable.
    * `/3/m/syst/ncpu`: Number of available CPUs.
    * `/3/m/syst/sysn`: Name of operating system.
    * `/3/m/syst/sysv`: Version of operating system.

See the [automation introduction](../doc/automation.md) for more information
on how to use automation primitives.
