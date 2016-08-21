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

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

/**
 * @author Aleksey Pivovarov
 */
public class CodingNetIssue {
  @NotNull private final String myHtmlUrl;
  private final long myNumber;
  @NotNull private final String myState;
  @NotNull private final String myTitle;
  @NotNull private final String myBody;

  @NotNull private final CodingNetUser myUser;
  @Nullable private final CodingNetUser myAssignee;

  @Nullable private final Date myClosedAt;
  @NotNull private final Date myCreatedAt;
  @NotNull private final Date myUpdatedAt;

  public CodingNetIssue(@NotNull String htmlUrl,
                        long number,
                        @NotNull String state,
                        @NotNull String title,
                        @Nullable String body,
                        @NotNull CodingNetUser user,
                        @Nullable CodingNetUser assignee,
                        @Nullable Date closedAt,
                        @NotNull Date createdAt,
                        @NotNull Date updatedAt) {
    myHtmlUrl = htmlUrl;
    myNumber = number;
    myState = state;
    myTitle = title;
    myBody = StringUtil.notNullize(body);
    myUser = user;
    myAssignee = assignee;
    myClosedAt = closedAt;
    myCreatedAt = createdAt;
    myUpdatedAt = updatedAt;
  }

  @NotNull
  public String getHtmlUrl() {
    return myHtmlUrl;
  }

  public long getNumber() {
    return myNumber;
  }

  @NotNull
  public String getState() {
    return myState;
  }

  @NotNull
  public String getTitle() {
    return myTitle;
  }

  @NotNull
  public String getBody() {
    return myBody;
  }

  @NotNull
  public CodingNetUser getUser() {
    return myUser;
  }

  @Nullable
  public CodingNetUser getAssignee() {
    return myAssignee;
  }

  @Nullable
  public Date getClosedAt() {
    return myClosedAt;
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
