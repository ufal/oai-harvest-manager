package nl.mpi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class Utilities {
    public static void deleteRecursive(Path dir) throws IOException {
        if(Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    public static String getConfig(String configResource) throws URISyntaxException {
        final URL resourceURL = Utilities.class.getClassLoader().getResource(configResource);
        return Paths.get(resourceURL.toURI()).toAbsolutePath().toString();
    }
}
