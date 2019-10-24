Splot for Java
==============

Splot for Java is an experimental set of Java libraries implementing
the [Splot Object Model (SOM)][SOM], enabling easy-to-use monitoring,
control, and thing-to-thing/machine-to-machine behaviors. It is broken
down into five subprojects:

*   `splot-base`: Provides the base classes and interfaces for the
    Splot Object Model.
*   `splot-processor`: An annotation processor for processing trait
    definitions.
*   `splot-traits`: A standard set of defined traits.
*   `splot-local`: Classes for implementing local things,
    as well as a local technology implementation.
*   `smcp`: A library for using the experimental CoAP-based [Splot Monitoring and Control Protocol][SMCP]
   using the [Splot Object Model][SOM] as
   implemented in `splot-base`.

[SOM]: https://google.github.io/splot-java/splot-book/som/intro.html
[SMCP]: https://google.github.io/splot-java/splot-book/smcp/intro.html

Splot for Java should be considered experimental and should not yet be
considered production-ready. This is an early release for testing and
discussion.

The ultimate goal for Splot is the development of an object model, API,
and protocol for IoT devices to facilitate straightforward,
easy to use machine-to-machine/thing-to-thing behaviors that are configured
locally and do not rely on cloud infrastructure.

## Protocol Implementation Features ##

*   Uses open standards like CoAP, CBOR, etc.
*   Fully working discovery, observing, groups, scenes, transitions, etc.
*   Type-safe property API
*   Easy to implement new things
*   Over 200 unit tests and growing.
*   In-band-manageable [automation primitives](doc/automation.md)

## Current Protocol Limitations ##

*   No native C/C++ implementation available yet.
*   No transport-layer security yet, security currently relies on
    link-layer security.
*   Java API is experimental and subject to change.

## Ongoing Work ##

*   Support for Automation Pairing and Automation Rules.
*   Native C implementation for SMCP that works on highly constrained
    devices, based on [libnyoci](http://libnyoci.org)
*   Splot Security Model Development
    *   Defines how native Splot/SMCP devices will securely operate.
    *   Broad strokes worked out, but details still being defined.
    *   Will likely use COSE, OSCORE, possibly DTLS under certain
        circumstances.

## Documentation ##

*   [Splot Design Documentation](https://google.github.io/splot-java/splot-book/)
*   [API Javadoc](https://google.github.io/splot-java/releases/latest/apidocs/)
*   [Github Project](https://github.com/google/splot-java)

## Examples ##

*   [SMCP Example Server](smcp-example-server/)
*   [Light Bulb Example](smcp-example-server/src/main/java/com/example/smcp/server/MyLightBulb.java)
*   [Coffee Machine Protocol Example](https://docs.google.com/document/d/e/2PACX-1vQselDu8k3rLdt8Qncy5ryL3uz7toLzzHgS6Sz9F0bfl1IhJsGzvsBG-WP5u3dLDkjITJipCFy6Ip18/pub)

<!-- TODO: Update this with real URL
*   [High-level introductory slide deck](TBD)
-->


### Related Projects ###

*   [CborTree](https://github.com/google/cbortree)
*   [CoapBlaster](https://github.com/google/coapblaster)

### Adding to Projects ###

In general, you do not need to build and install this project
in order to use it: by simply adding the appropriate dependencies
to your project's `build.gradle` or `pom.xml` file, the appropriate
jar files should be automatically downloaded.

Gradle:

    dependencies {
      compile 'com.google.iot.m2m:splot-base:0.02.00'
      compile 'com.google.iot.m2m:splot-traits:0.02.00'
      compile 'com.google.iot.m2m:splot-local:0.02.00'
      compile 'com.google.iot.m2m:smcp:0.02.00'
    }

Maven:

    <dependency>
      <groupId>com.google.iot.m2m</groupId>
      <artifactId>splot-base</artifactId>
      <version>0.02.00</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>com.google.iot.m2m</groupId>
      <artifactId>splot-traits</artifactId>
      <version>0.02.00</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>com.google.iot.m2m</groupId>
      <artifactId>splot-local</artifactId>
      <version>0.02.00</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>com.google.iot.m2m</groupId>
      <artifactId>smcp</artifactId>
      <version>0.02.00</version>
      <scope>compile</scope>
    </dependency>

## Building and Installing ##

This project uses Maven for building. Once Maven is installed, you
should be able to build and install the project by doing the
following:

    mvn verify
    mvn install

When building the project yourself, the version field for the
built artifacts will be `HEAD-SNAPSHOT`.

Note that the master branch of this project depends on
[CborTree](https://github.com/google/cbortree/) and
[CoapBlaster](https://github.com/google/coapblaster/), so you may need
to download, build, and install those projects first.


## License ##

Splot for Java is released under the [Apache 2.0 license](LICENSE).

    Copyright 2019 Google Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

## Disclaimer ##

This is not an officially supported Google product.
