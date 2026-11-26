
package ru.edu.pr08;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SelfCheck {

    private static int passed = 0;
    private static int total = 0;

    private static final ClientCsvParser parser =
            new ClientCsvParser();

    private static final ClientValidator validator =
            new ClientValidator();

    // ==========================================
    // MAIN — ТОЧКА ВХОДА В ПРОГРАММУ
    // ==========================================

    public static void main(String[] args) throws IOException {

        // Без аргументов запускаем все три режима
        if (args.length == 0) {

            System.out.println("===== UNIT CHECKS =====");
            runUnitChecks();

            System.out.println();
            System.out.println("===== DATA-DRIVEN CHECKS =====");
            runDataDriven();

            System.out.println();
            System.out.println("===== INTERACTIVE MODE =====");
            runInteractive();

        } else if (args[0].equals("unit")) {

            // Только unit-проверки
            runUnitChecks();

        } else if (args[0].equals("data")) {

            // Только CSV-проверки
            runDataDriven();

        } else if (args[0].equals("interactive")
                || args[0].equals("--interactive")) {

            // Только интерактивный режим
            runInteractive();

        } else {

            System.out.println(
                    "Использование: SelfCheck [unit|data|interactive]"
            );
        }
    }

    // ==========================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ==========================================

    // Выводим PASS или FAIL
    private static void check(int number, boolean condition) {

        total++;

        if (condition) {
            passed++;
            System.out.println("PASS " + number);
        } else {
            System.out.println("FAIL " + number);
        }
    }

    // Проверяем возникновение IllegalArgumentException
    private static boolean expectException(
            Runnable action,
            String expectedMessage) {

        try {

            action.run();
            return false;

        } catch (IllegalArgumentException e) {

            return e.getMessage() != null
                    && e.getMessage().contains(expectedMessage);
        }
    }

    // Ищем нужный текст в списке ошибок валидации
    private static boolean containsError(
            List<String> errors,
            String expected) {

        for (String error : errors) {

            if (error.contains(expected)) {
                return true;
            }
        }

        return false;
    }

    // Создаём тестовую карточку
    private static ClientDraft draft(
            String inn,
            String email) {

        return new ClientDraft(
                inn,
                "Иван",
                email,
                "2000-01-01"
        );
    }

    // Создаём временный CSV-файл
    private static Path createTestFile(String... lines)
            throws IOException {

        Path file = Files.createTempFile(
                "pr11-test-",
                ".csv"
        );

        file.toFile().deleteOnExit();

        List<String> content = new ArrayList<>();

        // Добавляем заголовок
        content.add("inn;name;email;birthDate");

        // Добавляем переданные строки
        for (String line : lines) {
            content.add(line);
        }

        Files.write(
                file,
                content,
                StandardCharsets.UTF_8
        );

        return file;
    }

    // Импортируем тестовые строки
    private static ImportResult importLines(
            String... lines) throws IOException {

        ClientRepository repository =
                new ClientRepository();

        ClientImporter importer =
                new ClientImporter(repository);

        return importer.importFile(createTestFile(lines));
    }

    // Проверяем номер строки с ошибкой
    private static boolean hasErrorAtLine(
            ImportResult result,
            int lineNumber) {

        for (ImportError error : result.errors()) {

            if (error.lineNumber() == lineNumber) {
                return true;
            }
        }

        return false;
    }

    // ==========================================
    // UNIT-ПРОВЕРКИ 1–22 (ВАРИАНТ 4)
    // ==========================================

    private static void runUnitChecks() throws IOException {

        passed = 0;
        total = 0;

        // 1. Корректный разбор CSV-строки
        ClientDraft parsed = parser.parse(
                "1234567890;Иван;ivan@mail.ru;2000-01-01",
                2
        );

        check(1,
                parsed.inn().equals("1234567890")
                && parsed.name().equals("Иван")
                && parsed.email().equals("ivan@mail.ru")
                && parsed.birthDate().equals("2000-01-01")
        );

        // 2. Неправильное количество полей
        check(2, expectException(
                () -> parser.parse(
                        "1234567890;Иван",
                        2
                ),
                "ожидается 4 поля"
        ));

        // 3. Пустая строка игнорируется
        ImportResult empty = importLines("");

        check(3,
                empty.accepted() == 0
                && empty.errors().isEmpty()
        );

        // 4. Корректный ИНН из 10 цифр
        check(4,
                validator.validate(
                        draft("1234567890", "ivan@mail.ru")
                ).isEmpty()
        );

        // 5. Корректный ИНН из 12 цифр
        check(5,
                validator.validate(
                        draft("123456789012", "ivan@mail.ru")
                ).isEmpty()
        );

        // 6. ИНН из 11 цифр отклоняется
        check(6, containsError(
                validator.validate(
                        draft("12345678901", "ivan@mail.ru")
                ),
                "10 или 12"
        ));

        // 7. ИНН с буквой отклоняется
        check(7, containsError(
                validator.validate(
                        draft("12345A7890", "ivan@mail.ru")
                ),
                "только цифры"
        ));

        // 8. Корректный email
        check(8,
                validator.validate(
                        draft("1234567890", "ivan@mail.ru")
                ).isEmpty()
        );

        // 9. Email без @ отклоняется
        check(9, containsError(
                validator.validate(
                        draft("1234567890", "ivanmail.ru")
                ),
                "содержать @"
        ));

        // 10. Пустая левая часть email
        check(10, containsError(
                validator.validate(
                        draft("1234567890", "@mail.ru")
                ),
                "пустая левая"
        ));

        // 11. Импорт трёх правильных клиентов
        ImportResult good = importLines(
                "1234567890;Иван;ivan@mail.ru;2000-01-01",
                "123456789012;Анна;anna@mail.ru;2000-01-01",
                "9876543210;Пётр;petr@mail.ru;2000-01-01"
        );

        check(11,
                good.accepted() == 3
                && good.errors().isEmpty()
        );

        // 12. Одна правильная и две ошибочные строки
        ImportResult bad = importLines(
                "1234567890;Иван;ivan@mail.ru;2000-01-01",
                "123;Неверный ИНН;bad@mail.ru;2000-01-01",
                "9876543210;Неверный email;noat;2000-01-01"
        );

        check(12,
                bad.accepted() == 1
                && bad.errors().size() == 2
                && hasErrorAtLine(bad, 3)
                && hasErrorAtLine(bad, 4)
        );

        // 13. Несуществующий файл вызывает IOException
        boolean fileError = false;

        Path missing = Files.createTempFile(
                "pr11-missing-",
                ".csv"
        );

        Files.delete(missing);

        try {

            ClientImporter importer = new ClientImporter(
                    new ClientRepository()
            );

            importer.importFile(missing);

        } catch (IOException e) {

            fileError = true;
        }

        check(13, fileError);

        // 14. Заголовок не считается записью
        ImportResult header = importLines(
                "1234567890;Иван;ivan@mail.ru;2000-01-01",
                "123456789012;Анна;anna@mail.ru;2000-01-01"
        );

        check(14,
                header.accepted() == 2
                && header.errors().isEmpty()
        );

        // 15. Дубликат ИНН отклоняется
        ImportResult duplicate = importLines(
                "1234567890;Иван;ivan@mail.ru;2000-01-01",
                "1234567890;Дубль;copy@mail.ru;2000-01-01"
        );

        check(15,
                duplicate.accepted() == 1
                && duplicate.errors().size() == 1
                && duplicate.errors().get(0)
                        .message().contains("дубликат")
        );

        // 16. Пустые строки и комментарии игнорируются
        ImportResult blanks = importLines(
                "",
                "# комментарий",
                "1234567890;Иван;ivan@mail.ru;2000-01-01"
        );

        check(16,
                blanks.accepted() == 1
                && blanks.errors().isEmpty()
        );

        // 17. Проверяем счётчики ImportResult
        ImportResult result = new ImportResult();

        for (int i = 0; i < 5; i++) {
            result.addAccepted();
        }

        result.addError(
                new ImportError(3, "неверный ИНН")
        );

        result.addError(
                new ImportError(4, "неверный email")
        );

        check(17,
                result.accepted() == 5
                && result.errors().size() == 2
        );

        // 18. Ошибка содержит номер строки и причину
        check(18,
                result.errors().get(0).lineNumber() == 3
                && result.errors().get(0)
                        .message().contains("ИНН")
        );

        // Вариант 4, часть А: обязательная дата рождения.
        ImportResult missingDate = importLines("1234567890;Иван;ivan@mail.ru;");
        check(19, missingDate.accepted() == 0
                && missingDate.errors().size() == 1
                && missingDate.errors().get(0).message().contains("birthDate"));

        ImportResult invalidDate = importLines(
                "1234567890;Иван;ivan@mail.ru;2023-02-29",
                "123456789012;Анна;anna@mail.ru;2000-01-01");
        check(20, invalidDate.accepted() == 1
                && invalidDate.errors().size() == 1
                && hasErrorAtLine(invalidDate, 2));

        // Вариант 4, часть Б: границы возрастных категорий.
        LocalDate today = LocalDate.now();
        check(21, validator.addAgeCategory(today.minusYears(18).plusDays(1).toString())
                    .equals("до 18")
                && validator.addAgeCategory(today.minusYears(18).toString())
                    .equals("18–65"));
        check(22, validator.addAgeCategory(today.minusYears(65).toString())
                    .equals("18–65")
                && validator.addAgeCategory(today.minusYears(66).toString())
                    .equals("старше 65"));

        // Выводим результат
        System.out.println(
                "Unit checks: "
                + passed + "/" + total + " passed"
        );

        if (passed != total) {
            throw new IllegalStateException(
                    "Unit checks failed"
            );
        }
    }

    // ==========================================
    // DATA-DRIVEN РЕЖИМ
    // ==========================================

    private static void runDataDriven() throws IOException {

        Path path = Path.of("data/sample.csv");

        List<String> lines = Files.readAllLines(
                path,
                StandardCharsets.UTF_8
        );

        int totalData = 0;
        int passedData = 0;

        // Ранее принятые строки нужны для проверки дубликатов
        List<String> accepted = new ArrayList<>();

        // Первая строка — заголовок
        for (int i = 1; i < lines.size(); i++) {

            String line = lines.get(i);

            // Пропускаем пустые строки и комментарии
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            totalData++;

            // Разделяем тестовую строку на пять полей
            String[] fields = line.split(";", -1);

            if (fields.length != 5) {

                System.out.println(
                        "FAIL data/" + totalData
                        + " (ожидается 5 полей)"
                );

                continue;
            }

            // Создаём обычную строку клиента
            String clientLine =
                    fields[0] + ";"
                    + fields[1] + ";"
                    + fields[2] + ";" + fields[3];

            String expected = fields[4];

            // Добавляем текущую строку к ранее принятым
            List<String> current =
                    new ArrayList<>(accepted);

            current.add(clientLine);

            ImportResult importResult = importLines(
                    current.toArray(new String[0])
            );

            boolean success =
                    importResult.accepted()
                    == accepted.size() + 1;

            String actual = success ? "OK" : "ERROR";

            boolean ok = actual.equals(expected);

            if (ok) {
                passedData++;
            }

            System.out.println(
                    (ok ? "PASS " : "FAIL ")
                    + "data/" + totalData
                    + " [" + actual + "]"
            );

            // Сохраняем только успешно принятые строки
            if (success) {
                accepted.add(clientLine);
            }
        }

        // Итог data-driven режима
        System.out.println(
                "Data-driven: "
                + passedData + "/" + totalData + " passed"
        );

        if (passedData != totalData) {
            throw new IllegalStateException(
                    "Data-driven checks failed"
            );
        }
    }

    // ==========================================
    // ИНТЕРАКТИВНЫЙ РЕЖИМ С МЕНЮ
    // ==========================================

    private static void runInteractive() {

        System.out.println("=== INTERACTIVE MODE ===");

        try (Scanner scanner = new Scanner(System.in)) {

            while (true) {

                System.out.println();
                System.out.println("1 - Ввести клиентов вручную");
                System.out.println("2 - Импортировать CSV-файл");
                System.out.println("0 - Выход");
                System.out.print("Выберите режим: ");

                if (!scanner.hasNextLine()) {
                    break;
                }

                String choice = scanner.nextLine().trim();

                if (choice.equals("0")) {
                    break;
                }

                ClientImporter importer = new ClientImporter(
                        new ClientRepository()
                );

                try {

                    ImportResult result;

                    if (choice.equals("1")) {

                        // ----------------------------------
                        // РУЧНОЙ ВВОД КЛИЕНТОВ
                        // ----------------------------------

                        List<String> clientLines = new ArrayList<>();

                        System.out.println();
                        System.out.println("=== РУЧНОЙ ВВОД ===");
                        System.out.println(
                                "Введите данные клиентов по очереди."
                        );
                        System.out.println(
                                "Пустой ИНН завершает ввод."
                        );

                        while (true) {

                            System.out.println();
                            System.out.println(
                                    "Клиент №" + (clientLines.size() + 1)
                            );

                            System.out.print("ИНН: ");

                            if (!scanner.hasNextLine()) {
                                break;
                            }

                            String inn = scanner.nextLine();

                            if (inn.isEmpty()) {
                                break;
                            }

                            System.out.print("Имя: ");

                            if (!scanner.hasNextLine()) {
                                break;
                            }

                            String name = scanner.nextLine();

                            System.out.print("Email: ");

                            if (!scanner.hasNextLine()) {
                                break;
                            }

                            String email = scanner.nextLine();

                            System.out.print("Дата рождения (ГГГГ-ММ-ДД): ");
                            if (!scanner.hasNextLine()) break;
                            String birthDate = scanner.nextLine();

                            // Преобразуем введённые данные в CSV-строку
                            clientLines.add(
                                    inn + ";" + name + ";" + email + ";" + birthDate
                            );

                            System.out.println("Клиент добавлен.");
                        }

                        if (clientLines.isEmpty()) {
                            System.out.println(
                                    "Клиенты не введены."
                            );
                            continue;
                        }

                        // Временный CSV-файл
                        Path tempFile = Files.createTempFile(
                                "pr11-interactive-",
                                ".csv"
                        );

                        try {

                            List<String> content =
                                    new ArrayList<>();

                            // Первая строка — заголовок
                            content.add("inn;name;email;birthDate");

                            // Затем введённые клиенты
                            content.addAll(clientLines);

                            Files.write(
                                    tempFile,
                                    content,
                                    StandardCharsets.UTF_8
                            );

                            // Вызываем основной импорт
                            result = importer.importFile(tempFile);

                        } finally {

                            // Удаляем временный файл
                            Files.deleteIfExists(tempFile);
                        }

                    } else if (choice.equals("2")) {

                        // ----------------------------------
                        // ЧТЕНИЕ ГОТОВОГО CSV-ФАЙЛА
                        // ----------------------------------

                        System.out.print("CSV path: ");

                        if (!scanner.hasNextLine()) {
                            break;
                        }

                        String path = scanner.nextLine().trim();

                        if (path.isEmpty()) {
                            System.out.println("Путь не указан.");
                            continue;
                        }

                        result = importer.importFile(
                                Path.of(path)
                        );

                    } else {

                        System.out.println(
                                "Неверный пункт меню."
                        );
                        continue;
                    }

                    // ----------------------------------
                    // РЕЗУЛЬТАТ ИМПОРТА
                    // ----------------------------------

                    System.out.println();
                    System.out.println("=== РЕЗУЛЬТАТ ИМПОРТА ===");

                    System.out.println(
                            "Accepted: " + result.accepted()
                    );

                    System.out.println(
                            "Rejected: " + result.errors().size()
                    );

                    // Показываем ошибки с номерами строк
                    for (ImportError error : result.errors()) {

                        System.out.println(
                                "строка " + error.lineNumber()
                                + ": " + error.message()
                        );
                    }



                } catch (IOException | IllegalArgumentException e) {

                    System.out.println(
                            "ERROR: " + e.getMessage()
                    );
                }
            }
        }

        System.out.println("Interactive mode finished.");
    }
}
