package com.ethlo.lamebda.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ethlo.lamebda.function.ServerFunction;

public interface ServletServerFunction extends ServerFunction<HttpServletRequest, HttpServletResponse>
{
   
}
