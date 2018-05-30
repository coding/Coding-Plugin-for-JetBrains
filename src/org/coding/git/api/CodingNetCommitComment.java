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

import java.util.Date;


@SuppressWarnings("UnusedDeclaration")
public class CodingNetCommitComment {
  @NotNull private final String myHtmlUrl;

  private final long myId;
  @NotNull private final String mySha;
  @NotNull private final String myPath;
  private final long myPosition; // line number in diff
  @NotNull private final String myBodyHtml;

  @NotNull private final CodingNetUser myUser;

  @NotNull private final Date myCreatedAt;
  @NotNull private final Date myUpdatedAt;

  public CodingNetCommitComment(@NotNull String htmlUrl,
                                long id,
                                @NotNull String sha,
                                @NotNull String path,
                                long position,
                                @NotNull String bodyHtml,
                                @NotNull CodingNetUser user,
                                @NotNull Date createdAt,
                                @NotNull Date updatedAt) {
    myHtmlUrl = htmlUrl;
    myId = id;
    mySha = sha;
    myPath = path;
    myPosition = position;
    myBodyHtml = bodyHtml;
    myUser = user;
    myCreatedAt = createdAt;
    myUpdatedAt = updatedAt;
  }

  @NotNull
  public String getHtmlUrl() {
    return myHtmlUrl;
  }

  public long getId() {
    return myId;
  }

  @NotNull
  public String getSha() {
    return mySha;
  }

  @NotNull
  public String getPath() {
    return myPath;
  }

  public long getPosition() {
    return myPosition;
  }

  @NotNull
  public String getBodyHtml() {
    return myBodyHtml;
  }

  @NotNull
  public CodingNetUser getUser() {
    return myUser;
  }

  @NotNull
  public Date getCreatedAt() {
    return myCreatedAt;
  }

  @NotNull
  public Date getUpdatedAt() {
    return myUpdatedAt;
  }
}
