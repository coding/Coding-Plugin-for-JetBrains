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

/**
 *
 */
@SuppressWarnings("UnusedDeclaration")
class CodingNetUserRaw implements ICodingNetDataConstructor {

  @Nullable public String login;
  @Nullable public Long id;

 @Nullable public String url;
  @Nullable public String htmlUrl;

//  @Nullable public String name;
//  @Nullable public String email;
//  @Nullable public String company;
//  @Nullable public String location;
//  @Nullable public String type;
//
//  @Nullable public Integer publicRepos;
//  @Nullable public Integer publicGists;
//  @Nullable public Integer totalPrivateRepos;
//  @Nullable public Integer ownedPrivateRepos;
//  @Nullable public Integer privateGists;
//  @Nullable public Long diskUsage;
//
//  @Nullable public Integer followers;
//  @Nullable public Integer following;
  @Nullable public String avatarUrl;
//  @Nullable public String gravatarId;
//  @Nullable public Integer collaborators;
//  @Nullable public String blog;
//
//  @Nullable public UserPlanRaw plan;

//  @Nullable public Date createdAt;

  @Nullable public Data data;

  public static class Data{
    public String name;

    /**
     * 构建用户详情对象
     * @return
     */
    public CodingNetUserDetailed.Data create(){
      return new CodingNetUserDetailed.Data(name);
    }

  }


  public static class UserPlanRaw {
    @Nullable public String name;
    @Nullable public Long space;
    @Nullable public Long collaborators;
    @Nullable public Long privateRepos;

    @SuppressWarnings("ConstantConditions")
    @NotNull
    public CodingNetUserDetailed.UserPlan create() {
      return new CodingNetUserDetailed.UserPlan(name, privateRepos);
    }
  }

  @SuppressWarnings("ConstantConditions")
  @NotNull
  public CodingNetUser createUser() {
    return new CodingNetUser(login, htmlUrl, avatarUrl);
  }

  @SuppressWarnings("ConstantConditions")
  @NotNull
  public CodingNetUserDetailed createUserDetailed() {
    CodingNetUserDetailed.Data data = this.data == null ? null : this.data.create();
//    return new CodingNetUserDetailed(login, htmlUrl, avatarUrl, name, email, ownedPrivateRepos, type, plan);
    return new CodingNetUserDetailed(data);
  }

  @SuppressWarnings("unchecked")
  @NotNull
  @Override
  public <T> T create(@NotNull Class<T> resultClass) {
//    if (resultClass == CodingNetUser.class) {
//      return (T)createUser();
//    }
    if (resultClass == CodingNetUserDetailed.class) {
      return (T)createUserDetailed();
    }

    throw new ClassCastException(this.getClass().getName() + ": bad class type: " + resultClass.getName());
  }
}
