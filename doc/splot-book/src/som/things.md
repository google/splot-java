### Things

Things are the fundamental control surface objects in the SOM.
Physical devices host one or more Things.

Things provide:

*   *Properties* that can be monitored, changed, or mutated
*   *Methods* that take named arguments and return values
*   Child things (*children*) which it owns and manages

Some hypothetical examples of Things and their relationship to
physical devices:

*   A smart light bulb that hosts a single Thing that is used to
    control and monitor the state of the light bulb.
*   A smart power strip that hosts one Thing per outlet, used to
    control the state of each outlet and monitor power usage.
*   A proprietary wireless sensor gateway hosting one (or more) Things
    for each associated wireless sensor, used to monitor temperature
    and humidity.
*   A device could have any number of automation primitives, with each
    one being a thing.
