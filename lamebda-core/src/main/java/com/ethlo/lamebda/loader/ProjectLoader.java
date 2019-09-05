package com.ethlo.lamebda.loader;

import java.nio.file.Path;
import java.util.List;

public interface ProjectLoader
{
    List<String> getProjectIds();

    Path init();
}
