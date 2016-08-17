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
