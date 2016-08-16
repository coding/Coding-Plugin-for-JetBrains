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
package org.coding.git.util;

import com.intellij.openapi.util.text.StringUtil;
import org.coding.git.api.CodingNetApiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.coding.git.api.CodingNetFullPath;

/**
 * @author robin
 */
public class CodingNetUrlUtil {

  /**
   * 去除协议前缀
   * @param url
   * @return
   */
  @NotNull
  public static String removeProtocolPrefix(String url) {
    int index = url.indexOf('@');
    if (index != -1) {
      return url.substring(index + 1).replace(':', '/');
    }
    index = url.indexOf("://");
    if (index != -1) {
      return url.substring(index + 3);
    }
    return url;
  }

  /**
   * 去除结尾斜杠
   * @param s
   * @return
   */
  @NotNull
  public static String removeTrailingSlash(@NotNull String s) {
    if (s.endsWith("/")) {
      return s.substring(0, s.length() - 1);
    }
    return s;
  }

  @NotNull
  public static String getApiUrl(@NotNull String urlFromSettings) {
    return getApiProtocolFromUrl(urlFromSettings) + getCodingOpenApiUrlDomainNameWithoutProtocol(urlFromSettings);
  }

  @NotNull
  public static String getApiProtocol() {
    return getApiProtocolFromUrl(CodingNetSettings.getInstance().getHost());
  }

  @NotNull
  public static String getApiProtocolFromUrl(@NotNull String urlFromSettings) {
    if (StringUtil.startsWithIgnoreCase(urlFromSettings.trim(), "http://")) return "http://";
    return "https://";
  }

  @NotNull
  public static String getApiUrl() {
    return getApiUrl(CodingNetSettings.getInstance().getHost());
  }

  @NotNull
  public static String getCodingHost() {
    return getApiProtocol() + getGitHostWithoutProtocol();
  }

  @NotNull
  public static String getHostFromUrl(@NotNull String url) {
    String path = removeProtocolPrefix(url).replace(':', '/');
    int index = path.indexOf('/');
    if (index == -1) {
      return path;
    }
    else {
      return path.substring(0, index);
    }
  }

  @NotNull
  public static String getGitHostWithoutProtocol() {
    return removeTrailingSlash(removeProtocolPrefix(CodingNetSettings.getInstance().getHost()));
  }

  /**
   * 根据Coding openAPI请求域名为https://coding.net
   * 如获取用户下项目列表为:https://coding.net/api/user/projects
   * @param urlFromSettings
   * @return
   */
  @NotNull
  public static String getCodingOpenApiUrlDomainNameWithoutProtocol(@NotNull String urlFromSettings) {
    String url = removeTrailingSlash(removeProtocolPrefix(urlFromSettings.toLowerCase()));
    if (url.equals(CodingNetApiUtil.DEFAULT_CODING_HOST)) {
      return  url;
    }
    return null;
  }

  public static boolean isCodingNetUrl(@NotNull String url) {
    return isCodingNetUrl(url, CodingNetSettings.getInstance().getHost());
  }

  public static boolean isCodingNetUrl(@NotNull String url, @NotNull String host) {
    host = getHostFromUrl(host);
    url = removeProtocolPrefix(url);
    if (StringUtil.startsWithIgnoreCase(url, host)) {
      if (url.length() > host.length() && ":/".indexOf(url.charAt(host.length())) == -1) {
        return false;
      }
      return true;
    }
    return false;
  }

  @Nullable
  public static CodingNetFullPath getUserAndRepositoryFromRemoteUrl(@NotNull String remoteUrl) {
    remoteUrl = removeProtocolPrefix(removeEndingDotGit(remoteUrl));
    int index1 = remoteUrl.lastIndexOf('/');
    if (index1 == -1) {
      return null;
    }
    String url = remoteUrl.substring(0, index1);
    int index2 = Math.max(url.lastIndexOf('/'), url.lastIndexOf(':'));
    if (index2 == -1) {
      return null;
    }
    final String username = remoteUrl.substring(index2 + 1, index1);
    final String reponame = remoteUrl.substring(index1 + 1);
    if (username.isEmpty() || reponame.isEmpty()) {
      return null;
    }
    return new CodingNetFullPath(username, reponame);
  }

  @Nullable
  public static String makeCodingNetRepoUrlFromRemoteUrl(@NotNull String remoteUrl) {
    return makeCodingNetRepoUrlFromRemoteUrl(remoteUrl, getCodingHost());
  }

  @Nullable
  public static String makeCodingNetRepoUrlFromRemoteUrl(@NotNull String remoteUrl, @NotNull String host) {
    CodingNetFullPath repo = getUserAndRepositoryFromRemoteUrl(remoteUrl);
    if (repo == null) {
      return null;
    }
    return host + '/' + repo.getUser() + '/' + repo.getRepository();
  }

  @NotNull
  private static String removeEndingDotGit(@NotNull String url) {
    url = removeTrailingSlash(url);
    final String DOT_GIT = ".git";
    if (url.endsWith(DOT_GIT)) {
      return url.substring(0, url.length() - DOT_GIT.length());
    }
    return url;
  }

  @NotNull
  public static String getCloneUrl(@NotNull CodingNetFullPath path) {
    return getCloneUrl(path.getUser(), path.getRepository());
  }

  @NotNull
  public static String getCloneUrl(@NotNull String user, @NotNull String repo) {
    if (CodingNetSettings.getInstance().isCloneGitUsingSsh()) {
      return "git@" + getGitHostWithoutProtocol() + ":" + user + "/" + repo + ".git";
    }
    else {
      return getApiProtocol() + getGitHostWithoutProtocol() + "/" + user + "/" + repo + ".git";
    }
  }
}
