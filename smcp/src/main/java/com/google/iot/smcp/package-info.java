/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * An experimental implementation of a simple monitoring and control protocol written in Java.
 *
 * <p>Uses <a href="https://github.com/google/splot-java">Splot for Java</a> for the API and object
 * model, <a href="https://github.com/google/coapblaster">CoapBlaster</a> for the CoAP stack, and <a
 * href="https://github.com/google/CborTree">CborTree</a> for CBOR encoding/decoding.
 */
@ParametersAreNonnullByDefault
@CheckReturnValue
package com.google.iot.smcp;

import com.google.errorprone.annotations.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
