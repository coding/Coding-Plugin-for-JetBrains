/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.openapi.util.ThrowableComputable;
import org.jetbrains.annotations.NotNull;


/**
 *
 * 认证数据信息设置管理
 *
 * @author robin
 *
 */
public class CodingNetAuthDataHolder {
  @NotNull private CodingNetAuthData myAuthData;

  public CodingNetAuthDataHolder(@NotNull CodingNetAuthData auth) {
    myAuthData = auth;
  }

  @NotNull
  public synchronized CodingNetAuthData getAuthData() {
    return myAuthData;
  }

  public synchronized <T extends Throwable> void runTransaction(@NotNull CodingNetAuthData expected,
                                                                @NotNull ThrowableComputable<CodingNetAuthData, T> task) throws T {
    if (expected != myAuthData) {
      return;
    }

    myAuthData = task.compute();
  }

  public static CodingNetAuthDataHolder createFromSettings() {
    return new CodingNetAuthDataHolder(CodingNetSettings.getInstance().getAuthData());
  }
}
