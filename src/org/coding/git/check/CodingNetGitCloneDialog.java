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
package org.coding.git.check;

import com.intellij.dvcs.DvcsRememberedInputs;
import com.intellij.dvcs.hosting.RepositoryHostingService;
import com.intellij.dvcs.hosting.RepositoryListLoader;
import com.intellij.dvcs.hosting.RepositoryListLoadingException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorComboBox;
import com.intellij.ui.TextFieldWithAutoCompletion;
import git4idea.checkout.GitCloneDialog;
import org.coding.git.api.CodingNetRepo;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by robin on 16/8/17.
 */
public class CodingNetGitCloneDialog extends GitCloneDialog {

    private List<CodingNetRepo> availableRepos;

    public CodingNetGitCloneDialog(@NotNull Project project) {
        super(project, null);
    }

    public CodingNetGitCloneDialog(Project project, List<CodingNetRepo> availableRepos) {
        this(project);
        this.availableRepos = availableRepos;
    }

    public void cleanCacheModel() {
        JComponent preferredFocusedComponent = getPreferredFocusedComponent();
        if (preferredFocusedComponent instanceof EditorComboBox) {
            ((EditorComboBox) getPreferredFocusedComponent()).removeAllItems();
            return;
        }
        if (preferredFocusedComponent instanceof TextFieldWithAutoCompletion) {
            TextFieldWithAutoCompletion textFieldWithAutoCompletion = (TextFieldWithAutoCompletion) preferredFocusedComponent;
            textFieldWithAutoCompletion.removeAll();
        }
    }

    public void rememberSettings() {

    }

    @NotNull
    @Override
    protected Collection<RepositoryHostingService> getRepositoryHostingServices() {
        return Collections.singletonList(new CodingNetRepositoryHostingService());
    }

    public class CodingNetRepositoryHostingService implements RepositoryHostingService, RepositoryListLoader {

        @NotNull
        @Override
        public String getServiceDisplayName() {
            return "Coding.net";
        }

        @NotNull
        @Override
        public RepositoryListLoader getRepositoryListLoader(@NotNull Project project) {
            return this;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public boolean enable() {
            return true;
        }

        @NotNull
        @Override
        public List<String> getAvailableRepositories(@NotNull ProgressIndicator progressIndicator) throws RepositoryListLoadingException {
            return availableRepos.stream().map(CodingNetRepo::getHttpsUrl).collect(Collectors.toList());
        }
    }
}
