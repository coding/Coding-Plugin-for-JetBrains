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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@SuppressWarnings("UnusedDeclaration")
class CodingNetCommitRaw implements ICodingNetDataConstructor {
  @Nullable public String url;
  @Nullable public String sha;

  @Nullable public CodingNetUserRaw author;
  @Nullable public CodingNetUserRaw committer;

  @Nullable public GitCommitRaw commit;

  @Nullable public CommitStatsRaw stats;
  @Nullable public List<CodingNetFileRaw> files;

  @Nullable public List<CodingNetCommitRaw> parents;

  public static class GitCommitRaw {
    @Nullable public String url;
    @Nullable public String message;

    @Nullable public GitUserRaw author;
    @Nullable public GitUserRaw committer;

    @SuppressWarnings("ConstantConditions")
    @NotNull
    public CodingNetCommit.GitCommit create() {
      return new CodingNetCommit.GitCommit(message, author.create(), committer.create());
    }
  }

  public static class GitUserRaw {
    @Nullable public String name;
    @Nullable public String email;
    @Nullable public Date date;

    @SuppressWarnings("ConstantConditions")
    @NotNull
    public CodingNetCommit.GitUser create() {
      return new CodingNetCommit.GitUser(name, email, date);
    }
  }

  public static class CommitStatsRaw {
    @Nullable public Integer additions;
    @Nullable public Integer deletions;
    @Nullable public Integer total;

    @SuppressWarnings("ConstantConditions")
    @NotNull
    public CodingNetCommitDetailed.CommitStats create() {
      return new CodingNetCommitDetailed.CommitStats(additions, deletions, total);
    }
  }

  @SuppressWarnings("ConstantConditions")
  @NotNull
  public CodingNetCommitSha createCommitSha() {
    return new CodingNetCommitSha(url, sha);
  }

  @SuppressWarnings("ConstantConditions")
  @NotNull
  public CodingNetCommit createCommit() {
    CodingNetUser author = this.author == null ? null : this.author.createUser();
    CodingNetUser committer = this.committer == null ? null : this.committer.createUser();

    List<CodingNetCommitSha> parents = new ArrayList<CodingNetCommitSha>();
    for (CodingNetCommitRaw raw : this.parents) {
      parents.add(raw.createCommitSha());
    }
    return new CodingNetCommit(url, sha, author, committer, parents, commit.create());
  }

  @SuppressWarnings("ConstantConditions")
  @NotNull
  public CodingNetCommitDetailed createCommitDetailed() {
    CodingNetCommit commit = createCommit();
    List<CodingNetFile> files = new ArrayList<CodingNetFile>();
    for (CodingNetFileRaw raw : this.files) {
      files.add(raw.createFile());
    }

    return new CodingNetCommitDetailed(commit.getUrl(), commit.getSha(), commit.getAuthor(), commit.getCommitter(), commit.getParents(),
                                    commit.getCommit(), stats.create(), files);
  }

  @SuppressWarnings("unchecked")
  @NotNull
  @Override
  public <T> T create(@NotNull Class<T> resultClass) {
    if (resultClass == CodingNetCommitSha.class) {
      return (T)createCommitSha();
    }
    if (resultClass == CodingNetCommit.class) {
      return (T)createCommit();
    }
    if (resultClass == CodingNetCommitDetailed.class) {
      return (T)createCommitDetailed();
    }

    throw new ClassCastException(this.getClass().getName() + ": bad class type: " + resultClass.getName());
  }
}
