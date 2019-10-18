# Level Trait (`levl`)


Level.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:level:v1:v0#r0` |
| Short-Id | `levl` |
| Has-Children | no |

 This trait is implemented by things that support smooth transitions for some properties. Only properties in the *state* section can be transitioned smoothly.

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Value | `s/levl/v` | ? | ? | X | Level value as a percentage |

### `s/levl/v` : Value

Level value as a percentage.

| Attribute | Value |
|----:|-------------|
| Value Type | percentage (0.0-1.0) |
| Flags | `REQ`, `OPT_GET`, `OPT_SET`, `OBS`|

The level is encoded as a floating-point value between 0.0 and 1.0. The exact meaning of this value is dependent on the type of device, but in general the value 0.0 represents one extreme state, the value 1.0 represents the opposite extreme state, and the values between those two represent a perceptually uniform distribution between those two states. When paired with the `OnOff` trait, the value 0.0 is intended to be closest to the off state that isn't actually off.

Some things to note:

 * With the exception of physical actuators, perceptual uniformity is generally not linear. For example, reducing the light output by 50% will only reduce perceived light output by around 25%.
 * If this trait is paired with an OnOff trait, then setting the level to 0.0 will likely behave differently than if the OnOff trait was turned off.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/levl/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/levl/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|
