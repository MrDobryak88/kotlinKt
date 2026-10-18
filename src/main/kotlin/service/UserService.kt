package com.example.service

import UserRepository
import com.example.Exception.ForbiddenException
import com.example.Exception.InternalServerException
import com.example.Exception.UnauthorizedException // Создай этот класс ошибки (401)
import com.example.Exception.UserNotFoundException
import com.example.Exception.ValidationException
import com.example.models.User

class UserService(private val userRepo: UserRepository, private val authService: AuthService) {

    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    // Регулярка для пароля (оставил твою, она сложная, но рабочая)
    val passwordRegex = Regex(
        "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$"
    )
    /**
     * Удаляет пользователя по ID.
     * @param targetId ID пользователя, которого хотим удалить
     * @param requesterEmail Email того, кто запрашивает удаление (для защиты от самоудаления, если нужно)
     */
    suspend fun deleteUser(targetId: Int, requesterEmail: String? = null) {
        // 1. Проверяем, существует ли пользователь вообще
        val userToDelete = userRepo.findById(targetId)
            ?: throw UserNotFoundException("User with ID $targetId not found")

        // 2. Опциональная защита: запрет самоудаления (если передали email запросчика)
        if (requesterEmail != null && userToDelete.email == requesterEmail) {
            throw ForbiddenException("You cannot delete your own account via this endpoint")
        }

        // 3. Выполняем удаление
        val success = userRepo.remove(targetId)

    }
    /**
     * Регистрация нового пользователя
     */
    suspend fun create(email: String, plainPassword: String, role: String): User {
        if (!isValidEmail(email)) throw ValidationException("Invalid email format")
        if (!isValidPassword(plainPassword)) throw ValidationException("Weak password")

        // Проверка уникальности ДО записи в БД (чтобы дать красивую ошибку)
        val existingUser = userRepo.findByEmail(email)
        if (existingUser != null) {
            throw com.example.Exception.DuplicateResourceException("Email already registered")
        }

        val hashedPassword = authService.hashPassword(plainPassword)
        return userRepo.create(email, hashedPassword, role)
    }

    /**
     * ЛОГИН: Проверка учетных данных
     * Возвращает объект User, если данные верны.
     * Бросает UnauthorizedException, если нет.
     */
    suspend fun login(email: String, plainPassword: String): User {
        // 1. Ищем пользователя в базе
        val user = userRepo.findByEmail(email)
            ?: throw UnauthorizedException("Invalid credentials") // Не пишем "user not found", чтобы не давать инфу хакерам

        // 2. Проверяем пароль через AuthService
        // Здесь происходит сравнение plainPassword с storedHash внутри BCrypt
        val isCorrect = authService.checkPassword(plainPassword, email)

        if (!isCorrect) {
            throw UnauthorizedException("Invalid credentials")
        }

        // 3. Всё ок, возвращаем пользователя (роутер потом сделает из него JWT токен)
        return user
    }

    // Вспомогательные методы валидации
    fun isValidEmail(email: String): Boolean = email.matches(emailRegex)
    fun isValidPassword(password: String): Boolean = password.matches(passwordRegex)

    // Метод получения профиля (для GET /users/me)
    suspend fun getUserById(id: Int): User {
        return userRepo.findById(id)
            ?: throw com.example.Exception.UserNotFoundException("User not found")
    }
    suspend fun findByEmail(email: String): User {
        return userRepo.findByEmail(email)
            ?: throw com.example.Exception.UserNotFoundException("User not found")
    }
}