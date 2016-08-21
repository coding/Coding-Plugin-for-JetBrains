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


@SuppressWarnings("UnusedDeclaration")
public class CodingNetCommitDetailed extends CodingNetCommit {
  @NotNull private final CommitStats myStats;
  @NotNull private final List<CodingNetFile> myFiles;

  public static class CommitStats {
    private final int myAdditions;
    private final int myDeletions;
    private final int myTotal;

    public CommitStats(int additions, int deletions, int total) {
      myAdditions = additions;
      myDeletions = deletions;
      myTotal = total;
    }

    public int getAdditions() {
      return myAdditions;
    }

    public int getDeletions() {
      return myDeletions;
    }

    public int getTotal() {
      return myTotal;
    }
  }

  public CodingNetCommitDetailed(@NotNull String url,
                                 @NotNull String sha,
                                 @Nullable CodingNetUser author,
                                 @Nullable CodingNetUser committer,
                                 @NotNull List<CodingNetCommitSha> parents,
                                 @NotNull GitCommit commit,
                                 @NotNull CommitStats stats,
                                 @NotNull List<CodingNetFile> files) {
    super(url, sha, author, committer, parents, commit);
    myStats = stats;
    myFiles = files;
  }

  @NotNull
  public CommitStats getStats() {
    return myStats;
  }

  @NotNull
  public List<CodingNetFile> getFiles() {
    return myFiles;
  }
}
