/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.google.gson.annotations.SerializedName;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration", "MismatchedQueryAndUpdateOfCollection"})
class CodingNetGistRequest {
  @NotNull private final String description;
  @NotNull private final Map<String, GistFile> files;

  @SerializedName("public")
  private final boolean isPublic;

  public static class GistFile {
    @NotNull private final String content;

    public GistFile(@NotNull String content) {
      this.content = content;
    }
  }

  public CodingNetGistRequest(@NotNull List<CodingNetGist.FileContent> files, @NotNull String description, boolean isPublic) {
    this.description = description;
    this.isPublic = isPublic;

    this.files = new HashMap<String, GistFile>();
    for (CodingNetGist.FileContent file : files) {
      this.files.put(file.getFileName(), new GistFile(file.getContent()));
    }
  }
}
