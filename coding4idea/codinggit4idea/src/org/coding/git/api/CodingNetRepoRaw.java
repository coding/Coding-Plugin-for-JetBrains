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

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * @author robin
 */
class CodingNetRepoRaw implements ICodingNetDataConstructor {
//  @Nullable public Long id;
//  @Nullable public String name;
//  @Nullable public String fullName;
//  @Nullable public String description;
//
//  @SerializedName("private")
//  @Nullable public Boolean isPrivate;
//  @SerializedName("fork")
//  @Nullable public Boolean isFork;
//
//  @Nullable public String url;
//  @Nullable public String htmlUrl;
//  @Nullable public String cloneUrl;
//  @Nullable public String gitUrl;
//  @Nullable public String sshUrl;
//  @Nullable public String svnUrl;
//  @Nullable public String mirrorUrl;
//
//  @Nullable public String homepage;
//  @Nullable public String language;
//  @Nullable public Integer size;
//
//  @Nullable public Integer forks;
//  @Nullable public Integer forksCount;
//  @Nullable public Integer watchers;
//  @Nullable public Integer watchersCount;
//  @Nullable public Integer openIssues;
//  @Nullable public Integer openIssuesCount;
//
//  @Nullable public String masterBranch;
//  @Nullable public String defaultBranch;
//
//  @Nullable public Boolean hasIssues;
//  @Nullable public Boolean hasWiki;
//  @Nullable public Boolean hasDownloads;
//
//  @Nullable public CodingNetRepoRaw parent;
//  @Nullable public CodingNetRepoRaw source;
//
//  @Nullable public CodingNetUserRaw owner;
//  @Nullable public CodingNetUserRaw organization;
//
//  @Nullable public Date pushedAt;
//  @Nullable public Date createdAt;
//  @Nullable public Date updatedAt;

  //@Nullable public Permissions permissions;

 // public Data data;


  //public static class Data{
    //List<Project> projects;
  //}

//  public static class Project{
    @SerializedName("https_url")
    String httpsUrl;
//  }




//  public static class Permissions {
//    @Nullable public Boolean admin;
//    @Nullable public Boolean pull;
//    @Nullable public Boolean push;
//
//    @SuppressWarnings("ConstantConditions")
//    @NotNull
//    public CodingNetRepoOrg.Permissions create() {
//      return new CodingNetRepoOrg.Permissions(admin, pull, push);
//    }
//  }


  @SuppressWarnings("ConstantConditions")
  @NotNull
  public CodingNetRepo createRepo() {
    //return new CodingNetRepo(name, description, isPrivate, isFork, htmlUrl, cloneUrl, defaultBranch, owner.createUser());
    return new CodingNetRepo(httpsUrl);
  }

//  @SuppressWarnings("ConstantConditions")
//  @NotNull
//  public CodingNetRepoOrg createRepoOrg() {
//    return new CodingNetRepoOrg(name, description, isPrivate, isFork, htmlUrl, cloneUrl, defaultBranch, owner.createUser(),
//                             permissions.create());
//  }

//  @SuppressWarnings("ConstantConditions")
//  @NotNull
//  public CodingNetRepoDetailed createRepoDetailed() {
//    CodingNetRepo parent = this.parent == null ? null : this.parent.createRepo();
//    CodingNetRepo source = this.source == null ? null : this.source.createRepo();
//    return new CodingNetRepoDetailed(name, description, isPrivate, isFork, htmlUrl, cloneUrl, defaultBranch, owner.createUser(),
//                                  parent, source);
//  }

  @SuppressWarnings("unchecked")
  @NotNull
  @Override
  public <T> T create(@NotNull Class<T> resultClass) {
    if (resultClass == CodingNetRepo.class) {
      return (T)createRepo();
    }
//    if (resultClass == CodingNetRepoOrg.class) {
//      return (T)createRepoOrg();
//    }
//    if (resultClass == CodingNetRepoDetailed.class) {
//      return (T)createRepoDetailed();
//    }

    throw new ClassCastException(this.getClass().getName() + ": bad class type: " + resultClass.getName());
  }
}
