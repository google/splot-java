# AutomationPairing Trait (`pair`)


Automation Pairing.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:pairing:v1:v0#r0` |
| Short-Id | `pair` |
| Has-Children | no |

 An automation pairing allows you to create a relationship between two different properties, potentially on two different things.

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Count | `s/pair/c` | X |   |   | The number of times this pairing has "fired". |
| Last | `s/pair/last` | X |   |   | The number of seconds ago that this pairing last fired. |

### `s/pair/c` : Count

The number of times this pairing has "fired".

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `GET`, `RESET`, `OBS`, `VOLATILE`|

This count may be reset by setting it to zero. The count is not preserved across power cycles.

### `s/pair/last` : Last

The number of seconds ago that this pairing last fired.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `GET`, `NO_SET`, `OBS`, `VOLATILE`|

This value is not cacheable. Observing it will only indicate when the value is reset to zero.

## Config Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Source | `c/pair/src` | X | X | X | Source Resource URI-reference. |
| Destination | `c/pair/dst` | X | X | X | Destination Resource URI-reference. |
| EnableForward | `c/pair/efwd` | X | X | X | Enables changes to the source to be applied to the destination. |
| EnableReverse | `c/pair/erev` | X | X |   | Enables changes to the destination to be applied to the source. |
| ForwardTransform | `c/pair/xfwd` | X | X |   | Forward value transform |
| ReverseTransform | `c/pair/xrev` | X | X |   | Reverse value transform |

### `c/pair/src` : Source

Source Resource URI-reference.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `REQ`, `GET`, `SET`|

By convention, this is typically the local resource.

### `c/pair/dst` : Destination

Destination Resource URI-reference.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `REQ`, `GET`, `SET`|

By convention, this is typically the remote resource.

### `c/pair/efwd` : EnableForward

Enables changes to the source to be applied to the destination.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `REQ`, `GET`, `SET`|



### `c/pair/erev` : EnableReverse

Enables changes to the destination to be applied to the source.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `GET`, `SET`|



### `c/pair/xfwd` : ForwardTransform

Forward value transform.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `GET`, `SET`|

This string contains a simple RPN expression for modifying the numeric value of the source before applying it to the destination during forward value propagation. The value read from the source is the first item on the stack. The return value is the top-most value on the stack after evaluation. Thus, an empty forward transform is the identity function. If the stack is empty or the last pushed value is "DROP", the value does not propagate --- this behavior can be used to implement a predicate.

Example: The algebraic expression `x' = (cos(x/2 - 0.5) + 1)/2` would become `2 / 0.5 - COS 1 + 2 /`.

Note that `COS`/<i>cos()</i> takes *turns* instead of radians for its argument.

Example: `POP 0.1858 - SWAP POP 0.3320 - SWAP DROP SWAP / -449 3525 -6823.3 5520.33 POLY3` would convert CIE xy chromaticity coordinates in an array into an approximate correlated color temperature in Kelvin.

### `c/pair/xrev` : ReverseTransform

Reverse value transform.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `GET`, `SET`|

This string contains a simple RPN expression for modifying the numeric value of the destination before applying it to the source during reverse value propagation. The value read from the destination is the first item on the stack. The return value is the top-most value on the stack after evaluation. Thus, an empty reverse transform is the identity function. If the stack is empty or the last pushed value is "DROP", the value does not propagate --- this behavior can be used to implement a predicate.

Example: For the forward transform `2 *` (<i>x' = x * 2</i>), the correct reverse transform would be `2 /` (<i>x' = x/2</i>).

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/pair/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/pair/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



## Associated Constants

| Name | Value | Description |
|-----|------|-------|
| `TRAP_SOURCE_WRITE_FAIL` | "src-write-fail" | An attempt to write to the source resource has failed. |
| `TRAP_DESTINATION_WRITE_FAIL` | "dest-write-fail" | An attempt to write to the destination resource has failed. |
| `TRAP_SOURCE_READ_FAIL` | "src-read-fail" | An attempt to read from the source resource has failed. |
| `TRAP_DESTINATION_READ_FAIL` | "dest-read-fail" | An attempt to read from the source resource has failed. |
