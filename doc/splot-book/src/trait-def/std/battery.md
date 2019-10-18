# Battery Trait (`batt`)


Battery trait for things which are backed by a battery.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:battery:v1:v0#r0` |
| Short-Id | `batt` |
| Has-Children | no |

 All of the properties in this trait are optional, but some properties have defined relationships with other properties that, if present, should be maintained.

Some Things may simply adopt this trait and implement none of the properties simply to indicate that it is battery-powered. Others might only want to indicate if the battery is low, but offer no additional information about the charge level or capacity.

On the other hand, some things might implement most of these properties, providing a rich amount of detail on the overall state and health of the battery.

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| ChargeRemaining | `s/batt/vpct` | X |   |   | Remaining battery charge, as a percentage. |
| EnergyRemaining | `s/batt/vnrg` | X |   |   | Energy remaining, in milliwatt-hours. |
| NeedsService | `s/batt/sreq` | X |   | X | Battery service-needed indicator. |
| ChargeState | `s/batt/stat` | X |   |   | Rechargable battery state |
| CapacityRemaining | `s/batt/rcap` | X |   |   | Capacity of the battery currently, relative to factory design capacity. |
| ChargeCycles | `s/batt/cycl` | X |   |   | Total number of battery recharge cycles. |
| CellVoltage | `s/batt/celV` | X |   |   | The voltages of the individual cells (or banks of cells) in the battery. |

### `s/batt/vpct` : ChargeRemaining

Remaining battery charge, as a percentage.

| Attribute | Value |
|----:|-------------|
| Value Type | percentage (0.0-1.0) |
| Flags | `GET`, `OBS`|

This field is optional, but highly recommended. If implemented, the value of this field is defined to be the following:

* Primary/Nonrechargable: `energyRemaining` divided by `energyCapacity`
* Secondary/Rechargeable: `energyRemaining` divided by the product of `capacityRemaining` and `energyCapacity`.

### `s/batt/vnrg` : EnergyRemaining

Energy remaining, in milliwatt-hours.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OBS`|

If implemented, the value of this field is defined to be the following:

* Primary/Nonrechargable: The product of `chargeRemaining` and `energyCapacity`,
* Secondary/Rechargeable: The product of `chargeRemaining` and `capacityRemaining` and `energyCapacity`.

### `s/batt/sreq` : NeedsService

Battery service-needed indicator.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `REQ`, `GET`, `OBS`|

True if the battery needs to be serviced, false otherwise. For example, this would become "true" if the battery was considered low.

### `s/batt/stat` : ChargeState

Rechargable battery state.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `GET`, `OBS`|

String describing the current state of the battery:

 * `charged`: Battery is fully charged. Connected to external power.
 * `charging`: Battery is currently charging from external power.
 * `discharging`: Battery is discharging normally.
 * `low`: Battery is discharging but little power remains.
 * `disconnected`: Battery has been disconnected, power being provided externally.
 * `trouble`: Something is wrong with the battery or charging system.

### `s/batt/rcap` : CapacityRemaining

Capacity of the battery currently, relative to factory design capacity.

| Attribute | Value |
|----:|-------------|
| Value Type | percentage (0.0-1.0) |
| Flags | `GET`, `OBS`|

Only used for rechargeable batteries. This is a ratio of the current maximum capacity of the battery versus the maximum capacity of the battery when it was new.

### `s/batt/cycl` : ChargeCycles

Total number of battery recharge cycles.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OBS`|

Only used for rechargeable batteries.

### `s/batt/celV` : CellVoltage

The voltages of the individual cells (or banks of cells) in the battery.

| Attribute | Value |
|----:|-------------|
| Value Type | array containing arrays containing real numbers |
| Flags | `GET`, `OBS`|

This can be used to determine the general health of the battery pack.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| EnergyCapacity | `m/batt/enrg` | X |   |   | The factory design energy capacity of the battery when new and fully charged, in milliwatt-hours. |
| NominalBatteryVoltage | `m/batt/volt` | X |   |   | The nominal voltage of the battery. |
| NominalCellVoltage | `m/batt/celV` | X |   |   | The nominal voltage of a cell in the battery. |
| CellCount | `m/batt/ccnt` | X |   |   | The number of cells in the battery. |
| Rechargable | `m/batt/rech` | X |   |   | Indicates if the battery is rechargeable or not. |

### `m/batt/enrg` : EnergyCapacity

The factory design energy capacity of the battery when new and fully charged, in milliwatt-hours.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `GET`|

The maximum energy capacity value is around 2 megawatt-hours.

### `m/batt/volt` : NominalBatteryVoltage

The nominal voltage of the battery.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`|



### `m/batt/celV` : NominalCellVoltage

The nominal voltage of a cell in the battery.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`|



### `m/batt/ccnt` : CellCount

The number of cells in the battery.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `GET`|



### `m/batt/rech` : Rechargable

Indicates if the battery is rechargeable or not.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `GET`|



## Associated Constants

| Name | Value | Description |
|-----|------|-------|
| `CHARGE_STATE_CHARGED` | "charged" | Battery is fully charged. Connected to external power. |
| `CHARGE_STATE_CHARGING` | "charging" | Battery is currently charging from external power. |
| `CHARGE_STATE_DISCHARGING` | "discharging" | Battery is discharging normally. |
| `CHARGE_STATE_LOW` | "low" | Battery is discharging but little power remains. |
| `CHARGE_STATE_DISCONNECTED` | "disconnected" | Battery has been disconnected, power being provided externally. |
| `CHARGE_STATE_TROUBLE` | "trouble" | Something is wrong with the battery or charging system. |
