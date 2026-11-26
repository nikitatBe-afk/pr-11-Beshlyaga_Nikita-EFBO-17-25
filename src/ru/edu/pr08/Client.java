package ru.edu.pr08;

/** Карточка клиента с обязательной датой рождения (вариант 4). */
public record Client(String inn, String name, String email, String birthDate) {}
