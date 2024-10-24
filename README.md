## Endec

endec is a format-agnostic serialization framework inspired by Rust's [serde](https://serde.rs) library and the Codec API from Mojang's [DataFixerUpper](https://github.com/mojang/datafixerupper).

This repository contains the in-progress Java implementation. For a more-complete implementation without guarantees, see this repository's [reference project](https://github.com/gliscowo/endec.dart) on the glisco's profile (written in Dart).

### Repository Structure

This project contains 3 submodules with the root src containing the core `endec` package which defines the API and contains some base implementations. The nested modules are as follows:

- `gson`: Support for Json language using the [Gson](https://github.com/google/gson) Library
- `jankson`: Support for Json/Json5 language using the [Jankson](https://github.com/falkreon/Jankson) Library
- `netty`: Support for the binary format using the [Netty](https://github.com/netty/netty) Library

### Documentation

For the time being, documentation can be found in the owo section of the [Wisp Forest docs](https://docs.wispforest.io/owo/endec). The linked document adequately explains the basics but is out-of-date and does not agree with this reference implementation in a number of places - it will be updated to match in the future

### Acknowledgements

The excellent serde documentation and [enjarai's](https://enjarai.dev) Codec guide [on the Fabric Docs](https://docs.fabricmc.net/develop/codecs) have been invaluable during development. Further, [glisco](https://github.com/gliscowo) is responsible for developing the initial implementation within Dart