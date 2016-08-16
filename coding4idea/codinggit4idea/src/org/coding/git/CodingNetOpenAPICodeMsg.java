package org.coding.git;

import com.google.gson.annotations.SerializedName;

/**
 * @author robin
 */
public enum CodingNetOpenAPICodeMsg {

    NO_LOGIN(1000, NoLogin.class),
    NO_EXIST_USER(1001,NoExistUser.class),
    NEED_VERIFICATION_CODE(903,NeedVerificationCode.class),
    USER_LOCKED(1009,UserLocked.class),
    USER_PASSWORD_NO_CORRECT(1,UserPasswordNoCorrect.class);


    private int code;

    private String message;

    private Class clazz;

    private CodingNetOpenAPICodeMsg(int code, Class clazz) {
        this.code = code;
        this.clazz=clazz;

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

    class UserLocked implements ICodingNetOpenAPICodeMsg{
        int code;
        UserLockedCodeMsg msg;

        @Override
        public String getMessage() {
            return msg.toString();
        }
    }

    class UserLockedCodeMsg {
        @SerializedName("account")
        String account;

        @Override
        public String toString() {
            return account;
        }
    }

    class UserPasswordNoCorrect implements ICodingNetOpenAPICodeMsg{
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


    class NeedVerificationCode implements ICodingNetOpenAPICodeMsg{
        int code;
        NeedVerificationCodeMsg msg;

        @Override
        public String getMessage() {
            return msg.toString();
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
    class NoExistUser implements ICodingNetOpenAPICodeMsg{
        int code;
        NoExistUser msg;

        @Override
        public String getMessage() {
            return msg.toString();
        }

    }

    class NoExistUserMsg{
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
