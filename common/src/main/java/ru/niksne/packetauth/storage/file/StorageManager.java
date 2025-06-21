package ru.niksne.packetauth.storage.file;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class StorageManager<T> {
    @NotNull
    private final Class<T> type;
    @NotNull
    private final Path filePath;
    @NotNull
    private final String defaultFilePath;
    @NotNull
    private T storage;

    public StorageManager(
            @NotNull
            Class<T> type,
            @NotNull
            Path configDir,
            @NotNull
            String fileName
    ) {
        this.type = type;
        String base;
        try {
            base = type.getDeclaredField("base").get(null).toString();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        this.filePath = configDir.resolve(fileName + ".toml");
        this.defaultFilePath = "config/" + base + ".toml";
        loadStorage();
    }

    @NotNull
    public T getStorage() {
        loadStorage();
        return storage;
    }

    public void saveStorage() {
        TomlWriter writer = new TomlWriter.Builder()
            .indentValuesBy(2)
            .indentTablesBy(3)
            .build();
        
        try {
            writer.write(storage, filePath.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadStorage() {
        if (!Files.exists(filePath)) {
            createDefaultStorage();
        }

        Toml toml = new Toml().read(filePath.toFile());
        storage = toml.to(type);
    }

    private void createDefaultStorage() {
        try {
            Files.createDirectories(filePath.getParent());
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(defaultFilePath)) {
                assert in != null;
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}