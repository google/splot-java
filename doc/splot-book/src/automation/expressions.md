## Splot Automation Expressions

Pairing transforms are specified using Splot Automation Expressions, a
specialized postfix (Reverse polish notation) language that is
expressed as a string. The language was designed to be expressive yet
simple to implement on highly-constrained devices.

For pairing transformations, the value being transformed is initially pushed
onto the stack and the last value on the stack after evaluation is the
output fo the expression.

The syntax is somewhat similar to forth, but unlike forth all numeric
values are considered floating-point.

For example, the following transform would apply *x²* to source
values before they are applied to the destination:

*   `"2 ^"` (or `"DUP *"`)

If the pairing was bidirectional, then the reverse transform for the
above expression would be:

*   `"0.5 ^"`

In this case we are raising *x* to the power of 0.5, which reverses
raising it to the power of two from the forward transform.

This example was fairly simple, but much more complex transforms are
possible. For example, the infix expression *(cos(x/2 - 0.5) + 1)/2*
would become `"2 / 0.5 - COS 1 + 2 /"`. (Note that currently, the
trigonometric operations use *turns* instead of *radians*)

In cases where an expression is used on an input, the previous input
can be pushed on the stack using `v_l`. This allows for things like
edge detection and determining the direction of a change. For
convenience, the current input can also be pushed onto the stack using
`v`.

The ability to handle arrays and dictionaries is also specified. For
example, imagine that we have a color light and a text display. Our
goal is to display the [approximate correlated color temperature
(CCT)][approxCT] of the light on the text display in Kelvin. However,
in this example, the color light only gives us the [CIE xy
chromaticity coordinates][CIExy] in the form of an array with two
floating point values. We can start by creating a pairing between the
CIE xy coordinates and the text display value. To get the right
transformation expression, we first have to have the infix expression:

*   *CCT(x, y) = -449n³ + 3525n² - 6823.3n + 5520.33*
*   where *n = (x − 0.332)/(y - 0.1858)*

By using the `POP` operation to remove the last item from an array on
the stack and push that value onto the stack, we end up with the
following expression:

    POP 0.1858 -
    SWAP POP 0.3320 -
    SWAP DROP SWAP /
    -449 3525 -6823.3 5520.33 POLY3

[approxCT]: https://en.wikipedia.org/wiki/Color_temperature#Approximation
[CIExy]: https://en.wikipedia.org/wiki/CIE_1931_color_space#CIE_xy_chromaticity_diagram_and_the_CIE_xyY_color_space

Arrays can be built by pushing values onto the stack and invoking one
of the array creation operators:

*   `[]` Pushes an empty array onto the stack.
*   `[1]` Pops the last value off of the stack, puts it into an array,
    and pushes that array onto the stack.
*   `[2]` Pops the last two values off of the stack, puts them into an
    array, and pushes that array onto the stack.
*   `[3]` Pops the last three values off of the stack, puts them into
    an array, and pushes that array onto the stack.
*   `[4]` Pops the last four values off of the stack, puts them into
    an array, and pushes that array onto the stack.

Dictionaries (json "objects") can also be read and written. The
following expression calculates the vector length for an input vector
specified as `{"x":12,"y":14}`:

    :x GET DUP * SWAP :y GET DUP * SWAP DROP + 0.5 ^

A dictionary can be created by putting it together value-by-value. For
example, the following expression takes an input of "turns" and
converts that to a vector:

    {} OVER COS :x PUT OVER SIN :y PUT

If you wanted to do something similar with arrays, you have two options:

 * `[] OVER COS PUSH OVER SIN PUSH`
 * `DUP COS SWAP SIN [2]`
