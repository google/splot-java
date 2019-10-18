# AccessRule Trait (`arul`)


Trait representing an access rule.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:access-rule:v1:v0#r0` |
| Short-Id | `arul` |
| Has-Children | no |
| Requires | `tag:google.com,2018:m2m:traits:enabled:v1:v0#r0`|

 This trait is a work in progress.

Rules are evaluated in order from lowest to highest. Once all rules have been evaluated (or when the last rule evaluated had the “done” flag set), the result of the evaluation is the value of the ‘deny’ flag of the last matching rule (or, if no matching rule, deny by default).

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Count | `s/arul/c` | X |   |   | The number of times this rule has been matched. |

### `s/arul/c` : Count

The number of times this rule has been matched.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `GET`, `RESET`, `OBS`, `VOLATILE`|

This count may be reset by setting it to zero. The count is not preserved across power cycles.

## Config Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Order | `c/arul/ordr` | X | X | X | The order in which this rule should be applied. |
| Path | `c/arul/path` | X | X | X | Path that this rule applies to. |
| Methods | `c/arul/meth` | X | X | X | REST Methods that this rule applies to. |
| ApplyIdentity | `c/arul/iden` | X | X |   | Identity to also apply access rules from. |
| Deny | `c/arul/deny` | X | X | X | The deny state to assume if this rule matches. |
| Done | `c/arul/done` | X | X | X | Stop evaluation if this rule matches. |

### `c/arul/ordr` : Order

The order in which this rule should be applied.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `REQ`, `RW`|

Rules are evaluated in order from lowest to highest.

### `c/arul/path` : Path

Path that this rule applies to.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `REQ`, `RW`|

Simple wildcards (`*`) are supported. Can also match query parameters. Path must be absolute unless starting with a wildcard or a question mark.

### `c/arul/meth` : Methods

REST Methods that this rule applies to.

| Attribute | Value |
|----:|-------------|
| Value Type | array containing text strings |
| Flags | `REQ`, `RW`|

Array can contain `GET`, `OBSERVE`, `POST`, `PUT`, or `DELETE`. Matches all methods if absent or empty.

### `c/arul/iden` : ApplyIdentity

Identity to also apply access rules from.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable text string |
| Flags | `RW`|

Assuming `path` and `meth` match, if this field is set the rules engine will execute the rules from another identity inline, as if they were a single rule. A stop flag encountered in that rule set will only stop evaluation within that rule set. This rule will then assume the deny status that results.

### `c/arul/deny` : Deny

The deny state to assume if this rule matches.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `REQ`, `RW`|

If the rule matches, changes the assumed deny state to this value. Behavior is different if “iden” is specified: it is used as the required resulting state from evaluating ‘iden’ if ‘done’ is set.

### `c/arul/done` : Done

Stop evaluation if this rule matches.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `REQ`, `RW`|

If the rule matches, stop evaluating. If `iden` is set, evaluation of the rules is only stopped if `deny` matches the result of evaluating `iden`.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/arul/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/arul/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|
