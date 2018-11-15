Splot for Java
==============

Splot for Java is an experimental set of Java libraries implementing
the Splot Object Model (SOM), enabling easy-to-use monitoring,
control, and thing-to-thing/machine-to-machine behaviors. It is broken
down into five subprojects:

*   `splot-base`: Provides the base classes and interfaces for the
    Splot Object Model.
*   `splot-processor`: An annotation processor for processing trait
    definitions.
*   `splot-traits`: A standard set of defined traits.
*   `splot-local`: Classes for implementing local functional
    endpoints, as well as a local technology implementation.
*   `smcp`: A library for using the experimental CoAP-based Simple
    Monitoring and Control Protocol using the Splot Object Model as
    implemented in `splot-base`.

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
*   Easy to implement new functional endpoints
*   Over 200 unit tests and growing.

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

## Introductory Material ##

*   [Introduction to the Splot Object Model
    (SOM)](doc/splot-object-model-intro.md)
*   [Introduction to the Simple Monitoring and Control
    Protocol](doc/smcp-intro.md)
*   [Light Bulb Example](doc/MyLightBulb.java)

<!-- TODO: Update this with real URL
*   [High-level introductory slide deck](TBD)
-->

## Documentation ##

*   [API Javadoc](https://google.github.io/splot-java/releases/latest/apidocs/)
*   [Github Project](https://github.com/google/splot-java)

### Related Projects ###

*   [CborTree](https://github.com/google/cbortree)
*   [CoapBlaster](https://github.com/google/coapblaster)

## Building and Installing ##

This project uses Maven for building. Once Maven is installed, you
should be able to build and install the project by doing the
following:

    mvn verify
    mvn install

Note that the master branch of this project depends on
[CborTree](https://github.com/google/cbortree/) and
[CoapBlaster](https://github.com/google/coapblaster/), so you may need
to download, build, and install those projects first.

### Adding to Projects ###

Gradle:

    dependencies {
      compile 'com.google.iot.m2m:splot-base:0.01.00'
      compile 'com.google.iot.m2m:splot-traits:0.01.00'
      compile 'com.google.iot.m2m:splot-local:0.01.00'
      compile 'com.google.iot.m2m:smcp:0.01.00'
    }

Maven:

    <dependency>
      <groupId>com.google.iot.m2m</groupId>
      <artifactId>splot-base</artifactId>
      <version>0.01.00</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>com.google.iot.m2m</groupId>
      <artifactId>splot-traits</artifactId>
      <version>0.01.00</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>com.google.iot.m2m</groupId>
      <artifactId>splot-local</artifactId>
      <version>0.01.00</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>com.google.iot.m2m</groupId>
      <artifactId>smcp</artifactId>
      <version>0.01.00</version>
      <scope>compile</scope>
    </dependency>

## License ##

Splot for Java is released under the [Apache 2.0 license](LICENSE).

    Copyright 2018 Google Inc.

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
