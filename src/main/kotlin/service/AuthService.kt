package com.example.service

import UserRepository
import org.mindrot.jbcrypt.BCrypt

class AuthService(private val userRepo: UserRepository) {

    /**
     * Хэширует пароль перед сохранением в БД
     */
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    /**
     * Проверяет соответствие введенного пароля сохраненному хешу
     * @return true если пароль верный, false если нет или пользователь не найден
     */
    suspend fun checkPassword(plainPassword: String, email: String): Boolean {
        val storedHash = userRepo.getHash(email)

        if (storedHash == null) {
            return false
        }

        return BCrypt.checkpw(plainPassword, storedHash)
    }
}