# Enabled Trait (`enab`)


Enabled/Disabled.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:enabled:v1:v0#r0` |
| Short-Id | `enab` |
| Has-Children | no |

 Implemented by things that can be configured as enabled or disabled.

## Config Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Value | `c/enab/v` | X | X | X | Enabled/Disabled state as a boolean |

### `c/enab/v` : Value

Enabled/Disabled state as a boolean.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `REQ`, `GET`, `SET`, `OBS`|

Enabled is `true`, Disabled is `false`

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/enab/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/enab/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|
