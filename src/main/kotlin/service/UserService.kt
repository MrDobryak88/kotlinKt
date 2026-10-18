package com.example.service

import UserRepository
import com.example.Exception.DuplicateResourceException
import com.example.Exception.ForbiddenException
import com.example.Exception.UnauthorizedException
import com.example.Exception.UserNotFoundException
import com.example.Exception.ValidationException
import com.example.models.User

class UserService(private val userRepo: UserRepository, private val authService: AuthService) {

    companion object {
        const val ROLE_USER = "USER"
        const val ROLE_ADMIN = "ADMIN"
    }

    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    // Пароль: минимум 8 символов, заглавная, строчная, цифра и спецсимвол
    val passwordRegex = Regex(
        "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$"
    )

    /**
     * Удаляет пользователя по ID.
     * @param targetId ID пользователя, которого хотим удалить
     * @param requesterEmail Email того, кто запрашивает удаление (запрет самоудаления)
     */
    suspend fun deleteUser(targetId: Int, requesterEmail: String? = null) {
        val userToDelete = userRepo.findById(targetId)
            ?: throw UserNotFoundException("User with ID $targetId not found")

        if (requesterEmail != null && userToDelete.email == requesterEmail) {
            throw ForbiddenException("You cannot delete your own account via this endpoint")
        }

        userRepo.remove(targetId)
    }

    /**
     * Публичная регистрация. Роль выбирает сервер, а не клиент:
     * самый первый пользователь системы становится ADMIN, все остальные — USER.
     */
    suspend fun register(email: String, plainPassword: String): User {
        val role = if (userRepo.hasUsers()) ROLE_USER else ROLE_ADMIN
        return create(email, plainPassword, role)
    }

    /**
     * Создание пользователя с явной ролью (для внутреннего использования)
     */
    suspend fun create(email: String, plainPassword: String, role: String): User {
        if (!isValidEmail(email)) throw ValidationException("Invalid email format")
        if (!isValidPassword(plainPassword)) throw ValidationException("Weak password")

        // Проверка уникальности ДО записи в БД (чтобы дать красивую ошибку)
        if (userRepo.findByEmail(email) != null) {
            throw DuplicateResourceException("Email already registered")
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
        // Не пишем "user not found", чтобы не давать лишнюю информацию
        val user = userRepo.findByEmail(email)
            ?: throw UnauthorizedException("Invalid credentials")

        if (!authService.checkPassword(plainPassword, email)) {
            throw UnauthorizedException("Invalid credentials")
        }

        return user
    }

    // Вспомогательные методы валидации
    fun isValidEmail(email: String): Boolean = email.matches(emailRegex)
    fun isValidPassword(password: String): Boolean = password.matches(passwordRegex)

    suspend fun getAll(): List<User> = userRepo.getAll()

    suspend fun getUserById(id: Int): User {
        return userRepo.findById(id)
            ?: throw UserNotFoundException("User not found")
    }

    suspend fun findByEmail(email: String): User {
        return userRepo.findByEmail(email)
            ?: throw UserNotFoundException("User not found")
    }
}
