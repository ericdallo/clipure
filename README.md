# clipure

A command-line utility for manage clipboard history built with Clojure and compiled with GraalVM.

## Installation

TODO

## Usage

First, run `clipure listen` as a separated process to listen to clipboard changes accross your OS and cache in `~/.cache/clipure/`.

Then you can:

- `clipure history` - return all the entries from history
- `clipure get` - return last entry from history
