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

import com.intellij.notification.NotificationListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.coding.git.exceptions.CodingNetOperationCanceledException;

import java.awt.*;

import static org.coding.git.util.CodingNetUtil.getErrorTextFromException;

public class CodingNetNotifications {
  private static final Logger LOG = CodingNetUtil.LOG;

  private static boolean isOperationCanceled(@NotNull Exception e) {
    return e instanceof CodingNetOperationCanceledException ||
           e instanceof ProcessCanceledException;
  }

  public static void showInfo(@NotNull Project project, @NotNull String title, @NotNull String message) {
    LOG.info(title + "; " + message);
    VcsNotifier.getInstance(project).notifyImportantInfo(title, message);
  }

  public static void showWarning(@NotNull Project project, @NotNull String title, @NotNull String message) {
    LOG.info(title + "; " + message);
    VcsNotifier.getInstance(project).notifyImportantWarning(title, message);
  }

  public static void showWarning(@NotNull Project project, @NotNull String title, @NotNull Exception e) {
    LOG.info(title + "; ", e);
    if (isOperationCanceled(e)) return;
    VcsNotifier.getInstance(project).notifyImportantWarning(title, getErrorTextFromException(e));
  }

  public static void showError(@NotNull Project project, @NotNull String title, @NotNull String message) {
    LOG.info(title + "; " + message);
    VcsNotifier.getInstance(project).notifyError(title, message);
  }

  public static void showError(@NotNull Project project, @NotNull String title, @NotNull String message, @NotNull String logDetails) {
    LOG.warn(title + "; " + message + "; " + logDetails);
    VcsNotifier.getInstance(project).notifyError(title, message);
  }

  public static void showError(@NotNull Project project, @NotNull String title, @NotNull Exception e) {
    LOG.warn(title + "; ", e);
    if (isOperationCanceled(e)) return;
    VcsNotifier.getInstance(project).notifyError(title, getErrorTextFromException(e));
  }

  public static void showInfoURL(@NotNull Project project, @NotNull String title, @NotNull String message, @NotNull String url) {
    LOG.info(title + "; " + message + "; " + url);
    VcsNotifier.getInstance(project)
      .notifyImportantInfo(title, "<a href='" + url + "'>" + message + "</a>", NotificationListener.URL_OPENING_LISTENER);
  }

  public static void showWarningURL(@NotNull Project project,
                                    @NotNull String title,
                                    @NotNull String prefix,
                                    @NotNull String highlight,
                                    @NotNull String postfix,
                                    @NotNull String url) {
    LOG.info(title + "; " + prefix + highlight + postfix + "; " + url);
    VcsNotifier.getInstance(project).notifyImportantWarning(title, prefix + "<a href='" + url + "'>" + highlight + "</a>" + postfix,
                                                            NotificationListener.URL_OPENING_LISTENER);
  }

  public static void showErrorURL(@NotNull Project project,
                                  @NotNull String title,
                                  @NotNull String prefix,
                                  @NotNull String highlight,
                                  @NotNull String postfix,
                                  @NotNull String url) {
    LOG.info(title + "; " + prefix + highlight + postfix + "; " + url);
    VcsNotifier.getInstance(project).notifyError(title, prefix + "<a href='" + url + "'>" + highlight + "</a>" + postfix,
                                                 NotificationListener.URL_OPENING_LISTENER);
  }

  public static void showInfoDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
    LOG.info(title + "; " + message);
    Messages.showInfoMessage(project, message, title);
  }

  public static void showInfoDialog(@NotNull Component component, @NotNull String title, @NotNull String message) {
    LOG.info(title + "; " + message);
    Messages.showInfoMessage(component, message, title);
  }

  public static void showWarningDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
    LOG.info(title + "; " + message);
    Messages.showWarningDialog(project, message, title);
  }

  public static void showWarningDialog(@NotNull Component component, @NotNull String title, @NotNull String message) {
    LOG.info(title + "; " + message);
    Messages.showWarningDialog(component, message, title);
  }

  public static void showErrorDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
    LOG.info(title + "; " + message);
    Messages.showErrorDialog(project, message, title);
  }

  public static void showErrorDialog(@Nullable Project project, @NotNull String title, @NotNull Exception e) {
    LOG.warn(title, e);
    if (isOperationCanceled(e)) return;
    Messages.showErrorDialog(project, getErrorTextFromException(e), title);
  }

  public static void showErrorDialog(@NotNull Component component, @NotNull String title, @NotNull Exception e) {
    LOG.info(title, e);
    if (isOperationCanceled(e)) return;
    Messages.showErrorDialog(component, getErrorTextFromException(e), title);
  }

  public static void showErrorDialog(@NotNull Component component, @NotNull String title, @NotNull String prefix, @NotNull Exception e) {
    LOG.info(title, e);
    if (isOperationCanceled(e)) return;
    Messages.showErrorDialog(component, prefix + getErrorTextFromException(e), title);
  }

  @Messages.YesNoResult
  public static boolean showYesNoDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
    return Messages.YES == Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon());
  }

  @Messages.YesNoResult
  public static boolean showYesNoDialog(@Nullable Project project,
                                        @NotNull String title,
                                        @NotNull String message,
                                        @NotNull DialogWrapper.DoNotAskOption doNotAskOption) {
    return Messages.YES == Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon(), doNotAskOption);
  }
}
