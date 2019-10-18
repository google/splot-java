# Button Trait (`bttn`)


A button that can be pressed.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:button:v1:v0#r0` |
| Short-Id | `bttn` |
| Has-Children | no |



## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Value | `s/bttn/v` | X | ? | X | Button State |
| PressCount | `s/bttn/c_dn` | X |   | X | The number of times this button has been *pressed*. |
| ReleaseCount | `s/bttn/c_up` | X |   | X | The number of times this button has been *released*. |
| Last | `s/bttn/last` | X |   |   | The number of seconds ago that this button was pressed or released. |

### `s/bttn/v` : Value

Button State.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `REQ`, `GET`, `OPT_SET`, `OBS`|

`true` while the button is pressed, `false` while the button is released.

### `s/bttn/c_dn` : PressCount

The number of times this button has been *pressed*.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `REQ`, `GET`, `RESET`, `OBS`|

This count may be reset by setting it to zero. The count is not preserved across power cycles.

### `s/bttn/c_up` : ReleaseCount

The number of times this button has been *released*.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `REQ`, `GET`, `RESET`, `OBS`|

This count may be reset by setting it to zero. The count is not preserved across power cycles.

### `s/bttn/last` : Last

The number of seconds ago that this button was pressed or released.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `GET`, `NO_SET`, `OBS`, `VOLATILE`|

This value is not cacheable. Observing it will only indicate when the value is reset to zero.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/bttn/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/bttn/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|
