## Groups ##

A group is a collection of things (members) which can be
controlled as a single thing.

Groups are always owned by a [technology](technology.md).
Members of a group must be associated with that group's
technology.

The state section of a group is special in that
any operation performed on it will be
applied to the state section of all of the group's members.

By comparison, the configuration and metadata sections belong to the
group itself. Thus, it is not possible to change a configuration or
metadata property across the members of a group in a single operation
like you can with state properties.

The behavior of reading state properties on a group is currently
undefined by the SOM and thus the behavior is technology-specific.
This may change at some point.
