# OnOff Trait (`onof`)


On/Off.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:on_off:v1:v0#r0` |
| Short-Id | `onof` |
| Has-Children | no |

 Implemented by things that can be turned on or off, such as a light bulb or a power controller.

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Value | `s/onof/v` | ? | ? | X | On/Off state as a boolean |

### `s/onof/v` : Value

On/Off state as a boolean.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `REQ`, `OPT_GET`, `OPT_SET`, `OBS`|

On is `true`, off is `false`

## Config Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| DurationOff | `c/onof/doff` | X | ? |   | Default duration (in seconds) for transitions from 'on' to 'off' |
| DurationOn | `c/onof/don` | X | ? |   | Default duration (in seconds) for transitions from 'off' to 'on' |
| SceneIdOn | `c/onof/scon` | X | ? |   | Power-on scene. |
| IsLuminary | `c/onof/lumi` | X | ? |   | Flag for indicating if this thing is controlling a Luminary (light) |

### `c/onof/doff` : DurationOff

Default duration (in seconds) for transitions from 'on' to 'off'.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OPT_SET`, `OBS`|

Indicates the default duration (in seconds) when transitioning from the 'on' state to the 'off' state. This property is only present on things which also have the *Transition* trait.

### `c/onof/don` : DurationOn

Default duration (in seconds) for transitions from 'off' to 'on'.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `EXPERIMENTAL`, `GET`, `OPT_SET`, `OBS`|

Indicates the default duration (in seconds) when transitioning from the 'off' state to the 'on' state. This property is only present on things which also have the *Transition* trait.

### `c/onof/scon` : SceneIdOn

Power-on scene.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `EXPERIMENTAL`, `GET`, `OPT_SET`, `OBS`|

Indicates the scene to recall when the device is physically powered on or rebooted. On some types of devices this may be read-only. Only present on things that also implement the `Scene` trait.

### `c/onof/lumi` : IsLuminary

Flag for indicating if this thing is controlling a Luminary (light).

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `EXPERIMENTAL`, `GET`, `OPT_SET`, `OBS`|

This property allows a thing that controls a generic load (Like a smart power switch) to be explicitly identified as controlling a luminary (a light that is used for illumination). If this is set to `true`, this thing will be included in the “luminaries” group. If this thing implements the Luminary trait, then this thing is already assumed to be a luminary and this property **MUST NOT** be present.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/onof/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/onof/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|
