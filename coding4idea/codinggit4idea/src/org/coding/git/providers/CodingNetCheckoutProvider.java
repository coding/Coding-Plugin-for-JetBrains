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
package org.coding.git.providers;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.actions.BasicAction;
import git4idea.checkout.GitCheckoutProvider;
import git4idea.checkout.GitCloneDialog;
import git4idea.commands.Git;
import org.coding.git.api.CodingNetApiUtil;
import org.coding.git.check.CodingNetGitCloneDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.coding.git.api.CodingNetRepo;
import org.coding.git.util.CodingNetAuthDataHolder;
import org.coding.git.util.CodingNetNotifications;
import org.coding.git.util.CodingNetUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 *  Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/extensions/GithubCheckoutProvider.java
 * @author JetBrains s.r.o.
 * @author oleg
 * @author robin
 */
public class CodingNetCheckoutProvider implements CheckoutProvider {

    public CodingNetCheckoutProvider() {

    }


    @Override
    public void doCheckout(@NotNull final Project project, @Nullable final Listener listener) {
        //--检测Git环境
        if (!CodingNetUtil.testGitExecutable(project)) {
            return;
        }
        BasicAction.saveAll();

        List<CodingNetRepo> availableRepos;
        try {
            //--获取有效资源
            availableRepos = CodingNetUtil.computeValueInModalIO(project, "Access to Coding.net", indicator ->
                    CodingNetUtil.runTask(project, CodingNetAuthDataHolder.createFromSettings(), indicator, CodingNetApiUtil::getAvailableRepos));
        } catch (IOException e) {
            CodingNetNotifications.showError(project, "Couldn't get the list of Coding.net repositories", e);
            return;
        }
//    Collections.sort(availableRepos, (r1, r2) -> {
//      final int comparedOwners = r1.getUserName().compareTo(r2.getUserName());
//      return comparedOwners != 0 ? comparedOwners : r1.getName().compareTo(r2.getName());
//    });

        final CodingNetGitCloneDialog dialog = new CodingNetGitCloneDialog(project);
        dialog.cleanCacheModel();
        // Add predefined repositories to history
        //dialog.prependToHistory("-----------------------------------------------");
        for (int i = availableRepos.size() - 1; i >= 0; i--) {
            dialog.prependToHistory(availableRepos.get(i).getHttpsUrl());
        }
        if (!dialog.showAndGet()) {
            return;
        }
        dialog.rememberSettings();
        final VirtualFile destinationParent = LocalFileSystem.getInstance().findFileByIoFile(new File(dialog.getParentDirectory()));
        if (destinationParent == null) {
            return;
        }
        final String sourceRepositoryURL = dialog.getSourceRepositoryURL();
        final String directoryName = dialog.getDirectoryName();
        final String parentDirectory = dialog.getParentDirectory();

        Git git = ServiceManager.getService(Git.class);
        GitCheckoutProvider.clone(project, git, listener, destinationParent, sourceRepositoryURL, directoryName, parentDirectory);
    }

    @Override
    public String getVcsName() {
        return "Coding.net";
    }
}
