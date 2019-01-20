package com.ethlo.lamebda.security;

public class UsernamePasswordCredentials
{
    private final String username;
    private final String password;

    public UsernamePasswordCredentials(final String username, final String password)
    {
        this.username = username;
        this.password = password;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public boolean matches(final String username, final String password)
    {
        return username.equals(this.username) && password.equals(this.password);
    }
}
