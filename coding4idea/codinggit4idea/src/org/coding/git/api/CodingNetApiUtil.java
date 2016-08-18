/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.google.gson.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.containers.ContainerUtil;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.message.BasicHeader;
import org.coding.git.CodingNetOpenAPICodeMsg;
import org.coding.git.exceptions.*;
import org.coding.git.security.CodingNetSecurityUtil;
import org.coding.git.util.CodingNetAuthData;
import org.coding.git.util.CodingNetUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

public class CodingNetApiUtil {
    private static final Logger LOG = CodingNetUtil.LOG;

    public static final String DEFAULT_CODING_HOST = "coding.net";

    private static final String PER_PAGE = "pageSize=500";

    private static final Header ACCEPT_V3_JSON_HTML_MARKUP = new BasicHeader("Accept", "application/vnd.github.v3.html+json");
    private static final Header ACCEPT_JSON = new BasicHeader("Accept", "application/json");

    @NotNull
    private static final Gson gson = initGson();

    private static Gson initGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        return builder.create();
    }

    @NotNull
    public static <T> T fromJson(@Nullable JsonElement json, @NotNull Class<T> classT) throws IOException {
        if (json == null) {
            throw new CodingNetJsonException("Unexpected empty response");
        }

        T res;
        try {
            //cast as workaround for early java 1.6 bug
            //noinspection RedundantCast
            res = (T) gson.fromJson(json, classT);
        } catch (ClassCastException e) {
            throw new CodingNetJsonException("Parse exception while converting JSON to object " + classT.toString(), e);
        } catch (JsonParseException e) {
            throw new CodingNetJsonException("Parse exception while converting JSON to object " + classT.toString(), e);
        }
        if (res == null) {
            throw new CodingNetJsonException("Empty Json response");
        }
        return res;
    }

    @NotNull
    public static <Raw extends ICodingNetDataConstructor, Result> Result createDataFromRaw(@NotNull Raw rawObject, @NotNull Class<Result> resultClass)
            throws CodingNetJsonException {
        try {
            return rawObject.create(resultClass);
        } catch (Exception e) {
            throw new CodingNetJsonException("Json parse error", e);
        }
    }

  /*
   * Operations
   */

    public static void askForTwoFactorCodeSMS(@NotNull CodingNetConnection connection) {
        try {
            connection.postRequest("/authorizations", null, ACCEPT_JSON);
        } catch (IOException e) {
            LOG.info(e);
        }
    }

    @NotNull
    public static Collection<String> getTokenScopes(@NotNull CodingNetConnection connection) throws IOException {
        Header[] headers = connection.headRequest("/user", ACCEPT_JSON);

        Header scopesHeader = null;
        for (Header header : headers) {
            if (header.getName().equals("X-OAuth-Scopes")) {
                scopesHeader = header;
                break;
            }
        }
        if (scopesHeader == null) {
            throw new CodingNetConfusingException("No scopes header");
        }

        Collection<String> scopes = new ArrayList<String>();
        for (HeaderElement elem : scopesHeader.getElements()) {
            scopes.add(elem.getName());
        }
        return scopes;
    }

    @NotNull
    public static String getScopedToken(@NotNull CodingNetConnection connection, @NotNull Collection<String> scopes, @NotNull String note)
            throws IOException {
        try {
            return getNewScopedToken(connection, scopes, note).getToken();
        } catch (CodingNetStatusCodeException e) {
            if (e.getError() != null && e.getError().containsErrorCode("already_exists")) {
                // with new API we can't reuse old token, so let's just create new one
                // we need to change note as well, because it should be unique

                List<CodingNetAuthorization> tokens = getAllTokens(connection);

                for (int i = 1; i < 100; i++) {
                    final String newNote = note + "_" + i;
                    if (!ContainerUtil.exists(tokens, authorization -> newNote.equals(authorization.getNote()))) {
                        return getNewScopedToken(connection, scopes, newNote).getToken();
                    }
                }
            }
            throw e;
        }
    }

    @NotNull
    private static CodingNetAuthorization updateTokenScopes(@NotNull CodingNetConnection connection,
                                                            @NotNull CodingNetAuthorization token,
                                                            @NotNull Collection<String> scopes) throws IOException {
        try {
            String path = "/authorizations/" + token.getId();

            CodingNetAuthorizationUpdateRequest request = new CodingNetAuthorizationUpdateRequest(new ArrayList<String>(scopes));

            return createDataFromRaw(fromJson(connection.patchRequest(path, gson.toJson(request), ACCEPT_JSON), CodingNetAuthorizationRaw.class),
                    CodingNetAuthorization.class);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't update token: scopes - " + scopes);
            throw e;
        }
    }

    @NotNull
    private static CodingNetAuthorization getNewScopedToken(@NotNull CodingNetConnection connection,
                                                            @NotNull Collection<String> scopes,
                                                            @NotNull String note)
            throws IOException {
        try {
            String path = "/authorizations";

            CodingNetAuthorizationCreateRequest request = new CodingNetAuthorizationCreateRequest(new ArrayList<String>(scopes), note, null);

            return createDataFromRaw(fromJson(connection.postRequest(path, gson.toJson(request), ACCEPT_JSON), CodingNetAuthorizationRaw.class),
                    CodingNetAuthorization.class);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't create token: scopes - " + scopes + " - note " + note);
            throw e;
        }
    }

    @NotNull
    private static List<CodingNetAuthorization> getAllTokens(@NotNull CodingNetConnection connection) throws IOException {
        try {
            String path = "/authorizations";

            CodingNetConnection.PagedRequest<CodingNetAuthorization> request =
                    new CodingNetConnection.PagedRequest<CodingNetAuthorization>(path, CodingNetAuthorization.class, CodingNetAuthorizationRaw[].class, ACCEPT_JSON);

            return request.getAll(connection);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get available tokens");
            throw e;
        }
    }

    @NotNull
    public static String getMasterToken(@NotNull CodingNetConnection connection, @NotNull String note) throws IOException {
        // "repo" - read/write access to public/private repositories
        // "gist" - create/delete gists
        List<String> scopes = Arrays.asList("repo", "gist");

        return getScopedToken(connection, scopes, note);
    }

    @NotNull
    public static String getTasksToken(@NotNull CodingNetConnection connection,
                                       @NotNull String user,
                                       @NotNull String repo,
                                       @NotNull String note)
            throws IOException {
        CodingNetRepo repository = getDetailedRepoInfo(connection, user, repo);

        List<String> scopes = repository.isPrivate() ? Collections.singletonList("repo") : Collections.singletonList("public_repo");

        return getScopedToken(connection, scopes, note);
    }

    @NotNull
    public static CodingNetUser getCurrentUser(@NotNull CodingNetConnection connection) throws IOException {
        try {
            JsonElement result = connection.getRequest("/user", ACCEPT_JSON);
            return createDataFromRaw(fromJson(result, CodingNetUserRaw.class), CodingNetUser.class);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get user info");
            throw e;
        }
    }

    /**
     * 获取coding用户信息
     *
     * @param connection
     * @return
     * @throws IOException
     */
    @NotNull
    public static CodingNetUserDetailed getCurrentUserDetailed(@NotNull CodingNetConnection connection, CodingNetAuthData authData) throws IOException {
        CodingNetConnection.ResponsePage responsePage = null;
        try {
            CodingNetAuthData.BasicAuth auth = authData.getBasicAuth();
            if (auth != null) {
                String authCode = auth.getAuthCode();
                //--守护两步认证
                if (authCode != null) {
                    responsePage = connection.doPostRequest("/api/check_two_factor_auth_code?" + "code=" + authCode, null, ACCEPT_JSON);
                } else {
                    String password = CodingNetSecurityUtil.getUserPasswordOfSHA1(auth.getPassword() == null ? "" : auth.getPassword());
                    responsePage = connection.doPostRequest("/api/v2/account/login?" + "account=" + auth.getLogin() + "&" + "password=" + password + "&" + "remember_me=false", null, ACCEPT_JSON);
                }
                JsonElement result = responsePage.getJsonElement();

                int code = result.getAsJsonObject().get("code").getAsInt();
                if (code == CodingNetOpenAPICodeMsg.NEED_TWO_FACTOR_AUTH_CODE.getCode()||code==CodingNetOpenAPICodeMsg.TWO_FACTOR_AUTH_CODE_REQUIRED.getCode()) {
                    throw new CodingNetTwoFactorAuthenticationException();
                }
                CodingNetUserDetailed codingNetUserDetailed = createDataFromRaw(fromJson(result, CodingNetUserRaw.class), CodingNetUserDetailed.class);

                new CreateCookieSid(authData, responsePage).invoke();
                return codingNetUserDetailed;
            }
            throw new CodingNetAuthenticationException();
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get user info");
            throw e;
        } catch (CodingNetTwoFactorAuthenticationException e) {
            new CreateCookieSid(authData, responsePage).invoke();
            throw e;

        }

    }


    /**
     * 获取当前用户仓库列表
     *
     * @param connection
     * @return
     * @throws IOException
     */
    @NotNull
    public static List<CodingNetRepo> getUserRepos(@NotNull CodingNetConnection connection) throws IOException {
        try {
            String path = "/api/user/projects?" + PER_PAGE;

            CodingNetConnection.PagedRequest<CodingNetRepo> request = new CodingNetConnection.PagedRequest<CodingNetRepo>(path, CodingNetRepo.class, CodingNetRepoRaw[].class, ACCEPT_JSON);
            return request.getAll(connection);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get user repositories");
            throw e;
        }
    }

    @NotNull
    public static List<CodingNetRepo> getUserRepos(@NotNull CodingNetConnection connection, @NotNull String user) throws IOException {
        try {
            String path = "/users/" + user + "/repos?" + PER_PAGE;

            CodingNetConnection.PagedRequest<CodingNetRepo> request = new CodingNetConnection.PagedRequest<CodingNetRepo>(path, CodingNetRepo.class, CodingNetRepoRaw[].class, ACCEPT_JSON);

            return request.getAll(connection);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get user repositories: " + user);
            throw e;
        }
    }

    /**
     * 获取有效Git资源列表
     *
     * @param connection
     * @return
     * @throws IOException
     */
    @NotNull
    public static List<CodingNetRepo> getAvailableRepos(@NotNull CodingNetConnection connection) throws IOException {
        try {
            List<CodingNetRepo> repos = new ArrayList<CodingNetRepo>();

            repos.addAll(getUserRepos(connection));

            //try {
            //repos.addAll(getMembershipRepos(connection));
            //} catch (CodingNetStatusCodeException ignore) {
            //}
            //try {
            // repos.addAll(getWatchedRepos(connection));
            //} catch (CodingNetStatusCodeException ignore) {
            //}

            return repos;
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get available repositories");
            throw e;
        }
    }

    @NotNull
    public static List<CodingNetRepoOrg> getMembershipRepos(@NotNull CodingNetConnection connection) throws IOException {
        String orgsPath = "/user/orgs?" + PER_PAGE;
        CodingNetConnection.PagedRequest<CodingNetOrg> orgsRequest = new CodingNetConnection.PagedRequest<CodingNetOrg>(orgsPath, CodingNetOrg.class, CodingNetOrgRaw[].class);

        List<CodingNetRepoOrg> repos = new ArrayList<CodingNetRepoOrg>();
        for (CodingNetOrg org : orgsRequest.getAll(connection)) {
            String path = "/orgs/" + org.getLogin() + "/repos?type=member&" + PER_PAGE;
            CodingNetConnection.PagedRequest<CodingNetRepoOrg> request =
                    new CodingNetConnection.PagedRequest<CodingNetRepoOrg>(path, CodingNetRepoOrg.class, CodingNetRepoRaw[].class, ACCEPT_JSON);
            repos.addAll(request.getAll(connection));
        }

        return repos;
    }

    @NotNull
    public static List<CodingNetRepo> getWatchedRepos(@NotNull CodingNetConnection connection) throws IOException {
        String pathWatched = "/user/subscriptions?" + PER_PAGE;
        CodingNetConnection.PagedRequest<CodingNetRepo> requestWatched =
                new CodingNetConnection.PagedRequest<CodingNetRepo>(pathWatched, CodingNetRepo.class, CodingNetRepoRaw[].class, ACCEPT_JSON);
        return requestWatched.getAll(connection);
    }

    @NotNull
    public static CodingNetRepoDetailed getDetailedRepoInfo(@NotNull CodingNetConnection connection, @NotNull String owner, @NotNull String name)
            throws IOException {
        try {
            final String request = "/repos/" + owner + "/" + name;

            JsonElement jsonObject = connection.getRequest(request, ACCEPT_JSON);

            return createDataFromRaw(fromJson(jsonObject, CodingNetRepoRaw.class), CodingNetRepoDetailed.class);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get repository info: " + owner + "/" + name);
            throw e;
        }
    }

    public static void deleteCodingNetRepository(@NotNull CodingNetConnection connection, @NotNull String username, @NotNull String repo)
            throws IOException {
        try {
            String path = "/repos/" + username + "/" + repo;
            connection.deleteRequest(path);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't delete repository: " + username + "/" + repo);
            throw e;
        }
    }

    public static void deleteGist(@NotNull CodingNetConnection connection, @NotNull String id) throws IOException {
        try {
            String path = "/gists/" + id;
            connection.deleteRequest(path);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't delete gist: id - " + id);
            throw e;
        }
    }

    @NotNull
    public static CodingNetGist getGist(@NotNull CodingNetConnection connection, @NotNull String id) throws IOException {
        try {
            String path = "/gists/" + id;
            JsonElement result = connection.getRequest(path, ACCEPT_JSON);

            return createDataFromRaw(fromJson(result, CodingNetGistRaw.class), CodingNetGist.class);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get gist info: id " + id);
            throw e;
        }
    }

    @NotNull
    public static CodingNetGist createGist(@NotNull CodingNetConnection connection,
                                           @NotNull List<CodingNetGist.FileContent> contents,
                                           @NotNull String description,
                                           boolean isPrivate) throws IOException {
        try {
            String request = gson.toJson(new CodingNetGistRequest(contents, description, !isPrivate));
            return createDataFromRaw(fromJson(connection.postRequest("/gists", request, ACCEPT_JSON), CodingNetGistRaw.class), CodingNetGist.class);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't create gist");
            throw e;
        }
    }

    @NotNull
    public static List<CodingNetRepo> getForks(@NotNull CodingNetConnection connection, @NotNull String owner, @NotNull String name)
            throws IOException {
        String path = "/repos/" + owner + "/" + name + "/forks?" + PER_PAGE;
        CodingNetConnection.PagedRequest<CodingNetRepo> requestWatched =
                new CodingNetConnection.PagedRequest<CodingNetRepo>(path, CodingNetRepo.class, CodingNetRepoRaw[].class, ACCEPT_JSON);
        return requestWatched.getAll(connection);
    }

    @NotNull
    public static CodingNetPullRequest createPullRequest(@NotNull CodingNetConnection connection,
                                                         @NotNull String user,
                                                         @NotNull String repo,
                                                         @NotNull String title,
                                                         @NotNull String description,
                                                         @NotNull String head,
                                                         @NotNull String base) throws IOException {
        try {
            String request = gson.toJson(new CodingNetPullRequestRequest(title, description, head, base));
            return createDataFromRaw(
                    fromJson(connection.postRequest("/repos/" + user + "/" + repo + "/pulls", request, ACCEPT_JSON), CodingNetPullRequestRaw.class),
                    CodingNetPullRequest.class);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't create pull request");
            throw e;
        }
    }

    @NotNull
    public static CodingNetRepo createRepo(@NotNull CodingNetConnection connection,
                                           @NotNull String name,
                                           @NotNull String description,
                                           boolean isPrivate)
            throws IOException {
        try {
            String path = "/user/repos";

            CodingNetRepoRequest request = new CodingNetRepoRequest(name, description, isPrivate);

            return createDataFromRaw(fromJson(connection.postRequest(path, gson.toJson(request), ACCEPT_JSON), CodingNetRepoRaw.class),
                    CodingNetRepo.class);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't create repository: " + name);
            throw e;
        }
    }

    /*
     * Open issues only
     */
    @NotNull
    public static List<CodingNetIssue> getIssuesAssigned(@NotNull CodingNetConnection connection,
                                                         @NotNull String user,
                                                         @NotNull String repo,
                                                         @Nullable String assigned,
                                                         int max,
                                                         boolean withClosed) throws IOException {
        try {
            String state = "state=" + (withClosed ? "all" : "open");
            String path;
            if (StringUtil.isEmptyOrSpaces(assigned)) {
                path = "/repos/" + user + "/" + repo + "/issues?" + PER_PAGE + "&" + state;
            } else {
                path = "/repos/" + user + "/" + repo + "/issues?assignee=" + assigned + "&" + PER_PAGE + "&" + state;
            }

            CodingNetConnection.PagedRequest<CodingNetIssue> request = new CodingNetConnection.PagedRequest<CodingNetIssue>(path, CodingNetIssue.class, CodingNetIssueRaw[].class, ACCEPT_JSON);

            List<CodingNetIssue> result = new ArrayList<CodingNetIssue>();
            while (request.hasNext() && max > result.size()) {
                result.addAll(request.next(connection));
            }
            return result;
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get assigned issues: " + user + "/" + repo + " - " + assigned);
            throw e;
        }
    }

    @NotNull
  /*
   * All issues - open and closed
   */
    public static List<CodingNetIssue> getIssuesQueried(@NotNull CodingNetConnection connection,
                                                        @NotNull String user,
                                                        @NotNull String repo,
                                                        @Nullable String assignedUser,
                                                        @Nullable String query,
                                                        boolean withClosed) throws IOException {
        try {
            String state = withClosed ? "" : " state:open";
            String assignee = StringUtil.isEmptyOrSpaces(assignedUser) ? "" : " assignee:" + assignedUser;
            query = URLEncoder.encode("repo:" + user + "/" + repo + state + assignee + " " + query, CharsetToolkit.UTF8);
            String path = "/search/issues?q=" + query;

            //TODO: Use bodyHtml for issues - Coding does not support this feature for SearchApi yet
            JsonElement result = connection.getRequest(path, ACCEPT_JSON);

            return createDataFromRaw(fromJson(result, CodingNetIssuesSearchResultRaw.class), CodingNetIssuesSearchResult.class).getIssues();
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get queried issues: " + user + "/" + repo + " - " + query);
            throw e;
        }
    }

    @NotNull
    public static CodingNetIssue getIssue(@NotNull CodingNetConnection connection, @NotNull String user, @NotNull String repo, @NotNull String id)
            throws IOException {
        try {
            String path = "/repos/" + user + "/" + repo + "/issues/" + id;

            JsonElement result = connection.getRequest(path, ACCEPT_JSON);

            return createDataFromRaw(fromJson(result, CodingNetIssueRaw.class), CodingNetIssue.class);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get issue info: " + user + "/" + repo + " - " + id);
            throw e;
        }
    }

    @NotNull
    public static List<CodingNetIssueComment> getIssueComments(@NotNull CodingNetConnection connection,
                                                               @NotNull String user,
                                                               @NotNull String repo,
                                                               long id)
            throws IOException {
        try {
            String path = "/repos/" + user + "/" + repo + "/issues/" + id + "/comments?" + PER_PAGE;

            CodingNetConnection.PagedRequest<CodingNetIssueComment> request =
                    new CodingNetConnection.PagedRequest<CodingNetIssueComment>(path, CodingNetIssueComment.class, CodingNetIssueCommentRaw[].class, ACCEPT_V3_JSON_HTML_MARKUP);

            return request.getAll(connection);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get issue comments: " + user + "/" + repo + " - " + id);
            throw e;
        }
    }

    public static void setIssueState(@NotNull CodingNetConnection connection,
                                     @NotNull String user,
                                     @NotNull String repo,
                                     @NotNull String id,
                                     boolean open)
            throws IOException {
        try {
            String path = "/repos/" + user + "/" + repo + "/issues/" + id;

            CodingNetChangeIssueStateRequest request = new CodingNetChangeIssueStateRequest(open ? "open" : "closed");

            JsonElement result = connection.patchRequest(path, gson.toJson(request), ACCEPT_JSON);

            createDataFromRaw(fromJson(result, CodingNetIssueRaw.class), CodingNetIssue.class);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't set issue state: " + user + "/" + repo + " - " + id + "@" + (open ? "open" : "closed"));
            throw e;
        }
    }


    @NotNull
    public static CodingNetCommitDetailed getCommit(@NotNull CodingNetConnection connection,
                                                    @NotNull String user,
                                                    @NotNull String repo,
                                                    @NotNull String sha) throws IOException {
        try {
            String path = "/repos/" + user + "/" + repo + "/commits/" + sha;

            JsonElement result = connection.getRequest(path, ACCEPT_JSON);
            return createDataFromRaw(fromJson(result, CodingNetCommitRaw.class), CodingNetCommitDetailed.class);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get commit info: " + user + "/" + repo + " - " + sha);
            throw e;
        }
    }

    @NotNull
    public static List<CodingNetCommitComment> getCommitComments(@NotNull CodingNetConnection connection,
                                                                 @NotNull String user,
                                                                 @NotNull String repo,
                                                                 @NotNull String sha) throws IOException {
        try {
            String path = "/repos/" + user + "/" + repo + "/commits/" + sha + "/comments";

            CodingNetConnection.PagedRequest<CodingNetCommitComment> request =
                    new CodingNetConnection.PagedRequest<CodingNetCommitComment>(path, CodingNetCommitComment.class, CodingNetCommitCommentRaw[].class, ACCEPT_V3_JSON_HTML_MARKUP);

            return request.getAll(connection);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get commit comments: " + user + "/" + repo + " - " + sha);
            throw e;
        }
    }

    @NotNull
    public static List<CodingNetCommitComment> getPullRequestComments(@NotNull CodingNetConnection connection,
                                                                      @NotNull String user,
                                                                      @NotNull String repo,
                                                                      long id) throws IOException {
        try {
            String path = "/repos/" + user + "/" + repo + "/pulls/" + id + "/comments";

            CodingNetConnection.PagedRequest<CodingNetCommitComment> request =
                    new CodingNetConnection.PagedRequest<CodingNetCommitComment>(path, CodingNetCommitComment.class, CodingNetCommitCommentRaw[].class, ACCEPT_V3_JSON_HTML_MARKUP);

            return request.getAll(connection);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get pull request comments: " + user + "/" + repo + " - " + id);
            throw e;
        }
    }

    @NotNull
    public static CodingNetPullRequest getPullRequest(@NotNull CodingNetConnection connection, @NotNull String user, @NotNull String repo, int id)
            throws IOException {
        try {
            String path = "/repos/" + user + "/" + repo + "/pulls/" + id;
            return createDataFromRaw(fromJson(connection.getRequest(path, ACCEPT_V3_JSON_HTML_MARKUP), CodingNetPullRequestRaw.class),
                    CodingNetPullRequest.class);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get pull request info: " + user + "/" + repo + " - " + id);
            throw e;
        }
    }

    @NotNull
    public static List<CodingNetPullRequest> getPullRequests(@NotNull CodingNetConnection connection, @NotNull String user, @NotNull String repo)
            throws IOException {
        try {
            String path = "/repos/" + user + "/" + repo + "/pulls?" + PER_PAGE;

            CodingNetConnection.PagedRequest<CodingNetPullRequest> request =
                    new CodingNetConnection.PagedRequest<CodingNetPullRequest>(path, CodingNetPullRequest.class, CodingNetPullRequestRaw[].class, ACCEPT_V3_JSON_HTML_MARKUP);

            return request.getAll(connection);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get pull requests" + user + "/" + repo);
            throw e;
        }
    }

    @NotNull
    public static CodingNetConnection.PagedRequest<CodingNetPullRequest> getPullRequests(@NotNull String user, @NotNull String repo) {
        String path = "/repos/" + user + "/" + repo + "/pulls?" + PER_PAGE;

        return new CodingNetConnection.PagedRequest<CodingNetPullRequest>(path, CodingNetPullRequest.class, CodingNetPullRequestRaw[].class, ACCEPT_V3_JSON_HTML_MARKUP);
    }

    @NotNull
    public static List<CodingNetCommit> getPullRequestCommits(@NotNull CodingNetConnection connection,
                                                              @NotNull String user,
                                                              @NotNull String repo,
                                                              long id)
            throws IOException {
        try {
            String path = "/repos/" + user + "/" + repo + "/pulls/" + id + "/commits?" + PER_PAGE;

            CodingNetConnection.PagedRequest<CodingNetCommit> request =
                    new CodingNetConnection.PagedRequest<CodingNetCommit>(path, CodingNetCommit.class, CodingNetCommitRaw[].class, ACCEPT_JSON);

            return request.getAll(connection);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get pull request commits: " + user + "/" + repo + " - " + id);
            throw e;
        }
    }

    @NotNull
    public static List<CodingNetFile> getPullRequestFiles(@NotNull CodingNetConnection connection,
                                                          @NotNull String user,
                                                          @NotNull String repo,
                                                          long id)
            throws IOException {
        try {
            String path = "/repos/" + user + "/" + repo + "/pulls/" + id + "/files?" + PER_PAGE;

            CodingNetConnection.PagedRequest<CodingNetFile> request = new CodingNetConnection.PagedRequest<CodingNetFile>(path, CodingNetFile.class, CodingNetFileRaw[].class, ACCEPT_JSON);

            return request.getAll(connection);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get pull request files: " + user + "/" + repo + " - " + id);
            throw e;
        }
    }

    @NotNull
    public static List<CodingNetBranch> getRepoBranches(@NotNull CodingNetConnection connection, @NotNull String user, @NotNull String repo)
            throws IOException {
        try {
            String path = "/repos/" + user + "/" + repo + "/branches?" + PER_PAGE;

            CodingNetConnection.PagedRequest<CodingNetBranch> request =
                    new CodingNetConnection.PagedRequest<CodingNetBranch>(path, CodingNetBranch.class, CodingNetBranchRaw[].class, ACCEPT_JSON);

            return request.getAll(connection);
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't get repository branches: " + user + "/" + repo);
            throw e;
        }
    }

    @Nullable
    public static CodingNetRepo findForkByUser(@NotNull CodingNetConnection connection,
                                               @NotNull String user,
                                               @NotNull String repo,
                                               @NotNull String forkUser) throws IOException {
        try {
            String path = "/repos/" + user + "/" + repo + "/forks?" + PER_PAGE;

            CodingNetConnection.PagedRequest<CodingNetRepo> request = new CodingNetConnection.PagedRequest<CodingNetRepo>(path, CodingNetRepo.class, CodingNetRepoRaw[].class, ACCEPT_JSON);

            while (request.hasNext()) {
                for (CodingNetRepo fork : request.next(connection)) {
                    if (StringUtil.equalsIgnoreCase(fork.getUserName(), forkUser)) {
                        return fork;
                    }
                }
            }

            return null;
        } catch (CodingNetConfusingException e) {
            e.setDetails("Can't find fork by user: " + user + "/" + repo + " - " + forkUser);
            throw e;
        }
    }

    private static class CreateCookieSid {
        private CodingNetAuthData authData;
        private CodingNetConnection.ResponsePage responsePage;

        public CreateCookieSid(CodingNetAuthData authData, CodingNetConnection.ResponsePage responsePage) {
            this.authData = authData;
            this.responsePage = responsePage;
        }

        public void invoke() {
            if (responsePage != null) {
                Header[] headers = responsePage.getHeaders();
                for (Header header : headers) {
                    if (header.getName().equals("Set-Cookie")) {
                        HeaderElement[] headerElements = header.getElements();
                        for (HeaderElement headerElement : headerElements) {
                            if (headerElement.getName().equals("sid")) {
                                authData.getBasicAuth().setSid(headerElement.getValue());
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
}
