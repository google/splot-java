## Properties

Properties are the primary mechanism by which a thing can be monitored
and controlled. Each property has a value type associated with it. The
types are roughly those defined by [CBOR][]:

*   Number (Integer or Floating-Point)
*   Text String
*   Byte String
*   Boolean
*   Array
*   Map
*   URI-Reference

[CBOR]: https://tools.ietf.org/html/rfc7049

The actual value of a property may be one of these types or `null`,
the meaning of which is outlined by the trait that defines the
property.

Each property has an identifier, which is a short string (typically
four characters long) that uniquely identifies the property with its
scope.

### Sections ###

Sections organize properties in a thing based on their usage and
intended purpose. There are three sections:

*   **State** properties describe/control what the Thing is currently
    doing or recently done.
*   **Config** properties control how the Thing works.
*   **Metadata** properties describe what the Thing is and how it might
    behave, independently of what the current configuration or state
    is.

Properties in the state section are the only properties that can be
saved in a Scene or smoothly transitioned from one value to
another.

Each section is associated with a single character used to identify
the section:

*   "`s`": State
*   "`c`": Configuration
*   "`m`": Metadata

### Property Keys ###

Properties are uniquely identified by a tuple of the section,
trait-id, and property-id, which is called a *property key*.
Property keys are written as a string in the form of
`<section-id>/<trait-id>/<property-id>`; e.g.:

*   `s/onof/v`: On/off state value (boolean)
*   `s/levl/v`: Level value (float)
*   `c/enrg/mxam`: Maximum allowed amps (float)
*   `m/base/name`: Administratively assigned name (String)

A convention of using four or fewer letters has been adopted in order
to reduce packet sizes, but this is not a hard rule. Obviously longer,
more descriptive identifiers would improve casual comprehension, but
at the cost of wasted bytes.

Properties may be broadly described as being read-only, read-write,
or write-only, although there are additional attributes.

<!--
### Property Definition Attributes ##

Traits can further describe properties using the following set of
boolean attributes:

*   `REQ` : This property *MUST* be implemented for this trait.
*   `GET` : A getter is required if this property is implemented.
*   `OPT_GET` : A getter is optional if this property is implemented.
*   `NO_GET`: A getter is prohibited if this property is implemented.
*   `SET` : A setter is required if this property is implemented.
*   `OPT_SET` : A setter is optional if this property is implemented.
*   `NO_SET`: A setter is prohibited if this property is implemented.
    This is useful for integers representing enumerated values.
*   `RESET`: This property is reset (e.g. a counter) to the same value
    by any write.
*   `VOLATILE`: This property should never be stored and recovered
    from non-volatile memory.
*   `NO_TRANS` : This property should not be smoothly transitioned
    between values.
*   `NO_MUTATE`: This property should not allow itself to be mutated.

Additionally, some attributes are defined as combinations of the above
single attributes:

*   `RO` (read only): `GET` | `OBS` | `NO_SET`
*   `RW` (read/write): `GET` | `SET` | `OBS`
*   `WO` (write only): `SET` | `NO_MUTATE`
*   `CONST` (constant value): `GET` | `NO_SET`
*   `ENUM` (enumeration): `NO_TRANS` | `NO_MUTATE`

-->
