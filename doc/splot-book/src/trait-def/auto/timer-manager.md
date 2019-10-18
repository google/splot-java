# AutomationTimerManager Trait (`tmgr`)


Experimental trait for managing automation timers.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:timer-manager:v1:v0#r0` |
| Short-Id | `tmgr` |
| Has-Children | yes |



## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/tmgr/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/tmgr/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



## Methods

| Key | Description |
|-----|-------------|
| `f/tmgr?create` | Creates a new automation timer. |

### `f/tmgr?create` : Create

Creates a new automation timer.

| Arg | Req | Returns | Description |
|-----|-----|---------|-------------|
| `pred` |  | text string | Predicate program. |
| `name` |  | text string | Descriptive name for the created timer. |
| `recy` |  | boolean | Determines if the created timer should be recyclable. |
| `acti` |  | array containing maps of nullable splot values | List of Actions. |
| `actm` |  | text string | Method for a single action. |
| `mtch` |  | text string |  |
| `actp` |  | URI-reference | Path for a single action. |
| `en` |  | boolean | Determines if the created timer will be immediately enabled or not. |
| `actb` |  | splot value | Body for a single action. |
| `adel` |  | boolean | Auto delete flag. |
| `dura` |  | real number | Duration of the timer. |
| `arst` |  | boolean | Auto reset flag. |
| `schd` |  | text string | Schedule Program. |


Returns URI for the created timer.
