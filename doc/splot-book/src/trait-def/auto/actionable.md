# Actionable Trait (`actn`)


Experimental trait for things that trigger actions, such as rules and timers.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:actionable:v1:v0#r0` |
| Short-Id | `actn` |
| Has-Children | no |



## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Count | `s/actn/c` | X |   |   | The number of times this thing has "fired". |
| Last | `s/actn/last` | X |   |   | The number of seconds ago that this thing last fired. |

### `s/actn/c` : Count

The number of times this thing has "fired".

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `GET`, `RESET`, `OBS`, `VOLATILE`|

This count may be reset by setting it to zero. The count is not preserved across power cycles.

### `s/actn/last` : Last

The number of seconds ago that this thing last fired.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `GET`, `NO_SET`, `OBS`, `VOLATILE`|

This value is not cacheable. Observing it will only indicate when the value is reset to zero.

## Config Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Actions | `c/actn/acti` | X | X | X | Actions to perform when this automation fires. |

### `c/actn/acti` : Actions

Actions to perform when this automation fires.

| Attribute | Value |
|----:|-------------|
| Value Type | array containing maps of nullable splot values |
| Flags | `REQ`, `GET`, `SET`|

Each criteria is defined as a map keyed by strings. The string keys are the following:

 * [`PARAM_ACTION_PATH`]: URL or absolute path to perform an action on
 * [`PARAM_ACTION_SKIP`]: True if this action should be skipped.
 * [`PARAM_ACTION_DESC`]: Human-readable description of the action
 * [`PARAM_ACTION_METHOD`]: The REST method to perform on the path
 * [`PARAM_ACTION_BODY`]: The body of the action
 * [`PARAM_ACTION_SYNC`]: If this action should complete before the next action

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/actn/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/actn/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



## Associated Constants

| Name | Value | Description |
|-----|------|-------|
| `PARAM_ACTION_PATH` | "p" | Path for action. |
| `PARAM_ACTION_METHOD` | "m" | The REST method to use for the action. |
| `PARAM_ACTION_BODY` | "b" | The body to use for the action. |
| `PARAM_ACTION_CONTENT_FORMAT` | "ct" | The [CoAP content-format](https://tools.ietf.org/html/rfc7252#section-12.3) to use for rendering the body when performing the action. |
| `PARAM_ACTION_SKIP` | "s" | Flag indicating if this action should be skipped. If absent, it is assumed to be false. |
| `PARAM_ACTION_DESC` | "desc" | Human readable description of the action |
| `PARAM_ACTION_SYNC` | "b" | Determines if this action should block execution or not. |
| `SYNC_DO_NOT_WAIT` | 0 | Value for [`PARAM_ACTION_SYNC`]: Trigger this action asynchronously. |
| `SYNC_WAIT_TO_FINISH` | 1 | Value for [`PARAM_ACTION_SYNC`]: Trigger this action synchronously. |
| `SYNC_STOP_ON_ERROR` | 2 | Value for [`PARAM_ACTION_SYNC`]: Trigger this action synchronously, stopping on error. |
