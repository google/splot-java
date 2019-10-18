# Light Trait (`lght`)


Trait for things that are used for illumination.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:light:v1:v0#r0` |
| Short-Id | `lght` |
| Has-Children | no |

 This trait is typically paired with the OnOff and Level *traits*.

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Mode | `s/lght/mode` | X | ? |   | Current color system mode |
| Effect | `s/lght/efct` | X | ? |   | Identifies the current special effect(s) in progress |
| Mireds | `s/lght/mire` | X | ? |   | Current color temperature, in Mireds |
| ChromaXy | `s/lght/chro` | X | ? |   | The ‘x’ and ‘y’ CIE chromaticity coordinates for the current color |
| Whitepoint | `s/lght/whpt` | X | ? |   | The ‘x’ and ‘y’ CIE chromaticity coordinates for the current whitepoint |
| sRGB | `s/lght/sRGB` | X | ? |   | sRGB values normalized to the range 0.0 to 1.0 |

### `s/lght/mode` : Mode

Current color system mode.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `GET`, `OPT_SET`|

Indicates the last color system used.

Can be one of the following values:

 * `ct`: Color Temperature
 * `sRGB`
 * `xy`: CIE xy
 * `LCh`: CIE LCh (**EXPERIMENTAL**)

This property is **REQUIRED** if the other implemented properties have side effects that would set this property to more than one specific value.

Internally, this property is used to determine which properties need to be stored in order to be able to properly recall the state later on, among other things.

### `s/lght/efct` : Effect

Identifies the current special effect(s) in progress.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable text string |
| Flags | `GET`, `OPT_SET`, `OBS`|

The interpretation of this field is vendor-specific, with the following exceptions: `null` indicates no effect is in use, `"colorcycle"` indicates that the hue of the light is cycling through colors, and `"candle"` indicates that the light is emulating the flicker of a candle.

Vendors MAY implement other effects with unique string identifiers. If this property is implemented, `m/lght/efct` **MUST** also be implemented.

### `s/lght/mire` : Mireds

Current color temperature, in Mireds.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`, `OPT_SET`, `OBS`|

<a href="https://en.wikipedia.org/wiki/Mired">Mireds</a> are a perceptually-uniform way to indicate color temperature. This is an important property to ensure that transitions appear to be smooth and natural.

You can easily convert from Kelvin to Mireds using the following formula:

~~~ ignore
M = 1000000 / K
~~~

Because of the reciprocal relationship between Mireds and Kelvin, the exact same formula also works for converting Mireds to Kelvin.

Implementing this property is **REQUIRED** on color-temperature lights and **RECOMMENDED** on full-color lights.

When this property is written to, the following changes occur:

 * The light is configured to emit "white" light at the given color temperature.
 * `s/lght/mode` (if implemented) changes to `"ct"`
 * `s/lght/whpt` (if implemented) is updated to reflect the new value
 * `s/lght/CIEC` becomes zero.

If `s/lght/mode` is set to `"ct"`, reading this property will yield the current color temperature of the white light that is being emitted. Otherwise, reading this property should yield either the *correlated* color temperature of the light that is being emitted or `null`.

### `s/lght/chro` : ChromaXy

The ‘x’ and ‘y’ CIE chromaticity coordinates for the current color.

| Attribute | Value |
|----:|-------------|
| Value Type | array containing real numbers |
| Flags | `GET`, `OPT_SET`, `OBS`|

This property is **REQUIRED** on full-color lights and **OPTIONAL** on color-temperature lights.

If the values written to this property are out of gamut, then the state when read will reflect an in-gamut approximation.

Writing to this property changes `s/lght/mode` to `xy`.

### `s/lght/whpt` : Whitepoint

The ‘x’ and ‘y’ CIE chromaticity coordinates for the current whitepoint.

| Attribute | Value |
|----:|-------------|
| Value Type | array containing real numbers |
| Flags | `EXPERIMENTAL`, `GET`, `OPT_SET`, `OBS`|

This property is **REQUIRED** on full-color lights and **OPTIONAL** on color-temperature lights.

If the values written to this property are out of gamut, then the state when read will reflect an in-gamut approximation.

Writing to this property changes `s/lght/mode` to `xy`.

### `s/lght/sRGB` : sRGB

sRGB values normalized to the range 0.0 to 1.0.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable array containing percentages |
| Flags | `EXPERIMENTAL`, `GET`, `OPT_SET`, `OBS`|

Uses the real composite sRGB gamma curve. Setting this value will change the color of the light to match the contained values.

Implementing this property is **RECOMMENDED** on full-color lights.

If `s/lght/whpt` is supported, use that as the reference whitepoint, otherwise use D65.

The read value is NOT cropped to fit into the gamut of the device, but individual values **MAY** be limited to the range of 0.0-1.0 or reflect the loss of precision from conversion to the internal representations. If the gamut of the device is larger than the sRGB gamut, then values outside of the range of 0.0-1.0 **MAY** be allowed.

The value read SHOULD read as `null` if `s/lght/mode` is not `sRGB`, otherwise it reports the last value written.

Setting a value of all zeros MUST cause `s/onof/v` to become false. Setting a value with any non-zero component **MUST** cause the `s/onof/v` to become true.

Writing to this property changes `s/lght/mode` to `sRGB`.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/lght/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |
| NativeMireds | `m/lght/mire` | X |   |   | The native correlated color temperature (in Mireds) |
| MaxMireds | `m/lght/mxct` | X |   |   | The *maximum* numerical value for `s/lght/mire` that this light supports. |
| MinMireds | `m/lght/mnct` | X |   |   | The *minimum* numerical value for `s/lght/mire` that this light supports. |
| SupportedEffects | `m/lght/efct` | X |   |   | Provides a list of supported special effects |
| Primaries | `m/lght/prim` | X |   |   | An array describing the primaries used on the light (up to six) in the CIE xyY colorspace. |
| Orientation | `m/lght/ornt` | X | ? |   | The physical orientation of the light. |
| Function | `m/lght/func` | X | ? |   | Identifies the primary function of the light. |

### `m/lght/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



### `m/lght/mire` : NativeMireds

The native correlated color temperature (in Mireds).

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `CONST`|

This **SHOULD** be present on monochromatic lights, but **MAY** be present on full-color lights. In the case of full-color lights, this property may be missing entirely or set to represent the specific value of `s/lght/mire` that would generate the largest possible light output.

### `m/lght/mxct` : MaxMireds

The *maximum* numerical value for `s/lght/mire` that this light supports.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `CONST`|



### `m/lght/mnct` : MinMireds

The *minimum* numerical value for `s/lght/mire` that this light supports.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `CONST`|



### `m/lght/efct` : SupportedEffects

Provides a list of supported special effects.

| Attribute | Value |
|----:|-------------|
| Value Type | array containing text strings |
| Flags | `CONST`|



### `m/lght/prim` : Primaries

An array describing the primaries used on the light (up to six) in the CIE xyY colorspace.

| Attribute | Value |
|----:|-------------|
| Value Type | array containing arrays containing real numbers |
| Flags | `CONST`|

Each primary is described by a three-element array that contains the x, y, and Y values respectively for the primary. The Y component is normalized to where the maximum brightness of the light is Y=1.0.

### `m/lght/ornt` : Orientation

The physical orientation of the light.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `GET`, `OPT_SET`|

0: unspecified, 1: omnidirectional, 2: downlight, 3:uplight, 4: sidelight

### `m/lght/func` : Function

Identifies the primary function of the light.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `GET`, `OPT_SET`|

0: unspecified, 1: functional/illuminative, 2: decorative, 3: informative

## Associated Constants

| Name | Value | Description |
|-----|------|-------|
| `EFFECT_COLORCYCLE` | "colorcycle" | Color-cycle special effect. |
| `EFFECT_CANDLE` | "candle" | Candle special effect. |
| `MODE_COLOR_TEMP` | "ct" | Color temperature light mode. |
| `MODE_SRGB` | "sRGB" | sRGB light mode. |
| `MODE_CIE_XY` | "xy" | CIE xy light mode. |
| `MODE_CIE_LCH` | "LCh" | CIE LCh light mode. |
| `ORIENTATION_UNSPECIFIED` | 0 | The orientation of this light is unspecified. |
| `ORIENTATION_OMNIDIRECTIONAL` | 1 | This light illuminates in all directions. |
| `ORIENTATION_DOWNLIGHT` | 2 | This light illuminates downward. |
| `ORIENTATION_UPLIGHT` | 3 | This light illuminates upward. |
| `ORIENTATION_SIDELIGHT` | 4 | This light illuminates to the side. |
| `FUNCTION_UNSPECIFIED` | 0 |  |
| `FUNCTION_ILLUMINATIVE` | 1 | The primary purpose of this light is illumination. |
| `FUNCTION_DECORATIVE` | 2 | This light is decorative. |
| `FUNCTION_INFORMATIVE` | 3 | The state of this light helps to inform the viewer. |
