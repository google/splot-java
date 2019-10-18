# Transition Trait (`tran`)


Smooth value transitions.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:transition:v1:v0#r0` |
| Short-Id | `tran` |
| Has-Children | no |

 This trait is implemented by things that support smooth transitions for some properties. Only properties in the *state* section can be transitioned smoothly.

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Duration | `s/tran/d` | X | X | X | Transition duration, in seconds |
| Speed | `s/tran/sp` | X | X |   | Transition duration, in percentage of maximum speed |

### `s/tran/d` : Duration

Transition duration, in seconds.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable real number |
| Flags | `REQ`, `RW`, `VOLATILE`, `NO_TRANS`|

When updated simultaneously with other state changes, it indicates the duration of the transition between the old state and the specified state. When read it indicates the time remaining in the current transition. The current transition can be halted by setting this to zero. Sometimes physical limitations will force a minimum duration that is longer than specified. The maximum value that is required to be supported is 604800, or one week. The resolution must be at or below one tenth of a second for durations of less than one hour.

### `s/tran/sp` : Speed

Transition duration, in percentage of maximum speed.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable percentage (0.0-1.0) |
| Flags | `EXPERIMENTAL`, `RW`, `VOLATILE`, `NO_TRANS`|

This is an alternative to specifying the duration of a transition for Things where certain properties cannot be physically transitioned faster than a certain speed. The units of this property are a percentage of full speed. The implementation SHOULD allow this parameter to be adjusted as the transition is occurring. This property SHOULD NOT be implemented unless it makes sense for the underlying hardware.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/tran/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/tran/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|
