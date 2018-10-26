package com.ethlo.lamebda;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import org.junit.Test;

import com.ethlo.lamebda.loaders.FileSystemClassResourceLoader;

public class FunctionLoaderTest
{
    private final File basepath = new File(System.getProperty("java.io.tmpdir"), "lamebda-unit-test");
    private FunctionManagerImpl functionManager;

    public FunctionLoaderTest() throws IOException
    {
        if (basepath.exists())
        {
            deleteDir(basepath.getCanonicalPath());
        }
        assertThat(basepath.mkdirs()).isTrue();

        functionManager = new FunctionManagerImpl(new FileSystemClassResourceLoader(f -> f, basepath.getAbsolutePath()), new FunctionManagerConfig());
    }

    @Test
    public void testLoadOnCreate() throws IOException, InterruptedException
    {
        move("Correct.groovy");
        Thread.sleep(1_000);
        final Map<String, ServerFunction> functions = functionManager.getFunctions();
        assertThat(functions.keySet()).containsExactly("Correct");
    }

    @Test
    public void testLoadOnModification() throws Exception
    {
        move("Correct.groovy");
        Thread.sleep(1_000);
        move("Correct.groovy");
        Thread.sleep(1_000);
    }

    @Test
    public void testUnloadOnRemoval() throws Exception
    {
        move("Correct.groovy");
        Thread.sleep(1_000);
        remove("Correct.groovy");
        Thread.sleep(1_000);
        final Map<String, ServerFunction> functions = functionManager.getFunctions();
        assertThat(functions.keySet()).doesNotContain("Correct");
    }

    @Test
    public void testUnloadOnError() throws Exception
    {
        final Path target = move("Correct.groovy");
        Thread.sleep(1_000);
        Files.copy(Paths.get("src/test/groovy/Incorrect.groovy"), target, StandardCopyOption.REPLACE_EXISTING);
        Thread.sleep(1_000);
        final Map<String, ServerFunction> functions = functionManager.getFunctions();
        assertThat(functions.keySet()).doesNotContain("Correct");
    }

    private Path move(final String name) throws IOException
    {
        return Files.copy(Paths.get("src/test/groovy", name), Paths.get(basepath.getCanonicalPath(), name), StandardCopyOption.REPLACE_EXISTING);
    }

    private void remove(final String name) throws IOException
    {
        final Path p = Paths.get(basepath.getCanonicalPath(), name);
        if (p.toFile().exists())
        {
            Files.delete(p);
        }
    }

    private void deleteDir(String dir) throws IOException
    {
        Path directory = Paths.get(dir);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
