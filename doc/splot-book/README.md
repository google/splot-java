Splot Design Documentation Source
=================================

This folder contains the sources for the Splot Design Documentation.

You can read the documentation directly from the sources or
you can read the rendered documentation [here](https://google.github.io/splot-java/splot-book/).

## Requirements

Building the documentation requires [mdBook](https://github.com/rust-lang-nursery/mdBook).
To get it:

    $ cargo install mdbook

## Building

To build the documentation, type:

    $ mdbook build

The output will be in the `book` directory. To check it out, open
[`book/index.html`](book/index.html) in your web browser.
