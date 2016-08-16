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
