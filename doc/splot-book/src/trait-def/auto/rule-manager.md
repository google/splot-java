# AutomationRuleManager Trait (`rmgr`)


Experimental trait for managing automation rules.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:rule-manager:v1:v0#r0` |
| Short-Id | `rmgr` |
| Has-Children | yes |



## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/rmgr/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/rmgr/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



## Methods

| Key | Description |
|-----|-------------|
| `f/rmgr?create` | Creates a new automation rule. |

### `f/rmgr?create` : Create

Creates a new automation rule.

| Arg | Req | Returns | Description |
|-----|-----|---------|-------------|
| `actm` |  | text string | Method for a single action. |
| `en` |  | boolean | Determines if the created rule will be immediately enabled or not. |
| `name` |  | text string | Descriptive name for the created rule. |
| `recy` |  | boolean | Determines if the created pairing should be recyclable. |
| `mtch` |  | text string |  |
| `actb` |  | splot value | Body for a single action. |
| `actp` |  | URI-reference | Path for a single action. |
| `acti` |  | array containing maps of nullable splot values | List of Actions. |
| `cond` |  | array containing maps of nullable splot values | List of conditions |


Returns URI for the created rule.
