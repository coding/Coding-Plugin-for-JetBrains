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
package org.coding.git.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.coding.git.util.CodingNetSettings;
import org.coding.git.util.CodingNetUtil;
import org.jetbrains.annotations.NotNull;
import org.coding.git.util.CodingNetAuthData;
import org.coding.git.util.CodingNetAuthDataHolder;

import javax.swing.*;
import java.io.IOException;

/**
 * @author robin
 *  Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubLoginDialog.java
 * @author JetBrains s.r.o.
 * @author oleg
 * @date 10/20/10
 */
public class CodingNetLoginDialog extends DialogWrapper {

  protected static final Logger LOG = CodingNetUtil.LOG;

  protected final CodingNetLoginPanel myCodingNetLoginPanel;
  protected final CodingNetSettings mySettings;

  protected final Project myProject;

  protected CodingNetAuthData myAuthData;

  public CodingNetLoginDialog(@NotNull final Project project, @NotNull CodingNetAuthData oldAuthData) {
    super(project, true);
    myProject = project;

    myCodingNetLoginPanel = new CodingNetLoginPanel(this);

    myCodingNetLoginPanel.setHost(oldAuthData.getHost());
    myCodingNetLoginPanel.setAuthType(oldAuthData.getAuthType());
    CodingNetAuthData.BasicAuth basicAuth = oldAuthData.getBasicAuth();
    if (basicAuth != null) {
      myCodingNetLoginPanel.setLogin(basicAuth.getLogin());
    }

    mySettings = CodingNetSettings.getInstance();
    if (mySettings.isSavePasswordMakesSense()) {
      myCodingNetLoginPanel.setSavePasswordSelected(mySettings.isSavePassword());
    }
    else {
      myCodingNetLoginPanel.setSavePasswordVisibleEnabled(false);
    }

    setTitle("Login to coding.net");
    setOKButtonText("Login");
    init();
  }

  @NotNull
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
  }

  @Override
  protected JComponent createCenterPanel() {
    return myCodingNetLoginPanel.getPanel();
  }

  @Override
  protected String getHelpId() {
    return "login_to_coding";
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myCodingNetLoginPanel.getPreferableFocusComponent();
  }

  @Override
  protected void doOKAction() {
    final CodingNetAuthDataHolder authHolder = new CodingNetAuthDataHolder(myCodingNetLoginPanel.getAuthData());
    try {
      CodingNetUtil.computeValueInModalIO(myProject, "Access to Coding.net", indicator ->
        CodingNetUtil.checkAuthData(myProject, authHolder, indicator));

      myAuthData = authHolder.getAuthData();

      if (mySettings.isSavePasswordMakesSense()) {
        mySettings.setSavePassword(myCodingNetLoginPanel.isSavePasswordSelected());
      }
      super.doOKAction();
    }
    catch (IOException e) {
      LOG.info(e);
      setErrorText("Can't login: " + CodingNetUtil.getErrorTextFromException(e));
    }
  }

  public boolean isSavePasswordSelected() {
    return myCodingNetLoginPanel.isSavePasswordSelected();
  }

  @NotNull
  public CodingNetAuthData getAuthData() {
    if (myAuthData == null) {
      throw new IllegalStateException("AuthData is not set");
    }
    return myAuthData;
  }

  public void clearErrors() {
    setErrorText(null);
  }
}