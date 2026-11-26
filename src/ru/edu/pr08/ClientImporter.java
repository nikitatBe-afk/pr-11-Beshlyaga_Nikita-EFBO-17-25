package ru.edu.pr08;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ClientImporter {
    private final ClientCsvParser parser = new ClientCsvParser();
    private final ClientValidator validator = new ClientValidator();
    private final ClientRepository repository;

    public ClientImporter(ClientRepository repository) {
        if (repository == null) throw new IllegalArgumentException("repository is null");
        this.repository = repository;
    }

    public ImportResult importFile(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            throw new IOException("Файл не найден: " + path);
        }
        ImportResult result = new ImportResult();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            reader.readLine(); // R1: заголовок
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    ClientDraft draft = parser.parse(line, lineNumber); // R2
                    List<String> errors = validator.validate(draft); // R3, R4, вариант 4
                    if (!errors.isEmpty()) {
                        throw new IllegalArgumentException(String.join("; ", errors));
                    }
                    if (repository.findByInn(draft.inn()) != null) { // R8
                        throw new IllegalArgumentException("дубликат ИНН: " + draft.inn());
                    }
                    repository.save(new Client(draft.inn(), draft.name(),
                            draft.email(), draft.birthDate()));
                    result.addAccepted();
                } catch (IllegalArgumentException e) { // R5, R6
                    result.addError(new ImportError(lineNumber, e.getMessage()));
                }
            }
        }
        return result;
    }
}
