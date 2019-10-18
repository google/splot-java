## Automation Recipes ##

It is not expected that end users would directly configure automation
primitives by hand---although they certainly could do that if they
wanted to. The expected way is that you would have applications (either
on your phone or in the cloud) that would configure and maintain these
automation pairings for you. Such applications can store some metadata
along with the automation primitives they are in charge of to help their
software make sense of them. There are also properties where such
applications are encouraged to include human-readable descriptions
and comments.

Recipes are instructions that enable management applications to
to create, manage, and update complex automation primitive chains.
They describe the complex relationships between concrete things and
automation-primitives required to implement arbitrary behaviors.


Examples of recipes might include:

 * Press a button and cause all lights in a room to slowly fade down to
   a value of 0% with a color temperature of ~2000 K over a period of
   two minutes before turning off entirely.
 * Use a motion sensor to automatically pause any ongoing lawn irrigation
   when motion is detected on the sidewalk, resuming 10 seconds after motion
   was last detected.
 * Use multiple motion sensors to control the lights in a room.
 * Automatically display "oven-on" notifications on smart displays near the
   kitchen when the oven is turned on, showing the current temperature in
   the notification and allowing the user to turn the oven off directly from
   the notification.

A recipe instance is a particular instantiation of a recipe. A recipe
can be used multiple times to create multiple recipe instances. For example,
you may want to use the "fading-down" recipe above in multiple rooms. In
such a case, each room would have one independent instance of the recipe.

The exact recipe specification remains TBD, but here are the general design goals:

 * Recipes are stored as JSON, in a format described by a JSON schema.
 * Recipe instances should require no local state on the management application
   that cannot be reconstructed by examining the metadata and configuration of the
   automation primitives that the recipe instance created. In other words,
   any automation primitive created by a recipe should contain enough information
   to identify what recipe created it, what part it plays in the recipe, and
   what logical recipe instance it is associated with.
 * Recipes should be upgradable and specify a logical way to transition an older
   recipe instance to a later version. Migration shouldn't be something users
   have to worry about.

Recipes will have the following structure:

*   A unique identifier
*   A version number which is incremented at each revision.
*   A list of prerequisite things, each having:
    *   An internal identifier string.
    *   A list of required metadata fields.
*   A list of configuration parameters which can be used to change the
    behavior of the recipe:
    *   An internal identifier string.
    *   The type of the parameter
        *   String
            *   Direct Entry (with input validation)
            *   Pulldown-box
        *   Numbers (Real/Integer)
            *   Direct Entry (with input validation)
            *   Slider in range
            *   Pulldown-box
        *   Color
        *   Angle
        *   Duration (in seconds)
        *   Schedule (one-off, repeating, etc)
        *   Boolean checkbox
    *   The default value of the parameter.
*   A list of created things, each having:
    *   An internal identifier string.
    *   A list of property values to set, each could be a:
        *   Constant value
        *   Value derived from prerequisite thing URI
        *   Value derived from recipe configuration
*   A list of version migration instructions, each having:
    *   Range of version numbers supported
    *   Mapping of changed parameters
    *   Mapping of changed prerequisite things
    *   Default values for new parameters (may be calculated
        dynamically)
