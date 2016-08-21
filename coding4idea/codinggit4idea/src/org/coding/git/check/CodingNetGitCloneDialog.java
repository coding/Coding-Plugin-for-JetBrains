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
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorComboBox;
import git4idea.checkout.GitCloneDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Created by robin on 16/8/17.
 */
public class CodingNetGitCloneDialog extends GitCloneDialog {

    public CodingNetGitCloneDialog(@NotNull Project project) {
        super(project, null);
    }

    public void cleanCacheModel() {
        ((EditorComboBox) getPreferredFocusedComponent()).removeAllItems();
    }

    public void rememberSettings() {

    }


}
