# Presence Trait (`pres`)


Detects presence.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:presence:v1:v0#r0` |
| Short-Id | `pres` |
| Has-Children | no |

 Implemented by things that can detect the presence of someone, such as motion sensors and pressure mats.

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Value | `s/pres/v` | X | ? | X | Presence Detected |
| Count | `s/pres/c` | X |   | X | Trip Count |
| Last | `s/pres/last` | X |   |   | The number of seconds ago that presence was last detected. |

### `s/pres/v` : Value

Presence Detected.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `REQ`, `GET`, `OPT_SET`, `OBS`|

`true` while presence is detected, `false` otherwise.

The presence signal is generally momentary in nature, flipping back and forth between detected and not-detected as the sensor detects movement.

### `s/pres/c` : Count

Trip Count.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `REQ`, `GET`, `RESET`, `OBS`|

This count may be reset by setting it to zero. The count is not preserved across power cycles.

### `s/pres/last` : Last

The number of seconds ago that presence was last detected.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `GET`, `NO_SET`, `OBS`, `VOLATILE`|

This value is not cacheable. Observing it will only indicate when the value is reset to zero.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/pres/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/pres/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|
