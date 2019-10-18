# Scene Trait (`scen`)


Support for Scenes.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:scene:v1:v0#r0` |
| Short-Id | `scen` |
| Has-Children | yes |

 Provides scene support for a thing: named collections of state properties that can be easily recalled.

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| SceneId | `s/scen/sid` | X | X | X | Current scene identifier. |

### `s/scen/sid` : SceneId

Current scene identifier.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable text string |
| Flags | `REQ`, `RW`, `VOLATILE`|

When written to, applies the scene to the state. When read, it will return the last state that was loaded if no changes to the state have been made since that time. Otherwise reading will return null.

## Config Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| SceneIdPowerOn | `c/scen/spor` | X | X |   | Scene identifier to use at power-up. |

### `c/scen/spor` : SceneIdPowerOn

Scene identifier to use at power-up.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable text string |
| Flags | `EXPERIMENTAL`, `RW`|

If not null, this property identifies the scene that will be automatically loaded at power-on or reset. Not all implementations support changing this value.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/scen/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/scen/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



## Methods

| Key | Description |
|-----|-------------|
| `f/scen?save` | Saves the current state to the given SceneId. |

### `f/scen?save` : Save

Saves the current state to the given SceneId.

| Arg | Req | Returns | Description |
|-----|-----|---------|-------------|
| `sid` |  | text string | Scene ID |


Returns URI for the created scene.


