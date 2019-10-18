# Base Trait (`base`)


Base trait required by all top-level things.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:base:v1:v0#r0` |
| Short-Id | `base` |
| Has-Children | no |

 It contains information about the model, manufacturer, and identifier, as well as administratively configurable properties like the administrative name, administrative id, and the hidden flag.

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Trap | `s/base/trap` | X | ? |   | Current trap condition(s) |

### `s/base/trap` : Trap

Current trap condition(s).

| Attribute | Value |
|----:|-------------|
| Value Type | nullable text string |
| Flags | `GET`, `OPT_SET`, `OBS`, `RESET`, `NO_TRANS`, `NO_MUTATE`|

When not null, contains a string indicating the current error/trap condition. This property will automatically revert to null when the error/trap condition has been cleared, which depends on the type of device. This is used to indicate things like power overload, door obstructed, manual override, or battery too low.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/base/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |
| Name | `m/base/name` | X | X | X | Administratively assigned human-readable name |
| UID | `m/base/uid` | X | ? |   | Administratively assigned unique identifier |
| Permanent | `m/base/perm` | X |   |   | Determines if this thing is permanent and cannot be removed. |
| ProductName | `m/base/prod` | X |   |   | Localized Product Name |
| Model | `m/base/modl` | X |   |   | Model identifier, unique to the manufacturer. |
| Manufacturer | `m/base/mfgr` | X |   |   | Manufacturer name |
| SoftwareVersion | `m/base/sver` | X |   |   | Software version |
| Serial | `m/base/seri` | X |   |   | Manufacturer unique-identifier or serial-number |
| TraitProfiles | `m/base/prof` | X |   |   | Trait profiles supported by this thing |
| Hidden | `m/base/hide` | X | X |   | Hidden flag |
| Context | `m/base/cntx` | X | X |   | Machine-readable dictionary containing information for the management applications. |

### `m/base/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



### `m/base/name` : Name

Administratively assigned human-readable name.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `REQ`, `RW`|

After a factory reset, this is set to a descriptive default value by the manufacturer. This value does not need to be unique.

### `m/base/uid` : UID

Administratively assigned unique identifier.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `GET`, `OPT_SET`, `OBS`|

Identifies the function of the thing. Rules may reference this identifier instead of a direct path in order to make replacement easier. After a factory reset, the value of this field is set to a random UID. This value must be unique.

### `m/base/perm` : Permanent

Determines if this thing is permanent and cannot be removed.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `CONST`|

True if this thing is permanent and cannot be deleted.

### `m/base/prod` : ProductName

Localized Product Name.

| Attribute | Value |
|----:|-------------|
| Value Type | map of text strings |
| Flags | `CONST`|

Contains a dictionary containing the localized names of the product in at least one language. The key to the dictionary is the locale code (like "en" or "jp"), and the value is the localized name for that locale.

### `m/base/modl` : Model

Model identifier, unique to the manufacturer.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `CONST`|

It identifies the specific model of the device hosting this thing. Note that this field is not for the marketing name: use ‘prod’ for that.

### `m/base/mfgr` : Manufacturer

Manufacturer name.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `CONST`|

Unique to the manufacturer. This property identifies the specific model of the device hosting this thing.

### `m/base/sver` : SoftwareVersion

Software version.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `CONST`|

This is often the version of the software running on the device, but in the case of a bridge may differ.

### `m/base/seri` : Serial

Manufacturer unique-identifier or serial-number.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `CONST`|

This is typically the thing index appended to the serial number of the device. The presence of this field is optional and may be omitted for privacy purposes.

### `m/base/prof` : TraitProfiles

Trait profiles supported by this thing.

| Attribute | Value |
|----:|-------------|
| Value Type | array containing text strings |
| Flags | `CONST`|

This property identifies which *trait profiles* this thing implements. Trait profiles define what the minimum implementation requirements are for specific types of functionality, such as lights.

Each trait profile specifies a unique string identifier (which is always a valid URI-reference) used to identify that specific version of the trait profile.

The first listed profile is intended to best describe the functionality of the thing, with subordinate profiles listed subsequently.

### `m/base/hide` : Hidden

Hidden flag.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `RW`|

This property is a simple boolean flag indicating if this thing should be hidden from administrative views.

### `m/base/cntx` : Context

Machine-readable dictionary containing information for the management applications.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `RW`|

This property is generally used by the management application to keep track of why certain things were created and how they fit into the bigger picture.

The following keys are specified:

* `rid`: Recipe ID. Contains the URI-Reference identifying the recipe that created this thing.
* `riid`: Recipe Instance ID. A opaque string identifying the recipe instance this thing belongs to.
* `ver`: The version of the recipe instance that this thing was created with. Any time a recipe instance is changed, the version number is incremented and all things owned by that instance are updated with the new version.
*`role`: The role this thing plays in the recipe. This opaque string is defined by the recipe.
