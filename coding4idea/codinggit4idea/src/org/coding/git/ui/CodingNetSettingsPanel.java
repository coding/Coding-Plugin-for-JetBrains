/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.coding.git.api.CodingNetApiUtil;
import org.coding.git.api.CodingNetUser;
import org.coding.git.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.coding.git.exceptions.CodingNetAuthenticationException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.io.IOException;

/**
 * @author robin
 */
public class CodingNetSettingsPanel {
    private static final String DEFAULT_PASSWORD_TEXT = "************";
    private final static String AUTH_PASSWORD = "Password";
    private final static String AUTH_TOKEN = "Token";

    private static final Logger LOG = CodingNetUtil.LOG;

    private final CodingNetSettings mySettings;

    private JTextField myLoginTextField;
    private JPasswordField myPasswordField;
    private JPasswordField myTokenField; // look at createUIComponents() to understand
    private JTextPane mySignupTextField;
    private JPanel myPane;
    private JButton myTestButton;
    private JTextField myHostTextField;
    private ComboBox myAuthTypeComboBox;
    private JPanel myCardPanel;
    private JBLabel myAuthTypeLabel;
    private JSpinner myTimeoutSpinner;
    private JButton myCreateTokenButton;
    //private JBCheckBox myCloneUsingSshCheckBox;

    private boolean myCredentialsModified;

    public CodingNetSettingsPanel() {
        mySettings = CodingNetSettings.getInstance();
        mySignupTextField.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(final HyperlinkEvent e) {
                BrowserUtil.browse(e.getURL());
            }
        });
        mySignupTextField.setText("<html>Do not have an account at coding.net? <a href=\"https://coding.net\">" + "Sign up" + "</a></html>");
        mySignupTextField.setBackground(myPane.getBackground());
        mySignupTextField.setCursor(new Cursor(Cursor.HAND_CURSOR));
        myAuthTypeLabel.setBorder(JBUI.Borders.emptyLeft(10));
        myAuthTypeComboBox.addItem(AUTH_PASSWORD);
        //myAuthTypeComboBox.addItem(AUTH_TOKEN);

        final Project project = ProjectManager.getInstance().getDefaultProject();

        myTestButton.addActionListener(e -> {
            try {
                CodingNetAuthData auth = getAuthData();
                auth.getBasicAuth().setCode("");
                CodingNetUser user = CodingNetUtil.computeValueInModalIO(project, "Access to Coding", indicator ->
                        CodingNetUtil.checkAuthData(project, new CodingNetAuthDataHolder(auth), indicator));

                if (CodingNetAuthData.AuthType.TOKEN.equals(getAuthType())) {
                    CodingNetNotifications.showInfoDialog(myPane, "Success", "Connection successful for user " + user.getLogin());
                } else {
                    CodingNetNotifications.showInfoDialog(myPane, "Success", "Connection successful");
                }
            } catch (CodingNetAuthenticationException ex) {
                CodingNetNotifications.showErrorDialog(myPane, "Login Failure", "Can't login using given credentials: ", ex);
            } catch (IOException ex) {
                CodingNetNotifications.showErrorDialog(myPane, "Login Failure", "Can't login: ", ex);
            }
        });

//        myCreateTokenButton.addActionListener(e -> {
//            try {
//                String newToken = CodingNetUtil.computeValueInModalIO(project, "Access to Coding", indicator ->
//                        CodingNetUtil.runTaskWithBasicAuthForHost(project, CodingNetAuthDataHolder.createFromSettings(), indicator, getHost(), connection ->
//                                CodingNetApiUtil.getMasterToken(connection, "IntelliJ plugin")));
//                myPasswordField.setText(newToken);
//            } catch (IOException ex) {
//                CodingNetNotifications.showErrorDialog(myPane, "Can't Create API Token", ex);
//            }
//        });

        myPasswordField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                myCredentialsModified = true;
            }
        });

        DocumentListener passwordEraser = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                if (!myCredentialsModified) {
                    erasePassword();
                }
            }
        };
        myHostTextField.getDocument().addDocumentListener(passwordEraser);
        myLoginTextField.getDocument().addDocumentListener(passwordEraser);

        myPasswordField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!myCredentialsModified && !getPassword().isEmpty()) {
                    erasePassword();
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });

        myAuthTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String item = e.getItem().toString();
                if (AUTH_PASSWORD.equals(item)) {
                    ((CardLayout) myCardPanel.getLayout()).show(myCardPanel, AUTH_PASSWORD);
                } else if (AUTH_TOKEN.equals(item)) {
                    ((CardLayout) myCardPanel.getLayout()).show(myCardPanel, AUTH_TOKEN);
                }
                erasePassword();
            }
        });

        reset();
    }

    private void erasePassword() {
        setPassword("");
        myCredentialsModified = true;
    }

    public JComponent getPanel() {
        return myPane;
    }

    @NotNull
    public String getHost() {
        return myHostTextField.getText().trim();
    }

    @NotNull
    public String getLogin() {
        return myLoginTextField.getText().trim();
    }

    public void setHost(@NotNull final String host) {
        myHostTextField.setText(host);
    }

    public void setLogin(@Nullable final String login) {
        myLoginTextField.setText(login);
    }

    @NotNull
    private String getPassword() {
        return String.valueOf(myPasswordField.getPassword());
    }

    private void setPassword(@NotNull final String password) {
        // Show password as blank if password is empty
        myPasswordField.setText(StringUtil.isEmpty(password) ? null : password);
    }

    @NotNull
    public CodingNetAuthData.AuthType getAuthType() {
        Object selected = myAuthTypeComboBox.getSelectedItem();
        if (AUTH_PASSWORD.equals(selected)) return CodingNetAuthData.AuthType.BASIC;
        if (AUTH_TOKEN.equals(selected)) return CodingNetAuthData.AuthType.TOKEN;
        LOG.error("CodingNetSettingsPanel: illegal selection: basic AuthType returned", selected.toString());
        return CodingNetAuthData.AuthType.BASIC;
    }

    public void setAuthType(@NotNull final CodingNetAuthData.AuthType type) {
        switch (type) {
            case BASIC:
                myAuthTypeComboBox.setSelectedItem(AUTH_PASSWORD);
                break;
            case TOKEN:
                myAuthTypeComboBox.setSelectedItem(AUTH_TOKEN);
                break;
            case ANONYMOUS:
            default:
                myAuthTypeComboBox.setSelectedItem(AUTH_PASSWORD);
        }
    }

    @NotNull
    public CodingNetAuthData getAuthData() {
        if (!myCredentialsModified) {
            return mySettings.getAuthData();
        }
        Object selected = myAuthTypeComboBox.getSelectedItem();
        if (AUTH_PASSWORD.equals(selected))
            return CodingNetAuthData.createBasicAuth(getHost(), getLogin(), getPassword());
        if (AUTH_TOKEN.equals(selected))
            return CodingNetAuthData.createTokenAuth(getHost(), StringUtil.trim(getPassword()));
        LOG.error("CodingNetSettingsPanel: illegal selection: anonymous AuthData created", selected.toString());
        return CodingNetAuthData.createAnonymous(getHost());
    }

    public void setConnectionTimeout(int timeout) {
        myTimeoutSpinner.setValue(Integer.valueOf(timeout));
    }

    public int getConnectionTimeout() {
        return ((SpinnerNumberModel) myTimeoutSpinner.getModel()).getNumber().intValue();
    }

    public void reset() {
        setHost(mySettings.getHost());
        setLogin(mySettings.getLogin());
        setPassword(mySettings.isAuthConfigured() ? DEFAULT_PASSWORD_TEXT : "");
        setAuthType(mySettings.getAuthType());
        setConnectionTimeout(mySettings.getConnectionTimeout());
        //myCloneUsingSshCheckBox.setSelected(mySettings.isCloneGitUsingSsh());
        resetCredentialsModification();
    }

    public void apply() {
        if (myCredentialsModified) {
            mySettings.setAuthData(getAuthData(), true);
        }
        mySettings.setConnectionTimeout(getConnectionTimeout());
        //mySettings.setCloneGitUsingSsh(myCloneUsingSshCheckBox.isSelected());
        resetCredentialsModification();
    }

    public boolean isModified() {
        return myCredentialsModified ||
                !Comparing.equal(mySettings.getHost(), getHost()) ||
                !Comparing.equal(mySettings.getConnectionTimeout(), getConnectionTimeout())
                //||
                //!Comparing.equal(mySettings.isCloneGitUsingSsh(), myCloneUsingSshCheckBox.isSelected())
                ;
    }

    public void resetCredentialsModification() {
        myCredentialsModified = false;
    }

    private void createUIComponents() {
        Document doc = new PlainDocument();
        myPasswordField = new JPasswordField(doc, null, 0);
        myTokenField = new JPasswordField(doc, null, 0);
        myTimeoutSpinner =
                new JSpinner(new SpinnerNumberModel(Integer.valueOf(5000), Integer.valueOf(0), Integer.valueOf(60000), Integer.valueOf(500)));
    }
}
