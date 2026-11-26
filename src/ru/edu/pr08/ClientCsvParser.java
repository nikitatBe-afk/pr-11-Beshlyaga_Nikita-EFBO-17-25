package ru.edu.pr08;

public class ClientCsvParser {
    public ClientDraft parse(String line, int lineNumber) {
        if (line == null) {
            throw new IllegalArgumentException("строка " + lineNumber + ": строка равна null");
        }
        String[] parts = line.split(";", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("строка " + lineNumber
                    + ": ожидается 4 поля, получено " + parts.length);
        }
        return new ClientDraft(parts[0], parts[1], parts[2], parts[3]);
    }
}
