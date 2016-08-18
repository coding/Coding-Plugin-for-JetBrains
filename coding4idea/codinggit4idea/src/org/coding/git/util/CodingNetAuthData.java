/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.openapi.util.text.StringUtil;
import org.coding.git.api.CodingNetApiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author robin
 */
public class CodingNetAuthData {
    public enum AuthType {BASIC, TOKEN, ANONYMOUS}

    @NotNull
    private final AuthType myAuthType;
    @NotNull
    private final String myHost;
    @Nullable
    private final BasicAuth myBasicAuth;
    @Nullable
    private final TokenAuth myTokenAuth;
    private final boolean myUseProxy;

    private CodingNetAuthData(@NotNull AuthType authType,
                              @NotNull String host,
                              @Nullable BasicAuth basicAuth,
                              @Nullable TokenAuth tokenAuth,
                              boolean useProxy) {
        myAuthType = authType;
        myHost = host;
        myBasicAuth = basicAuth;
        myTokenAuth = tokenAuth;
        myUseProxy = useProxy;
    }

    public static CodingNetAuthData createFromSettings() {
        return CodingNetSettings.getInstance().getAuthData();
    }

    public static CodingNetAuthData createAnonymous() {
        return createAnonymous(CodingNetApiUtil.DEFAULT_CODING_HOST);
    }

    public static CodingNetAuthData createAnonymous(@NotNull String host) {
        return new CodingNetAuthData(AuthType.ANONYMOUS, host, null, null, true);
    }

    public static CodingNetAuthData createBasicAuth(@NotNull String host, @NotNull String login, @NotNull String password) {
        return new CodingNetAuthData(AuthType.BASIC, host, new BasicAuth(login, password), null, true);
    }

    /**
     * 两步认证对象构建
     *
     * @param host
     * @param login
     * @param password
     * @param code
     * @return
     */
    public static CodingNetAuthData createBasicAuthTF(@NotNull String host,
                                                      @NotNull String login,
                                                      @NotNull String password,
                                                      @NotNull String sid,
                                                      @NotNull String code) {
        return new CodingNetAuthData(AuthType.BASIC, host, new BasicAuth(login, password, sid, code), null, true);
    }

    public static CodingNetAuthData createTokenAuth(@NotNull String host, @NotNull String token) {
        return new CodingNetAuthData(AuthType.TOKEN, host, null, new TokenAuth(token), true);
    }

    public static CodingNetAuthData createTokenAuth(@NotNull String host, @NotNull String token, boolean useProxy) {
        return new CodingNetAuthData(AuthType.TOKEN, host, null, new TokenAuth(token), useProxy);
    }

    @NotNull
    public AuthType getAuthType() {
        return myAuthType;
    }

    @NotNull
    public String getHost() {
        return myHost;
    }

    @Nullable
    public BasicAuth getBasicAuth() {
        return myBasicAuth;
    }

    @Nullable
    public TokenAuth getTokenAuth() {
        return myTokenAuth;
    }

    public boolean isUseProxy() {
        return myUseProxy;
    }

    @NotNull
    public CodingNetAuthData copyWithTwoFactorCode(@NotNull String code) {
        if (myBasicAuth == null) {
            throw new IllegalStateException("Two factor authentication can be used only with Login/Password");
        }

        return createBasicAuthTF(getHost(), myBasicAuth.getLogin(), myBasicAuth.getPassword(), myBasicAuth.getSid(), code);
    }

    /**
     * 用户名密码基本认证对象
     */
    public static class BasicAuth {
        @NotNull
        private final String myLogin;
        @NotNull
        private final String myPassword;
        @Nullable
        private String mySid;
        @Nullable
        private String myAuthCode;


        private BasicAuth(@NotNull String login, @NotNull String password) {
            this(login, password, null);
        }

        /**
         * @param login
         * @param password
         * @param sid
         */
        private BasicAuth(@NotNull String login, @NotNull String password, @Nullable String sid) {
            myLogin = login;
            myPassword = password;
            mySid = sid;
        }

        private BasicAuth(@NotNull String login, @NotNull String password, @Nullable String sid, @Nullable String authCode) {
            this(login, password, sid);
            myAuthCode = authCode;
        }


        @NotNull
        public String getLogin() {
            return myLogin;
        }

        @NotNull
        public String getPassword() {
            return myPassword;
        }

        @Nullable
        public String getSid() {
            return mySid;
        }

        public void setSid(@Nullable String sid) {
            this.mySid = sid;
        }

        @Nullable
        public String getAuthCode() {
            return myAuthCode;
        }

        public void setAuthCode(@Nullable String authCode) {
            this.myAuthCode = myAuthCode;
        }
    }


    /**
     * Token认证
     */
    public static class TokenAuth {
        @NotNull
        private final String myToken;

        private TokenAuth(@NotNull String token) {
            myToken = StringUtil.trim(token);
        }

        @NotNull
        public String getToken() {
            return myToken;
        }
    }
}
