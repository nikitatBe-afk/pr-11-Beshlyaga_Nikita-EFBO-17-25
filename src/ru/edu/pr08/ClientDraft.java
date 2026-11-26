package ru.edu.pr08;

/** Поля CSV до проверки бизнес-правил. */
public record ClientDraft(String inn, String name, String email, String birthDate) {}
