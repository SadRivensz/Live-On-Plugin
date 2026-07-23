package com.liveon.auth;

import com.liveon.auth.AccessSession;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccessSessionTest
{
    @Test
    public void staffSessionIsAuthorized()
    {
        AccessSession session = AccessSession.authorized("Example", "administrator", true, "Welcome");
        assertTrue(session.isAuthorized());
        assertTrue(session.isStaff());
    }

    @Test
    public void deniedSessionCannotBeStaff()
    {
        AccessSession session = AccessSession.denied("No clan membership");
        assertFalse(session.isAuthorized());
        assertFalse(session.isStaff());
    }
}
