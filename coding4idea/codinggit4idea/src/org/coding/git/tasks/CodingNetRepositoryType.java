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
package org.coding.git.tasks;

import com.intellij.openapi.project.Project;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.TaskState;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.tasks.impl.BaseRepositoryType;
import com.intellij.util.Consumer;
import icons.TasksIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;


/**
 * @author robin
 * Coding Git仓库
 */
public class CodingNetRepositoryType extends BaseRepositoryType<CodingNetRepository> {

  @NotNull
  @Override
  public String getName() {
    return "Coding";
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return TasksIcons.Github;
  }

  @NotNull
  @Override
  public TaskRepository createRepository() {
    return new CodingNetRepository(this);
  }

  @Override
  public Class<CodingNetRepository> getRepositoryClass() {
    return CodingNetRepository.class;
  }

  @NotNull
  @Override
  public TaskRepositoryEditor createEditor(CodingNetRepository repository,
                                           Project project,
                                           Consumer<CodingNetRepository> changeListener) {
    return new CodingNetRepositoryEditor(project, repository, changeListener);
  }

  public EnumSet<TaskState> getPossibleTaskStates() {
    return EnumSet.of(TaskState.OPEN, TaskState.RESOLVED);
  }

}
