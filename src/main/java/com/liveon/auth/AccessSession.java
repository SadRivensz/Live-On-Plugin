package com.liveon.auth;

public final class AccessSession
{
    private final boolean authorized;
    private final boolean staff;
    private final String rsn;
    private final String role;
    private final String loginMessage;
    private final String reason;

    private AccessSession(boolean authorized, boolean staff, String rsn, String role, String loginMessage, String reason)
    {
        this.authorized = authorized;
        this.staff = staff;
        this.rsn = rsn;
        this.role = role;
        this.loginMessage = loginMessage;
        this.reason = reason;
    }

    public static AccessSession authorized(String rsn, String role, boolean staff, String loginMessage)
    {
        return new AccessSession(true, staff, rsn, role, loginMessage, null);
    }

    public static AccessSession denied(String reason)
    {
        return new AccessSession(false, false, null, null, null, reason);
    }

    public boolean isAuthorized()
    {
        return authorized;
    }

    public boolean isStaff()
    {
        return staff;
    }

    public String getRsn()
    {
        return rsn;
    }

    public String getRole()
    {
        return role;
    }

    public String getLoginMessage()
    {
        return loginMessage;
    }

    public String getReason()
    {
        return reason;
    }
}
