/*
 * Copyright 2016 Coding
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
package org.coding.git.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author robin
 */
@SuppressWarnings("UnusedDeclaration")
class CodingNetAuthorizationRaw implements ICodingNetDataConstructor {
  @Nullable public Long id;
  @Nullable public String url;
  @Nullable public String token;
  @Nullable public String note;
  @Nullable public String noteUrl;
  @Nullable public List<String> scopes;

  public CodingNetAuthorization createAuthorization() {
    return new CodingNetAuthorization(id, token, scopes, note);
  }

  @SuppressWarnings("unchecked")
  @NotNull
  @Override
  public <T> T create(@NotNull Class<T> resultClass) {
    if (resultClass == CodingNetAuthorization.class) {
      return (T)createAuthorization();
    }

    throw new ClassCastException(this.getClass().getName() + ": bad class type: " + resultClass.getName());
  }
}
