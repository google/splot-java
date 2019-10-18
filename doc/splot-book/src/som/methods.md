## Methods

Methods are a way to manipulate a thing in ways that would not
translate well to being represented with properties. The most common
example is for creating new child things, but the mechanism is defined
to be open-ended.

Methods take a named set of arguments as input and are defined to
output a single value or indicate an error. Arguments are identified
by a short string rather than by their order. Arguments and return
values share the same set of value types that are used by properties,
with the addition that an method may return a direct reference to a
child thing. (This is used to return created or updated child things).

Methods are defined by traits just like properties are. The trait
defines what the named arguments are, what their types should be,
which arguments are required, and what the method return type will be.

Just like properties, methods are scoped by trait. They have a special
section designated "`f`"(This scope gets double-duty in SMCP with also
handling child things, but I digress). The short-identifiers of the
scope, trait, and method are combined similarly to properties, except
that the second slash is instead a question mark:

*   `f/scen?save`: Save current state to a new or existing scene
*   `f/gmgr?create`: Create a new group

<!--

## Method Definition Attributes ##

Like properties, traits can also specify attributes as a part of the
method definition. However, there are relatively few:

*   `REQ`: This method is required to be implemented.
*   `GID`: If invoked via a group, add the group id to the arguments.

-->