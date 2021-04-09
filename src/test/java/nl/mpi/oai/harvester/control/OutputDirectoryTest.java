package nl.mpi.oai.harvester.control;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OutputDirectoryTest {


    @Test
    public void placeNewFileWithLimitCreatesMultipleSubdirectories() throws IOException {
        final Path tmp = Files.createTempDirectory("test-");
        final OutputDirectory o = new OutputDirectory(tmp, 1);
        o.placeNewFile("first");
        o.placeNewFile("second");
        final List<Path> dirs = Files.list(tmp).collect(Collectors.toList());
        assertEquals(2, dirs.size());
        assertTrue(Files.isDirectory(dirs.get(0)));
        nl.mpi.Utilities.deleteRecursive(tmp);
    }
}
