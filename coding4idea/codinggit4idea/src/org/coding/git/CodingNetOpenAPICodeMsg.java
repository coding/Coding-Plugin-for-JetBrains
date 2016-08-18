package org.coding.git;

import com.google.gson.annotations.SerializedName;

/**
 * @author robin
 */
public enum CodingNetOpenAPICodeMsg {

    USER_PASSWORD_NO_CORRECT(1, UserPasswordNoCorrect.class),
    NEED_VERIFICATION_CODE(903, NeedVerificationCode.class),
    NO_LOGIN(1000, NoLogin.class),
    NO_EXIST_USER(1001, NoExistUser.class),
    LOGIN_EXPIRED(1029, LoginExpired.class),
    AUTH_ERROR(1402, AuthError.class),
    NEED_TWO_FACTOR_AUTH_CODE(3205, null),
    TWO_FACTOR_AUTH_CODE_REQUIRED(3209, null),
    USER_LOCKED(1009, UserLocked.class);


    private int code;


    private Class clazz;

    CodingNetOpenAPICodeMsg(int code, Class clazz) {
        this.code = code;
        this.clazz = clazz;

    }

    public Class getClazz() {
        return clazz;
    }

    public int getCode() {
        return code;
    }

    public interface ICodingNetOpenAPICodeMsg {
        String getMessage();
    }


    /**
     * 2步认证失败
     */
    class AuthError implements ICodingNetOpenAPICodeMsg {
        int code;
        AuthErrorCodeMsg msg;

        @Override
        public String getMessage() {
            return msg.toString();
        }

        class AuthErrorCodeMsg {
            @SerializedName("auth_error")
            String authError;

            @Override
            public String toString() {
                return authError;
            }
        }
    }

    /**
     * 用户锁定
     */
    class UserLocked implements ICodingNetOpenAPICodeMsg {
        int code;
        UserLockedCodeMsg msg;

        @Override
        public String getMessage() {
            return msg.toString();
        }

        class UserLockedCodeMsg {
            @SerializedName("account")
            String account;

            @Override
            public String toString() {
                return account;
            }
        }
    }


    /**
     * 用户登陆超时
     */
    class LoginExpired implements ICodingNetOpenAPICodeMsg {
        int code;
        LoginExpiredCodeMsg msg;

        @Override
        public String getMessage() {
            return msg.toString();
        }

        class LoginExpiredCodeMsg {
            @SerializedName("user_login_status_expired")
            String loginStatusExpired;

            @Override
            public String toString() {
                return loginStatusExpired;
            }
        }
    }

    class UserPasswordNoCorrect implements ICodingNetOpenAPICodeMsg {
        int code;
        UserPasswordNoCorrectMsg msg;

        @Override
        public String getMessage() {
            return msg.toString();
        }
    }

    class UserPasswordNoCorrectMsg {
        String password;

        @Override
        public String toString() {
            return password;
        }
    }


    class NeedVerificationCode implements ICodingNetOpenAPICodeMsg {
        int code;
        NeedVerificationCodeMsg msg;

        @Override
        public String getMessage() {
            return "登陆次数已超过最大上限,请稍后再试.";
        }
    }

    class NeedVerificationCodeMsg {
        @SerializedName("j_captcha")
        String captcha;

        @Override
        public String toString() {
            return captcha;
        }
    }


    /**
     * 用户不存在
     */
    class NoExistUser implements ICodingNetOpenAPICodeMsg {
        int code;
        NoExistUser msg;

        @Override
        public String getMessage() {
            return msg.toString();
        }

    }

    class NoExistUserMsg {
        String account;

        @Override
        public String toString() {
            return account;
        }
    }

    /**
     * 未登陆
     */
    class NoLogin implements ICodingNetOpenAPICodeMsg {
        int code;
        NoLoginMsg msg;

        @Override
        public String getMessage() {
            return msg.toString();
        }
    }

    class NoLoginMsg {
        @SerializedName("user_not_login")
        String userNotLogin;

        @Override
        public String toString() {
            return userNotLogin;
        }
    }


}
