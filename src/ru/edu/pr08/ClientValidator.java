package ru.edu.pr08;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ClientValidator {
    public List<String> validate(ClientDraft d) {
        List<String> errors = new ArrayList<>();
        if (d == null) {
            errors.add("карточка отсутствует");
            return errors;
        }

        String inn = d.inn();
        if (inn == null || (inn.length() != 10 && inn.length() != 12)) {
            errors.add("ИНН должен содержать 10 или 12 цифр");
        } else if (!inn.matches("[0-9]+")) {
            errors.add("ИНН должен содержать только цифры");
        }
        if (inn != null && !inn.equals(inn.trim())) {
            errors.add("пробелы по краям ИНН запрещены");
        }

        String name = d.name();
        if (name == null || name.isEmpty()) {
            errors.add("имя не должно быть пустым");
        } else if (!name.equals(name.trim())) {
            errors.add("пробелы по краям имени запрещены");
        }

        String email = d.email();
        if (email == null || email.isEmpty()) {
            errors.add("email не должен быть пустым");
        } else if (!email.equals(email.trim())) {
            errors.add("пробелы по краям email запрещены");
        } else {
            int at = email.indexOf('@');
            if (at < 0) {
                errors.add("email должен содержать @");
            } else if (at == 0) {
                errors.add("email: пустая левая часть");
            } else if (at == email.length() - 1) {
                errors.add("email: пустая правая часть");
            } else if (at != email.lastIndexOf('@')) {
                errors.add("email должен содержать ровно один @");
            }
        }

        try {
            parseBirthDate(d.birthDate());
        } catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
        }
        return errors;
    }

    /** Возраст на текущую дату: 18 и 65 лет входят в среднюю категорию. */
    public String addAgeCategory(String birthDate) {
        int age = Period.between(parseBirthDate(birthDate), LocalDate.now()).getYears();
        if (age < 18) return "до 18";
        if (age <= 65) return "18–65";
        return "старше 65";
    }

    private LocalDate parseBirthDate(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new IllegalArgumentException("birthDate: требуется дата ГГГГ-ММ-ДД");
        }
        try {
            LocalDate date = LocalDate.parse(value);
            if (date.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("birthDate: дата рождения в будущем");
            }
            return date;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("birthDate: несуществующая дата");
        }
    }
}
