/*
 * Copyright (c) 2015, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.dartlang.vm.service.element;

// This is a generated file.

import com.google.gson.JsonObject;

/**
 * A {@link LibraryDependency} provides information about an import or export.
 */
public class LibraryDependency extends Element {

  public LibraryDependency(JsonObject json) {
    super(json);
  }

  /**
   * Is this dependency deferred?
   */
  public boolean getIsDeferred() {
    return json.get("isDeferred").getAsBoolean();
  }

  /**
   * Is this dependency an import (rather than an export)?
   */
  public boolean getIsImport() {
    return json.get("isImport").getAsBoolean();
  }

  /**
   * The prefix of an 'as' import, or null.
   */
  public String getPrefix() {
    return json.get("prefix").getAsString();
  }

  /**
   * The library being imported or exported.
   */
  public LibraryRef getTarget() {
    return new LibraryRef((JsonObject) json.get("target"));
  }
}
