### Rules

Rules are actionable automation primitives that consist of one or more
*conditions*. Each condition can evaluate to "true" or "false". A rule
can be configured to trigger when ALL conditions match or when ANY
condition matches.

A condition monitors a specific URI value and defines how that value
should be interpreted in the form of a Splot Automation Expression, as
described above. Similar to pairings, the previous value is pushed
onto the stack before the current value: allowing for edge triggers.
