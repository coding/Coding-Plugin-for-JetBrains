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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

/**
 * @author robin
 */
@SuppressWarnings("UnusedDeclaration")
public class CodingNetCommit extends CodingNetCommitSha {
  @Nullable private final CodingNetUser myAuthor;
  @Nullable private final CodingNetUser myCommitter;

  @NotNull private final List<CodingNetCommitSha> myParents;

  @NotNull private final GitCommit myCommit;

  public static class GitCommit {
    @NotNull private final String myMessage;

    @NotNull private final GitUser myAuthor;
    @NotNull private final GitUser myCommitter;

    public GitCommit(@NotNull String message, @NotNull GitUser author, @NotNull GitUser committer) {
      myMessage = message;
      myAuthor = author;
      myCommitter = committer;
    }

    @NotNull
    public String getMessage() {
      return myMessage;
    }

    @NotNull
    public GitUser getAuthor() {
      return myAuthor;
    }

    @NotNull
    public GitUser getCommitter() {
      return myCommitter;
    }
  }

  public static class GitUser {
    @NotNull private final String myName;
    @NotNull private final String myEmail;
    @NotNull private final Date myDate;

    public GitUser(@NotNull String name, @NotNull String email, @NotNull Date date) {
      myName = name;
      myEmail = email;
      myDate = date;
    }

    @NotNull
    public String getName() {
      return myName;
    }

    @NotNull
    public String getEmail() {
      return myEmail;
    }

    @NotNull
    public Date getDate() {
      return myDate;
    }
  }

  public CodingNetCommit(@NotNull String url,
                         @NotNull String sha,
                         @Nullable CodingNetUser author,
                         @Nullable CodingNetUser committer,
                         @NotNull List<CodingNetCommitSha> parents,
                         @NotNull GitCommit commit) {
    super(url, sha);
    myAuthor = author;
    myCommitter = committer;
    myParents = parents;
    myCommit = commit;
  }

  @Nullable
  public CodingNetUser getAuthor() {
    return myAuthor;
  }

  @Nullable
  public CodingNetUser getCommitter() {
    return myCommitter;
  }

  @NotNull
  public List<CodingNetCommitSha> getParents() {
    return myParents;
  }

  @NotNull
  public GitCommit getCommit() {
    return myCommit;
  }
}
