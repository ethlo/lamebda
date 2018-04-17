package com.ethlo.lamebda.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ethlo.lamebda.ClassResourceLoader;
import com.ethlo.lamebda.FunctionManager;

public class ServletFunctionManager extends FunctionManager<HttpServletRequest, HttpServletResponse, ServletServerFunction>
{
    public ServletFunctionManager(ClassResourceLoader<HttpServletRequest, HttpServletResponse, ServletServerFunction> loader)
    {
        super(loader);
    }    
}
