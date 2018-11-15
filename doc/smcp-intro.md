Introduction to the Simple Monitoring and Control Protocol (SMCP)
=================================================================

SMCP is an experimental RESTful machine-to-machine/thing-to-thing
protocol designed for monitoring and controlling networked devices, as
well as automating direct machine-to-machine interactions. SMCP can be
used via either CoAP or HTTP, although CoAP is recommended.

SMCP was designed simultaneously with the [Splot Object Model
(SOM)](splot-object-model-intro.md). Thus, it shares much of the same
nomenclature: Functional Endpoints, Traits, Properties, and Methods
are all fundamental concepts in SMCP. However, whereas the Splot
Object Model defines the process and interfaces for how you can
discover, manipulate, and automate things, SMCP defines what that can
actually look like on the wire.

SMCP is still in the experimental and developmental stages, but SMCP
is ultimately being designed to be able to monitor and control a large
variety of devices:

*   Consumer IoT equipment: like temperature sensors, smart light
    bulbs, smart buttons, and network cameras.
*   Networking equipment: like smart switches, routers, and
    workstations.
*   Industrial equipment: like traffic signals, street lights, power
    distribution management.

## Introductory Example ##

This section introduces a strawman device and describes how it would
be used in practice. It has been written in such a way as to allow you
to jump right in without much context on how the SOM works, but I do
recommend having a look at the [Splot Object Model
Introduction](splot-object-model-intro.md).

Since SMCP is a RESTful protocol, it uses a hierarchy reminiscent o
that of a file system. Here is an example of what the hierarchy might
look like for a simple smart light bulb:

     * /.well-known/
        * core                  <- RFC6690, for service discovery
     * /1/                      <- Feature Functional Endpoint #1
        * s/                    <- State Section
            * onof/v            <- OnOff value (Boolean)
            * levl/v            <- Level value (Float)
            * scen/sid          <- Scene ID
            * tran/d            <- Transition Duration
        * c/                    <- Configuration Section
            * lock/v            <- Change Lock (Boolean)
        * m/                    <- Metadata Section
            * base/turi         <- Base Trait Version URI
            * base/name         <- Administrative Name for this FE
            * base/uid          <- Administrative UID for this FE
            * base/sver         <- Software version
            * onof/turi         <- OnOff Trait Version URI
            * lock/turi         <- Lock Trait Version URI
            * levl/turi         <- Level Trait Version URI
            * lght/turi         <- Light Trait Version URI
            * lght/mire         <- Native Correlated Color Temperature
            * lght/mxbr         <- Maximum Lumen Output
            * lght/mdim         <- Minimum Dimming Output
            * scen/turi         <- Scene Version URI
            * tran/turi         <- Transition Version URI
        * f/                    <- Methods/Children Section
            * scen?save         <- Scene Trait Save Method
            * scen/[scene-id]/  <- Individual Scene FE
                * s/onof/v      <- OnOff value for [scene-id]
                * s/levl/v      <- Level value for [scene-id]
                * s/tran/d      <- Transition duration for [scene-id]
                * m/base/name   <- Administrative name of the scene

### Example REST Operations ###

Note that while the following examples use JSON, CBOR is the preferred
encoding for SMCP. Support for JSON in SMCP is optional.

To get information about the state of this functional endpoint:

    GET /1/s

    ...

    2.05 CONTENT
    {"onof":{"v":false},"levl":{"v":0.2},"tran":{"d":0}}

To get information on just the on/off status:

    GET /1/s/onof/v

    ...

    2.05 CONTENT
    false

To turn on this light to full brightness (assuming it isn't locked):

    POST /1/s
    {"onof":{"v":true},"levl":{"v":1}}

    ...

    2.04 CHANGED

To toggle this light on or off:

    POST /1/s/onof/v?tog

    ...

    2.04 CHANGED

To increase the perceived brightness of the light by around 10% of
maximum:

    POST /1/s/levl/v?inc
    0.1

    ...

    2.04 CHANGED

Change the light level to 100% brightness over the course of three
seconds:

    POST /1/s
    {"levl":{"v":1},"tran":{"d":3}}

    ...

    2.04 CHANGED

Significantly decrease the brightness of the light (by 0.5) over the
course of 10 seconds:

    POST /1/s/levl/v?inc&d=10
    -0.5

    ...

    2.04 CHANGED

To stop a transition currently in progress:

    POST /1/s/tran/d
    0

    ...

    2.04 CHANGED

To save the current state as a scene identified as 'evening':

    POST /1/f/scen?save
    {"sid":"evening"}

    ...

    2.01 CREATED /1/f/scen/evening/

To later see what the state for the evening scene is:

    GET /1/f/scen/evening/s

    ...

    2.05 CONTENT
    {"onof":{"v":true},"levl":{"v":0.3}}

To recall evening scene state:

    POST /1/s/scen/sid
    "evening"

    ...

    2.04 CHANGED

To delete the evening scene:

    DELETE /1/f/scen/evening/

    ...

    2.02 DELETED

### Additional Bits ###

Now that we've covered some of the more simple sorts of interactions,
we can add on to the hierarchy to add things like group support,
device management, keychain management, etc:

     * /g/                   <- Group directory
        * ?create            <- Query for creating a new group
        * [group-id]/        <- Individual Group Functional Endpoints
            * s/...          <- Group Member State (changes state of all members)
            * c/...          <- Group Configuration (normal section)
            * m/...          <- Group Metadata (normal section)
            * f/...          <- Group Member Functions/Children (similar to state)
     * /dev/                 <- Device Management Functional Endpoint
        * m/                 <- Metadata Section
            * base/turi      <- Base Trait Version URI
            * base/name      <- Administrative Name for this FE
            * base/uid       <- Administrative UID for this FE
            * base/sver      <- Software version
            * prmg/turi      <- Automation Pairing Manager Version URI
            * rlmg/turi      <- Automation Rule Manager Version URI
            * keyc/turi      <- Keychain Version URI
            * gmgr/turi      <- Group Manager Version URI
        * f/                 <- Methods/Children Section
            * prmg/
                * ?create    <- Query to create automation pairings
                * [pair-id]/ <- Path to the FEs for individual pairings
            * rlmg/
                * ?create    <- Query to create automation rules
                * [rule-id]/ <- Path to the FEs for individual rules
            * keyc/
                * ?create    <- Query to create new keychain items
                * [key-id]/  <- Path to the FEs for individual keychain items
