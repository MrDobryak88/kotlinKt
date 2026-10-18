package com.example

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerTest {

    private val password = "Passw0rd!x"

    /** Поднимает приложение с чистой БД H2 в памяти (у каждого теста своя). */
    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment {
            config = MapApplicationConfig(
                "db.url" to "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
                "db.driver" to "org.h2.Driver",
                "db.user" to "sa",
                "db.password" to "",
                "jwt.secret" to "test-secret",
                "jwt.issuer" to "test-issuer",
                "jwt.audience" to "test-audience",
                "jwt.realm" to "test"
            )
        }
        application { module() }
        block()
    }

    private suspend fun ApplicationTestBuilder.register(email: String, pass: String = password): HttpResponse =
        client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","plainPassword":"$pass"}""")
        }

    private suspend fun ApplicationTestBuilder.login(email: String, pass: String = password): HttpResponse =
        client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","plainPassword":"$pass"}""")
        }

    /** Регистрирует пользователя, логинится и возвращает токен. */
    private suspend fun ApplicationTestBuilder.registerAndLogin(email: String): String {
        assertEquals(HttpStatusCode.Created, register(email).status)
        val response = login(email)
        assertEquals(HttpStatusCode.OK, response.status)
        return Json.parseToJsonElement(response.bodyAsText())
            .jsonObject.getValue("token").jsonPrimitive.content
    }

    private fun json(text: String) = Json.parseToJsonElement(text).jsonObject

    private suspend fun ApplicationTestBuilder.postEquipment(
        token: String,
        name: String = "Microscope",
        labName: String = "Lab 1",
        hourlyRate: Int = 100
    ): HttpResponse = client.post("/api/equipment") {
        header(HttpHeaders.Authorization, "Bearer $token")
        contentType(ContentType.Application.Json)
        setBody("""{"name":"$name","labName":"$labName","hourlyRate":$hourlyRate}""")
    }

    @Test
    fun `root endpoint responds`() = withApp {
        assertEquals(HttpStatusCode.OK, client.get("/").status)
    }

    @Test
    fun `first user becomes admin and the next ones are regular users`() = withApp {
        val adminToken = registerAndLogin("admin@lab.com")
        val userToken = registerAndLogin("user@lab.com")

        val admin = client.get("/api/me") { header(HttpHeaders.Authorization, "Bearer $adminToken") }
        val user = client.get("/api/me") { header(HttpHeaders.Authorization, "Bearer $userToken") }

        assertEquals("ADMIN", json(admin.bodyAsText()).getValue("role").jsonPrimitive.content)
        assertEquals("USER", json(user.bodyAsText()).getValue("role").jsonPrimitive.content)
    }

    @Test
    fun `registration cannot be used to grab the admin role and does not leak the hash`() = withApp {
        register("first@lab.com") // первый пользователь — ADMIN

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"hacker@lab.com","plainPassword":"$password","role":"ADMIN"}""")
        }
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.Created, response.status)
        assertFalse(body.contains("passwordHash"), "hash must not be returned: $body")
        assertTrue(body.contains("USER"), "role must be forced to USER: $body")
    }

    @Test
    fun `register and login validate their input`() = withApp {
        assertEquals(HttpStatusCode.Created, register("a@lab.com").status)
        assertEquals(HttpStatusCode.Conflict, register("a@lab.com").status)
        assertEquals(HttpStatusCode.BadRequest, register("b@lab.com", "weak").status)
        assertEquals(HttpStatusCode.BadRequest, register("not-an-email").status)
        assertEquals(HttpStatusCode.Unauthorized, login("a@lab.com", "Wrong0Pass!").status)
        assertEquals(HttpStatusCode.Unauthorized, login("nobody@lab.com").status)

        val broken = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("{ this is not json")
        }
        assertEquals(HttpStatusCode.BadRequest, broken.status)
    }

    @Test
    fun `protected routes require a token`() = withApp {
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/me").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/equipment").status)
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.get("/api/me") { header(HttpHeaders.Authorization, "Bearer garbage") }.status
        )
    }

    @Test
    fun `equipment crud works for admin and is read only for users`() = withApp {
        val adminToken = registerAndLogin("admin@lab.com")
        val userToken = registerAndLogin("user@lab.com")

        // создание
        val created = postEquipment(adminToken)
        assertEquals(HttpStatusCode.Created, created.status)
        val id = json(created.bodyAsText()).getValue("id").jsonPrimitive.int

        // обычный пользователь не может изменять, но может читать
        assertEquals(HttpStatusCode.Forbidden, postEquipment(userToken, name = "Centrifuge").status)
        val list = client.get("/api/equipment") { header(HttpHeaders.Authorization, "Bearer $userToken") }
        assertEquals(HttpStatusCode.OK, list.status)
        assertTrue(list.bodyAsText().contains("Microscope"))
        assertEquals(
            HttpStatusCode.OK,
            client.get("/api/equipment/$id") { header(HttpHeaders.Authorization, "Bearer $userToken") }.status
        )
        assertEquals(
            HttpStatusCode.Forbidden,
            client.delete("/api/equipment/$id") { header(HttpHeaders.Authorization, "Bearer $userToken") }.status
        )

        // обновление
        val updated = client.put("/api/equipment/$id") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Microscope","labName":"Lab 1","hourlyRate":250,"isAvailable":false}""")
        }
        assertEquals(HttpStatusCode.OK, updated.status)
        assertEquals(250, json(updated.bodyAsText()).getValue("hourlyRate").jsonPrimitive.int)

        // фильтр по доступности: оборудование теперь недоступно
        val available = client.get("/api/equipment?available=true") {
            header(HttpHeaders.Authorization, "Bearer $userToken")
        }
        assertFalse(available.bodyAsText().contains("Microscope"))

        // удаление
        assertEquals(
            HttpStatusCode.NoContent,
            client.delete("/api/equipment/$id") { header(HttpHeaders.Authorization, "Bearer $adminToken") }.status
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/api/equipment/$id") { header(HttpHeaders.Authorization, "Bearer $adminToken") }.status
        )
    }

    @Test
    fun `equipment input is validated`() = withApp {
        val adminToken = registerAndLogin("admin@lab.com")

        assertEquals(HttpStatusCode.Created, postEquipment(adminToken).status)
        // тот же прибор в той же лаборатории
        assertEquals(HttpStatusCode.Conflict, postEquipment(adminToken).status)
        // тот же прибор в другой лаборатории — можно
        assertEquals(HttpStatusCode.Created, postEquipment(adminToken, labName = "Lab 2").status)
        // отрицательная цена и пустое имя
        assertEquals(HttpStatusCode.BadRequest, postEquipment(adminToken, name = "Pump", hourlyRate = -5).status)
        assertEquals(HttpStatusCode.BadRequest, postEquipment(adminToken, name = "   ").status)
    }

    @Test
    fun `only admin can list and delete users and cannot delete self`() = withApp {
        val adminToken = registerAndLogin("admin@lab.com")
        val userToken = registerAndLogin("user@lab.com")

        assertEquals(
            HttpStatusCode.Forbidden,
            client.get("/api/users") { header(HttpHeaders.Authorization, "Bearer $userToken") }.status
        )
        val users = client.get("/api/users") { header(HttpHeaders.Authorization, "Bearer $adminToken") }
        assertEquals(HttpStatusCode.OK, users.status)
        assertFalse(users.bodyAsText().contains("passwordHash"))

        val me = client.get("/api/me") { header(HttpHeaders.Authorization, "Bearer $adminToken") }
        val adminId = json(me.bodyAsText()).getValue("id").jsonPrimitive.int
        val other = client.get("/api/me") { header(HttpHeaders.Authorization, "Bearer $userToken") }
        val userId = json(other.bodyAsText()).getValue("id").jsonPrimitive.int

        assertEquals(
            HttpStatusCode.Forbidden,
            client.post("/api/admin/delete-user/$userId") {
                header(HttpHeaders.Authorization, "Bearer $userToken")
            }.status
        )
        assertEquals(
            HttpStatusCode.Forbidden,
            client.post("/api/admin/delete-user/$adminId") {
                header(HttpHeaders.Authorization, "Bearer $adminToken")
            }.status
        )
        assertEquals(
            HttpStatusCode.OK,
            client.post("/api/admin/delete-user/$userId") {
                header(HttpHeaders.Authorization, "Bearer $adminToken")
            }.status
        )
        // токен удалённого пользователя больше не работает
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.get("/api/me") { header(HttpHeaders.Authorization, "Bearer $userToken") }.status
        )
    }
}
