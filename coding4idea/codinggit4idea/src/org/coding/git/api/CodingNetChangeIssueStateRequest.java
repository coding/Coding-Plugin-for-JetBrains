package org.coding.git.api;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class CodingNetChangeIssueStateRequest {
  @NotNull private final String state;

  public CodingNetChangeIssueStateRequest(@NotNull String state) {
    this.state = state;
  }
}
