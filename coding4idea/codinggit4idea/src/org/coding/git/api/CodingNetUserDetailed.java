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

/**
 * @author robin
 */
public class CodingNetUserDetailed extends CodingNetUser {
  @Nullable private  String myName;
  @Nullable private  String myEmail;

  @Nullable private  Integer myOwnedPrivateRepos;

  @Nullable private  String myType;
  @Nullable private  UserPlan myPlan;

  /**
   * 用户信息详情
   */
  private Data myData;

  public static class Data{
    private String myName;

    public Data(String name){
      this.myName=name;
    }
  }


  public static class UserPlan {
    @NotNull private final String myName;
    private final long myPrivateRepos;

    public UserPlan(@NotNull String name, long privateRepos) {
      myName = name;
      myPrivateRepos = privateRepos;
    }

    @NotNull
    public String getName() {
      return myName;
    }

    public long getPrivateRepos() {
      return myPrivateRepos;
    }
  }

  public boolean canCreatePrivateRepo() {
    return getPlan() == null || getOwnedPrivateRepos() == null || getPlan().getPrivateRepos() > getOwnedPrivateRepos();
  }

  public CodingNetUserDetailed(){

  }


  public CodingNetUserDetailed(Data data){
    this.myData=data;
  }


  public CodingNetUserDetailed(@NotNull String login,
                               @NotNull String htmlUrl,
                               @Nullable String avatarUrl,
                               @Nullable String name,
                               @Nullable String email,
                               @Nullable Integer ownedPrivateRepos,
                               @Nullable String type,
                               @Nullable UserPlan plan) {
    super(login, htmlUrl, avatarUrl);
    myName = name;
    myEmail = email;
    myOwnedPrivateRepos = ownedPrivateRepos;
    myType = type;
    myPlan = plan;
  }

  @Nullable
  public String getName() {
    return myName;
  }

  @Nullable
  public String getEmail() {
    return myEmail;
  }

  @Nullable
  public String getType() {
    return myType;
  }

  @Nullable
  public Integer getOwnedPrivateRepos() {
    return myOwnedPrivateRepos;
  }

  @Nullable
  public UserPlan getPlan() {
    return myPlan;
  }
}
