# clipure

A command-line utility for manage clipboard history built with Clojure and compiled with GraalVM.

## Installation

TODO

## Usage

First, run `clipure listen` as a separated process to listen to clipboard changes accross your OS and cache in `~/.cache/clipure/`.

Then you can:

- `clipure history` - return all the entries from history.
- `clipure get` - return last entry from history (previous 1).
- `clipure get <n>` - return the previous N entry from history.
- `clipure copy <text>` - copy text to the clipboard.
- `clipure clear` - clear all clipboard history.

## Development

Install [babashka](https://github.com/babashka/babashka).

### JVM

`bb run` should run the CLI, you can pass any args like `bb run history`.

### Native image

`bb native-cli` will build a native-image with GraalVM, then you can `./clipure`.
