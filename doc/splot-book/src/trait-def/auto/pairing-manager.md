# AutomationPairingManager Trait (`pmgr`)


Experimental trait for managing automation pairings.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:pairing-manager:v1:v0#r0` |
| Short-Id | `pmgr` |
| Has-Children | yes |



## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/pmgr/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/pmgr/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



## Methods

| Key | Description |
|-----|-------------|
| `f/pmgr?create` | Creates a new automation pairing. |

### `f/pmgr?create` : Create

Creates a new automation pairing.

| Arg | Req | Returns | Description |
|-----|-----|---------|-------------|
| `xfwd` |  | text string | Maps to `c/pair/xfwd` |
| `dst` | X | URI-reference | Maps to `c/pair/dst` |
| `erev` |  | boolean | Maps to `c/pair/erev` |
| `en` |  | boolean | Maps to `c/enab/v` |
| `src` | X | URI-reference | Maps to `c/pair/src` |
| `name` |  | text string | Maps to `m/base/name` |
| `xrev` |  | text string | Maps to `c/pair/xrev` |
| `efwd` |  | boolean | Maps to `c/pair/efwd` |


Returns URI for the created pairing.
