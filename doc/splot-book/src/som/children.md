## Children #

A trait's definition can indicate if it has child things,
and, if so, how those things behave and what
traits the children themselves implement.

For example, the scene trait allows a thing to save the
values of the properties in its state section and associating them
with a scene-id. The scene itself is represented as a child thing
that can be manipulated independently.

Each child thing can be identified by a combination of
the trait identifier and a child-id.

For example, if I used `f/scen?save` to create a scene with a scene-id
(child-id) of `evening`, the resulting child would be identified by
the tuple (`scen`, `evening`).

The exact mechanism for how you reference children is implementation
specific, but in [SMCP][] for example, it would be referenced as
`f/scen/evening/`, relative to the parent.

The nesting of child things is allowed.

[SMCP]: ./smcp/intro.md
