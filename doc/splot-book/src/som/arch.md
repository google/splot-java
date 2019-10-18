## Architecture ##

![Splot Object Model UML Diagram](splot-object-model.svg)

Breaking this down:

*   A *physical device* can host one or more *things*.
*   A *thing* can have multiple *properties* and *methods*, each
    defined by a *trait*.
*   A *thing* can host other *things* that it owns (children).
*   While not illustrated in the previous diagram, each child is
    associated with a single trait on its parent, in much the same way
    that properties and methods are.
