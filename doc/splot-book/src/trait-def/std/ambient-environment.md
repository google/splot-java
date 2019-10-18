# AmbientEnvironment Trait (`aenv`)


Trait for measuring ambient temperature, pressure, humidity, and light level.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:ambient-environment:v1:v0#r0` |
| Short-Id | `aenv` |
| Has-Children | no |

 This would typically be used by environmental sensors.

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Pressure | `s/aenv/pres` | X |   |   | Ambient air pressure. *Units TBD*. |
| Temperature | `s/aenv/temp` | X |   |   | Ambient air temperature, in °C |
| Humidity | `s/aenv/humi` | X |   |   | Relative ambient humidity |
| LightLevel | `s/aenv/temp` | X |   |   | Ambient light level. *Units TBD*. |

### `s/aenv/pres` : Pressure

Ambient air pressure. *Units TBD*.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OBS`|



### `s/aenv/temp` : Temperature

Ambient air temperature, in °C.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OBS`|



### `s/aenv/humi` : Humidity

Relative ambient humidity.

| Attribute | Value |
|----:|-------------|
| Value Type | percentage (0.0-1.0) |
| Flags | `GET`, `OBS`|

Units TBD.

### `s/aenv/temp` : LightLevel

Ambient light level. *Units TBD*.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OBS`|



## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/aenv/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/aenv/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|
