# Energy Trait (`enrg`)


Energy.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:energy:v1:v0#r0` |
| Short-Id | `enrg` |
| Has-Children | no |

 The Energy trait contains properties that relate to the energy consumption of a device. A Thing would rarely implement all of the described properties: only the relevant properties would be implemented.

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Watts | `s/enrg/watt` | X |   |   | Instantaneous power draw, measured in watts. |
| Amps | `s/enrg/amps` | X |   |   | Instantaneous power draw, measured in amps. |
| Volts | `s/enrg/volt` | X |   |   | Instantaneous electric potential, measured in volts. |
| VoltAmps | `s/enrg/voam` | X |   |   | Apparent instantaneous power draw, measured in volt-amps. |
| PowerFactor | `s/enrg/pwft` | X |   |   | The instantaneous measured power factor of the load. Unitless. |
| Energy | `s/enrg/enrg` | X |   |   | The accumulated power (energy) used over time by this thing, measured in watt-hours. |

### `s/enrg/watt` : Watts

Instantaneous power draw, measured in watts.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OBS`|

Unlike the property volt-amps, this property takes into consideration power factor when measuring AC.

### `s/enrg/amps` : Amps

Instantaneous power draw, measured in amps.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OBS`|



### `s/enrg/volt` : Volts

Instantaneous electric potential, measured in volts.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OBS`|



### `s/enrg/voam` : VoltAmps

Apparent instantaneous power draw, measured in volt-amps.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OBS`|

Note that this is literally the volts multiplied by the amps, so this will differ if the power factor is anything other than 1.0. Only really meaningful when measuring AC.

### `s/enrg/pwft` : PowerFactor

The instantaneous measured power factor of the load. Unitless.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OBS`|

Only meaningful when measuring AC.

### `s/enrg/enrg` : Energy

The accumulated power (energy) used over time by this thing, measured in watt-hours.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OBS`, `RESET`|

If this Thing allows this value to be reset, it can be reset by setting its value to zero or null. Setting to any other value MUST fail.

## Config Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| LimitMaxWatts | `c/enrg/mxwt` | X | ? |   | The maximum real power that the load is allowed to draw before being automatically shutting down. |
| LimitMaxVoltAmps | `c/enrg/mxva` | X | ? |   | The maximum apparent power (volt-amps) that the load is allowed to draw before being automatically shutting down. |
| LimitMaxVolts | `c/enrg/mxvo` | X | ? |   | The voltage above which the load is automatically shut down. |
| LimitMinVolts | `c/enrg/mnvo` | X | ? |   | The voltage below which the load is automatically shut down. |
| LimitMaxAmps | `c/enrg/mxam` | X | ? |   | The maximum current that the load is allowed to draw before the load is automatically shut down. |

### `c/enrg/mxwt` : LimitMaxWatts

The maximum real power that the load is allowed to draw before being automatically shutting down.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable real number |
| Flags | `GET`, `OPT_SET`, `OBS`|

Set to null to disable. This property requires that the OnOff trait also be supported. When tripped, `s/base/trap` is set to [`TRAP_MAX_WATTS`] until the condition is reset by turning the load on again.

### `c/enrg/mxva` : LimitMaxVoltAmps

The maximum apparent power (volt-amps) that the load is allowed to draw before being automatically shutting down.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable real number |
| Flags | `GET`, `OPT_SET`, `OBS`|

Set to null to disable. This property requires that the OnOff trait also be supported. When tripped, `s/base/trap` is set to [`TRAP_MAX_VOLT_AMPS`] until the condition is reset by turning the load on again.

### `c/enrg/mxvo` : LimitMaxVolts

The voltage above which the load is automatically shut down.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable real number |
| Flags | `GET`, `OPT_SET`, `OBS`|

Set to null to disable. This property requires that the OnOff trait also be supported. When tripped, `s/base/trap` is set to [`TRAP_MAX_VOLTS`] until the condition is reset by turning the load on again.

### `c/enrg/mnvo` : LimitMinVolts

The voltage below which the load is automatically shut down.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable real number |
| Flags | `GET`, `OPT_SET`, `OBS`|

Set to null to disable. This property requires that the OnOff trait also be supported. When tripped, `s/base/trap` is set to [`TRAP_MIN_VOLTS`] until the condition is reset by turning the load on again.

### `c/enrg/mxam` : LimitMaxAmps

The maximum current that the load is allowed to draw before the load is automatically shut down.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable real number |
| Flags | `GET`, `OPT_SET`, `OBS`|

Set to null to disable. This property requires that the OnOff trait also be supported. When tripped, `s/base/trap` is set to [`TRAP_MAX_AMPS`] until the condition is reset by turning the load on again.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/enrg/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |
| MaxWatts | `m/enrg/mxwt` | X |   |   | The maximum power that this thing is capable of drawing. |
| MaxAmps | `m/enrg/mxam` | X |   |   | The maximum current that this thing is capable of drawing. |

### `m/enrg/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



### `m/enrg/mxwt` : MaxWatts

The maximum power that this thing is capable of drawing.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `CONST`|



### `m/enrg/mxam` : MaxAmps

The maximum current that this thing is capable of drawing.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `CONST`|



## Associated Constants

| Name | Value | Description |
|-----|------|-------|
| `TRAP_MAX_WATTS` | "energy-max-watts" | The real power of the load exceeded the value specified by `c/enrg/mxwt`. |
| `TRAP_MAX_VOLT_AMPS` | "energy-max-volt-amps" | The apparent power of the load exceeded the value specified by `c/enrg/mxva`. |
| `TRAP_MAX_VOLTS` | "energy-max-volts" | The voltage exceeded the value specified by `c/enrg/mxvo`. |
| `TRAP_MIN_VOLTS` | "energy-min-volts" | The voltage was lower than the value specified by `c/enrg/mnvo`. |
| `TRAP_MAX_AMPS` | "energy-max-amps" | The current being drawn by the load exceeded the value specified by `c/enrg/mxam`. |
