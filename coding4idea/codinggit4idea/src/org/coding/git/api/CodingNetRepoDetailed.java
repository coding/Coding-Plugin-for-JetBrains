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


public class CodingNetRepoDetailed extends CodingNetRepo {
  @Nullable private final CodingNetRepo myParent;
  @Nullable private final CodingNetRepo mySource;

  public CodingNetRepoDetailed(@NotNull String name,
                               @Nullable String description,
                               boolean isPrivate,
                               boolean isFork,
                               @NotNull String htmlUrl,
                               @NotNull String cloneUrl,
                               @Nullable String defaultBranch,
                               @NotNull CodingNetUser owner,
                               @Nullable CodingNetRepo parent,
                               @Nullable CodingNetRepo source) {
    super(name, description, isPrivate, isFork, htmlUrl, cloneUrl, defaultBranch, owner);
    myParent = parent;
    mySource = source;
  }

  @Nullable
  public CodingNetRepo getParent() {
    return myParent;
  }

  @Nullable
  public CodingNetRepo getSource() {
    return mySource;
  }
}
