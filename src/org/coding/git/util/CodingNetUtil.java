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
package org.coding.git.util;

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.ThrowableConvertor;
import com.intellij.util.containers.Convertor;
import git4idea.DialogManager;
import git4idea.GitUtil;
import git4idea.commands.GitCommand;
import git4idea.commands.GitSimpleHandler;
import git4idea.config.GitVcsApplicationSettings;
import git4idea.config.GitVersion;
import git4idea.i18n.GitBundle;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.coding.git.api.CodingNetApiUtil;
import org.coding.git.api.CodingNetConnection;
import org.coding.git.api.CodingNetUserDetailed;
import org.coding.git.exceptions.CodingNetTwoFactorAuthenticationException;
import org.coding.git.ui.CodingNetBasicLoginDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.coding.git.exceptions.CodingNetAuthenticationException;
import org.coding.git.exceptions.CodingNetOperationCanceledException;
import org.coding.git.ui.CodingNetLoginDialog;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * @author robin
 */
public class CodingNetUtil {

    public static final Logger LOG = Logger.getInstance("coding.net");

    /**
     * 执行登陆动作
     *
     * @param project
     * @param authHolder
     * @param indicator
     * @param task
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T> T runTask(@NotNull Project project,
                                @NotNull CodingNetAuthDataHolder authHolder,
                                @NotNull final ProgressIndicator indicator,
                                @NotNull ThrowableConvertor<CodingNetConnection, T, IOException> task) throws IOException {
        //--根据设置选项设置
        CodingNetAuthData auth = authHolder.getAuthData();
        try {
            //--网络连接器
            final CodingNetConnection connection = new CodingNetConnection(auth, true);
            ScheduledFuture<?> future = null;

            try {
                //--直接尝试获取用户信息
                CodingNetAuthData.BasicAuth basicAuth = auth.getBasicAuth();
                //if (basicAuth.getAuthCode() == null)
                    CodingNetApiUtil.getCurrentUserDetailed(connection, auth);

                future = addCancellationListener(indicator, connection);
                return task.convert(connection);
            } finally {
                connection.close();
                if (future != null) future.cancel(true);
            }
        }
        //--捕获2步认证,根据异常进行跳转
        catch (CodingNetTwoFactorAuthenticationException e) {
            getTwoFactorAuthData(project, authHolder, indicator, auth);
            return runTask(project, authHolder, indicator, task);
        }
        //--登陆认证失败,根据异常进行跳转
        catch (CodingNetAuthenticationException e) {
            getValidAuthData(project, authHolder, indicator, auth);
            return runTask(project, authHolder, indicator, task);
        }
    }

    public static <T> T runTaskWithBasicAuthForHost(@NotNull Project project,
                                                    @NotNull CodingNetAuthDataHolder authHolder,
                                                    @NotNull final ProgressIndicator indicator,
                                                    @NotNull String host,
                                                    @NotNull ThrowableConvertor<CodingNetConnection, T, IOException> task) throws IOException {
        CodingNetAuthData auth = authHolder.getAuthData();
        try {
            if (auth.getAuthType() != CodingNetAuthData.AuthType.BASIC) {
                throw new CodingNetAuthenticationException("Expected basic authentication");
            }

            final CodingNetConnection connection = new CodingNetConnection(auth, true);
            ScheduledFuture<?> future = null;

            try {
                future = addCancellationListener(indicator, connection);
                return task.convert(connection);
            } finally {
                connection.close();
                if (future != null) future.cancel(true);
            }
        } catch (CodingNetTwoFactorAuthenticationException e) {
            getTwoFactorAuthData(project, authHolder, indicator, auth);
            return runTaskWithBasicAuthForHost(project, authHolder, indicator, host, task);
        } catch (CodingNetAuthenticationException e) {
            getValidBasicAuthDataForHost(project, authHolder, indicator, auth, host);
            return runTaskWithBasicAuthForHost(project, authHolder, indicator, host, task);
        }
    }

    /**
     * 根据用户认证信息
     *
     * @param project
     * @param authHolder
     * @param indicator
     * @return
     * @throws IOException
     */
    @NotNull
    private static CodingNetUserDetailed testConnection(@NotNull Project project,
                                                        @NotNull CodingNetAuthDataHolder authHolder,
                                                        @NotNull final ProgressIndicator indicator) throws IOException {
        CodingNetAuthData auth = authHolder.getAuthData();
        try {
            final CodingNetConnection connection = new CodingNetConnection(auth, true);
            ScheduledFuture<?> future = null;

            try {
                future = addCancellationListener(indicator, connection);
                return CodingNetApiUtil.getCurrentUserDetailed(connection, auth);
            } finally {
                connection.close();
                if (future != null) future.cancel(true);
            }
        } catch (CodingNetTwoFactorAuthenticationException e) {
            //--如果需要2步认证,直接返回
            return new CodingNetUserDetailed();
            //getTwoFactorAuthData(project, authHolder, indicator, auth);
            //return testConnection(project, authHolder, indicator);
        }
    }

    @NotNull
    private static ScheduledFuture<?> addCancellationListener(@NotNull Runnable run) {
        return JobScheduler.getScheduler().scheduleWithFixedDelay(run, 1000, 300, TimeUnit.MILLISECONDS);
    }

    @NotNull
    private static ScheduledFuture<?> addCancellationListener(@NotNull final ProgressIndicator indicator,
                                                              @NotNull final CodingNetConnection connection) {
        return addCancellationListener(() -> {
            if (indicator.isCanceled()) connection.abort();
        });
    }

    @NotNull
    private static ScheduledFuture<?> addCancellationListener(@NotNull final ProgressIndicator indicator,
                                                              @NotNull final Thread thread) {
        return addCancellationListener(() -> {
            if (indicator.isCanceled()) thread.interrupt();
        });
    }

    public static void getValidAuthData(@NotNull final Project project,
                                        @NotNull final CodingNetAuthDataHolder authHolder,
                                        @NotNull final ProgressIndicator indicator,
                                        @NotNull final CodingNetAuthData oldAuth) throws CodingNetOperationCanceledException {
        authHolder.runTransaction(oldAuth, () -> {
            final CodingNetAuthData[] authData = new CodingNetAuthData[1];
            final boolean[] ok = new boolean[1];
            ApplicationManager.getApplication().invokeAndWait(() -> {
                final CodingNetLoginDialog dialog = new CodingNetLoginDialog(project, oldAuth);
                DialogManager.show(dialog);
                ok[0] = dialog.isOK();

                if (ok[0]) {
                    authData[0] = dialog.getAuthData();
                    CodingNetSettings.getInstance().setAuthData(authData[0], dialog.isSavePasswordSelected());
                }
            }, indicator.getModalityState());
            if (!ok[0]) {
                throw new CodingNetOperationCanceledException("Can't get valid credentials");
            }
            return authData[0];
        });
    }

    public static void getValidBasicAuthDataForHost(@NotNull final Project project,
                                                    @NotNull final CodingNetAuthDataHolder authHolder,
                                                    @NotNull final ProgressIndicator indicator,
                                                    @NotNull final CodingNetAuthData oldAuth,
                                                    @NotNull final String host) throws CodingNetOperationCanceledException {
        authHolder.runTransaction(oldAuth, () -> {
            final CodingNetAuthData[] authData = new CodingNetAuthData[1];
            final boolean[] ok = new boolean[1];
            ApplicationManager.getApplication().invokeAndWait(() -> {
                final CodingNetLoginDialog dialog = new CodingNetBasicLoginDialog(project, oldAuth, host);
                DialogManager.show(dialog);
                ok[0] = dialog.isOK();
                if (ok[0]) {
                    authData[0] = dialog.getAuthData();

                    final CodingNetSettings settings = CodingNetSettings.getInstance();
                    if (settings.getAuthType() != CodingNetAuthData.AuthType.TOKEN) {
                        CodingNetSettings.getInstance().setAuthData(authData[0], dialog.isSavePasswordSelected());
                    }
                }
            }, indicator.getModalityState());
            if (!ok[0]) {
                throw new CodingNetOperationCanceledException("Can't get valid credentials");
            }
            return authData[0];
        });
    }

    private static void getTwoFactorAuthData(@NotNull final Project project,
                                             @NotNull final CodingNetAuthDataHolder authHolder,
                                             @NotNull final ProgressIndicator indicator,
                                             @NotNull final CodingNetAuthData oldAuth) throws CodingNetOperationCanceledException {
        authHolder.runTransaction(oldAuth, () -> {
            if (authHolder.getAuthData().getAuthType() != CodingNetAuthData.AuthType.BASIC) {
                throw new CodingNetOperationCanceledException("Two factor authentication can be used only with Login/Password");
            }

            //CodingNetApiUtil.askForTwoFactorCodeSMS(new CodingNetConnection(oldAuth, false));

            final Ref<String> codeRef = new Ref<String>();
            ApplicationManager.getApplication().invokeAndWait(() -> {
                codeRef.set(Messages.showInputDialog(project, "Authentication Code", "Coding Two-Factor Authentication", null));
            }, indicator.getModalityState());
            if (codeRef.isNull()) {
                throw new CodingNetOperationCanceledException("Can't get two factor authentication code");
            }

            CodingNetSettings settings = CodingNetSettings.getInstance();
            if (settings.getAuthType() == CodingNetAuthData.AuthType.BASIC &&
                    StringUtil.equalsIgnoreCase(settings.getLogin(), oldAuth.getBasicAuth().getLogin())) {
                settings.setValidGitAuth(false);
            }

            return oldAuth.copyWithTwoFactorCode(codeRef.get());
        });
    }

    @NotNull
    public static CodingNetAuthDataHolder getValidAuthDataHolderFromConfig(@NotNull Project project, @NotNull ProgressIndicator indicator)
            throws IOException {
        CodingNetAuthData auth = CodingNetAuthData.createFromSettings();
        CodingNetAuthDataHolder authHolder = new CodingNetAuthDataHolder(auth);
        try {
            checkAuthData(project, authHolder, indicator);
            return authHolder;
        } catch (CodingNetAuthenticationException e) {
            getValidAuthData(project, authHolder, indicator, auth);
            return authHolder;
        }
    }

    @NotNull
    public static CodingNetUserDetailed checkAuthData(@NotNull Project project,
                                                      @NotNull CodingNetAuthDataHolder authHolder,
                                                      @NotNull ProgressIndicator indicator) throws IOException {
        CodingNetAuthData auth = authHolder.getAuthData();

        if (StringUtil.isEmptyOrSpaces(auth.getHost())) {
            throw new CodingNetAuthenticationException("Target host not defined");
        }

        try {
            new URI(auth.getHost());
        } catch (URISyntaxException e) {
            throw new CodingNetAuthenticationException("Invalid host URL");
        }

        switch (auth.getAuthType()) {
            case BASIC:
                CodingNetAuthData.BasicAuth basicAuth = auth.getBasicAuth();
                assert basicAuth != null;
                if (StringUtil.isEmptyOrSpaces(basicAuth.getLogin()) || StringUtil.isEmptyOrSpaces(basicAuth.getPassword())) {
                    throw new CodingNetAuthenticationException("Empty login or password");
                }
                break;
            case TOKEN:
                CodingNetAuthData.TokenAuth tokenAuth = auth.getTokenAuth();
                assert tokenAuth != null;
                if (StringUtil.isEmptyOrSpaces(tokenAuth.getToken())) {
                    throw new CodingNetAuthenticationException("Empty token");
                }
                break;
            case ANONYMOUS:
                throw new CodingNetAuthenticationException("Anonymous connection not allowed");
        }

        return testConnection(project, authHolder, indicator);
    }

    public static <T> T computeValueInModalIO(@NotNull Project project,
                                              @NotNull String caption,
                                              @NotNull final ThrowableConvertor<ProgressIndicator, T, IOException> task) throws IOException {
        return ProgressManager.getInstance().run(new Task.WithResult<T, IOException>(project, caption, true) {
            @Override
            protected T compute(@NotNull ProgressIndicator indicator) throws IOException {
                return task.convert(indicator);
            }
        });
    }

    public static <T> T computeValueInModal(@NotNull Project project,
                                            @NotNull String caption,
                                            @NotNull final Convertor<ProgressIndicator, T> task) {
        return computeValueInModal(project, caption, true, task);
    }

    public static <T> T computeValueInModal(@NotNull Project project,
                                            @NotNull String caption,
                                            boolean canBeCancelled,
                                            @NotNull final Convertor<ProgressIndicator, T> task) {
        return ProgressManager.getInstance().run(new Task.WithResult<T, RuntimeException>(project, caption, canBeCancelled) {
            @Override
            protected T compute(@NotNull ProgressIndicator indicator) {
                return task.convert(indicator);
            }
        });
    }

    public static void computeValueInModal(@NotNull Project project,
                                           @NotNull String caption,
                                           boolean canBeCancelled,
                                           @NotNull final Consumer<ProgressIndicator> task) {
        ProgressManager.getInstance().run(new Task.WithResult<Void, RuntimeException>(project, caption, canBeCancelled) {
            @Override
            protected Void compute(@NotNull ProgressIndicator indicator) {
                task.consume(indicator);
                return null;
            }
        });
    }

    public static <T> T runInterruptable(@NotNull final ProgressIndicator indicator,
                                         @NotNull ThrowableComputable<T, IOException> task) throws IOException {
        ScheduledFuture<?> future = null;
        try {
            final Thread thread = Thread.currentThread();
            future = addCancellationListener(indicator, thread);

            return task.compute();
        } finally {
            if (future != null) future.cancel(true);
            Thread.interrupted();
        }
    }

  /*
  * Git utils
  */

    @Nullable
    public static String findCodingNetRemoteUrl(@NotNull GitRepository repository) {
        Pair<GitRemote, String> remote = findCodingNetRemote(repository);
        if (remote == null) {
            return null;
        }
        return remote.getSecond();
    }

    @Nullable
    public static Pair<GitRemote, String> findCodingNetRemote(@NotNull GitRepository repository) {
        Pair<GitRemote, String> codingNetRemote = null;
        for (GitRemote gitRemote : repository.getRemotes()) {
            for (String remoteUrl : gitRemote.getUrls()) {
                if (CodingNetUrlUtil.isCodingNetUrl(remoteUrl)) {
                    final String remoteName = gitRemote.getName();
                    if (("coding").equals(remoteName) || "origin".equals(remoteName)) {
                        return Pair.create(gitRemote, remoteUrl);
                    }
                    if (codingNetRemote == null) {
                        codingNetRemote = Pair.create(gitRemote, remoteUrl);
                    }
                    break;
                }
            }
        }
        return codingNetRemote;
    }

    @Nullable
    public static String findUpstreamRemote(@NotNull GitRepository repository) {
        for (GitRemote gitRemote : repository.getRemotes()) {
            final String remoteName = gitRemote.getName();
            if ("upstream".equals(remoteName)) {
                for (String remoteUrl : gitRemote.getUrls()) {
                    if (CodingNetUrlUtil.isCodingNetUrl(remoteUrl)) {
                        return remoteUrl;
                    }
                }
                return gitRemote.getFirstUrl();
            }
        }
        return null;
    }

    /**
     * 测试Git环境配置
     *
     * @param project
     * @return
     */
    public static boolean testGitExecutable(final Project project) {
        final GitVcsApplicationSettings settings = GitVcsApplicationSettings.getInstance();
        final String executable = settings.getPathToGit();
        final GitVersion version;
        try {
            version = GitVersion.identifyVersion(executable);
        } catch (Exception e) {
            CodingNetNotifications.showErrorDialog(project, GitBundle.getString("find.git.error.title"), e);
            return false;
        }

        if (!version.isSupported()) {
            CodingNetNotifications.showWarningDialog(project, GitBundle.message("find.git.unsupported.message", version.toString(), GitVersion.MIN),
                    GitBundle.getString("find.git.success.title"));
            return false;
        }
        return true;
    }

    public static boolean isRepositoryOnCoding(@NotNull GitRepository repository) {
        return findCodingNetRemoteUrl(repository) != null;
    }

    @NotNull
    public static String getErrorTextFromException(@NotNull Exception e) {
        if (e instanceof UnknownHostException) {
            return "Unknown host: " + e.getMessage();
        }
        return StringUtil.notNullize(e.getMessage(), "Unknown error");
    }

    @Nullable
    public static GitRepository getGitRepository(@NotNull Project project, @Nullable VirtualFile file) {
        GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
        List<GitRepository> repositories = manager.getRepositories();
        if (repositories.size() == 0) {
            return null;
        }
        if (repositories.size() == 1) {
            return repositories.get(0);
        }
        if (file != null) {
            GitRepository repository = manager.getRepositoryForFile(file);
            if (repository != null) {
                return repository;
            }
        }
        return manager.getRepositoryForFile(project.getBaseDir());
    }

    public static boolean addCodingRemote(@NotNull Project project,
                                          @NotNull GitRepository repository,
                                          @NotNull String remote,
                                          @NotNull String url) {
        final GitSimpleHandler handler = new GitSimpleHandler(project, repository.getRoot(), GitCommand.REMOTE);
        handler.setSilent(true);

        try {
            handler.addParameters("add", remote, url);
            handler.run();
            if (handler.getExitCode() != 0) {
                CodingNetNotifications.showError(project, "Can't add remote", "Failed to add Coding remote: '" + url + "'. " + handler.getStderr());
                return false;
            }
            // catch newly added remote
            repository.update();
            return true;
        } catch (VcsException e) {
            CodingNetNotifications.showError(project, "Can't add remote", e);
            return false;
        }
    }
}
