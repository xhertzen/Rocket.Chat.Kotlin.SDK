package chat.rocket.core.internal.rest

import chat.rocket.common.RocketChatApiException
import chat.rocket.common.RocketChatAuthException
import chat.rocket.common.RocketChatInvalidResponseException
import chat.rocket.common.model.Token
import chat.rocket.common.util.PlatformLogger
import chat.rocket.core.RocketChatClient
import chat.rocket.core.TokenRepository
import com.nhaarman.mockito_kotlin.check
import com.nhaarman.mockito_kotlin.verify
import com.squareup.moshi.JsonEncodingException
import io.fabric8.mockwebserver.DefaultMockServer
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.OkHttpClient
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.MockitoAnnotations
import org.hamcrest.CoreMatchers.`is` as isEqualTo

class LoginTest {

    private lateinit var mockServer: DefaultMockServer

    private lateinit var sut: RocketChatClient

    @Mock
    private lateinit var tokenProvider: TokenRepository

    private val authToken = Token("userId", "authToken")

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        mockServer = DefaultMockServer()
        mockServer.start()

        val client = OkHttpClient()
        sut = RocketChatClient.create {
            httpClient = client
            restUrl = mockServer.url("/")
            tokenRepository = this@LoginTest.tokenProvider
            platformLogger = PlatformLogger.NoOpLogger()
        }

        `when`(tokenProvider.get()).thenReturn(authToken)
    }

    @Test
    fun `login() should succeed with right credentials`() {
        mockServer.expect()
                .post()
                .withPath("/api/v1/login")
                .andReturn(200, LOGIN_SUCCESS)
                .once()

        runBlocking {
            val token = sut.login("username", "password")
            assertThat(token, isEqualTo(authToken))
            assertThat(token.userId, isEqualTo("userId"))
            assertThat(token.authToken, isEqualTo("authToken"))

            verify(tokenProvider).save(check {
                assertThat(it.userId, isEqualTo("userId"))
                assertThat(it.authToken, isEqualTo("authToken"))
            })
        }
    }

    @Test
    fun `login() should fail with RocketChatAuthException on wrong credentials`() {
        mockServer.expect()
                .post()
                .withPath("/api/v1/login")
                .andReturn(401, LOGIN_ERROR)
                .once()

        runBlocking {
            try {
                sut.login("wronguser", "wrongpass")

                throw RuntimeException("unreachable code")
            } catch (ex: Exception) {
                assertThat(ex, isEqualTo(instanceOf(RocketChatAuthException::class.java)))
                assertThat(ex.message, isEqualTo("Unauthorized"))
            }
            verify(tokenProvider, never()).save(check { })
        }
    }

    @Test
    fun `login() should fail with RocketChatInvalidResponseException on invalid response`() {
        mockServer.expect()
                .post()
                .withPath("/api/v1/login")
                .andReturn(200, "NOT A JSON")
                .once()

        runBlocking {
            try {
                sut.login("user", "password")

                throw RuntimeException("unreachable code")
            } catch (ex: Exception) {
                assertThat(ex, isEqualTo(instanceOf(RocketChatInvalidResponseException::class.java)))
                assertThat(ex.message, isEqualTo("Use JsonReader.setLenient(true) to accept malformed JSON at path $"))
                assertThat(ex.cause, isEqualTo(instanceOf(JsonEncodingException::class.java)))
            }

            verify(tokenProvider, never()).save(check { })
        }
    }

    @Test
    fun `login() should fail with RocketChatApiException when response is not 200 OK`() {
        runBlocking {
            try {
                sut.login("user", "password")

                throw RuntimeException("unreachable code")
            } catch (ex: Exception) {
                assertThat(ex, isEqualTo(instanceOf(RocketChatApiException::class.java)))
            }
            verify(tokenProvider, never()).save(check { })
        }
    }

    @Test
    fun `loginWithEmail() should succeed with right credentials`() {
        mockServer.expect()
                .post()
                .withPath("/api/v1/login")
                .andReturn(200, LOGIN_SUCCESS)
                .once()

        runBlocking {
            val token = sut.loginWithEmail("test@email.com", "password")
            assertThat(token, isEqualTo(authToken))
            assertThat(token.userId, isEqualTo("userId"))
            assertThat(token.authToken, isEqualTo("authToken"))

            verify(tokenProvider).save(check {
                assertThat(it.userId, isEqualTo("userId"))
                assertThat(it.authToken, isEqualTo("authToken"))
            })
        }
    }

    @Test
    fun `loginWithEmail() should fail with RocketChatAuthException on wrong credentials`() {
        mockServer.expect()
                .post()
                .withPath("/api/v1/login")
                .andReturn(401, LOGIN_ERROR)
                .once()

        runBlocking {
            try {
                sut.loginWithEmail("wrongemail@gmail.com", "wrongpass")

                throw RuntimeException("unreachable code")
            } catch (ex: Exception) {
                assertThat(ex, isEqualTo(instanceOf(RocketChatAuthException::class.java)))
                assertThat(ex.message, isEqualTo("Unauthorized"))
            }
            verify(tokenProvider, never()).save(check { })
        }
    }

    @Test
    fun `loginWithEmail() should fail with RocketChatInvalidResponseException on invalid response`() {
        mockServer.expect()
                .post()
                .withPath("/api/v1/login")
                .andReturn(200, "NOT A JSON")
                .once()

        runBlocking {
            try {
                sut.loginWithEmail("user@email.com", "password")

                throw RuntimeException("unreachable code")
            } catch (ex: Exception) {
                assertThat(ex, isEqualTo(instanceOf(RocketChatInvalidResponseException::class.java)))
                assertThat(ex.message, isEqualTo("Use JsonReader.setLenient(true) to accept malformed JSON at path $"))
                assertThat(ex.cause, isEqualTo(instanceOf(JsonEncodingException::class.java)))
            }

            verify(tokenProvider, never()).save(check { })
        }
    }

    @Test
    fun `loginWithEmail() should fail with RocketChatApiException when response is not 200 OK`() {
        runBlocking {
            try {
                sut.loginWithEmail("user@email.com", "password")

                throw RuntimeException("unreachable code")
            } catch (ex: Exception) {
                assertThat(ex, isEqualTo(instanceOf(RocketChatApiException::class.java)))
            }
            verify(tokenProvider, never()).save(check { })
        }
    }

    @Test
    fun `signup() should succeed with valid parameters`() {
        mockServer.expect()
                .post()
                .withPath("/api/v1/users.register")
                .andReturn(200, USER_REGISTER_SUCCESS)
                .once()

        runBlocking {
            val user = sut.signup("test@email.com", "Test User", "testuser", "password")
            assertThat(user.id, isEqualTo("userId"))
        }
    }

    @Test
    fun `signup() should fail with RocketChatApiException if email is already in use`() {
        mockServer.expect()
                .post()
                .withPath("/api/v1/users.register")
                .andReturn(403, FAIL_EMAIL_IN_USE)
                .once()

        runBlocking {
            try {
                sut.signup("test@email.com", "Test User", "testuser", "password")

                throw RuntimeException("unreachable code")
            } catch (ex: Exception) {
                assertThat(ex, isEqualTo(instanceOf(RocketChatApiException::class.java)))
                assertThat(ex.message, isEqualTo("Email already exists. [403]"))
                val api = ex as RocketChatApiException
                assertThat(api.errorType, isEqualTo("403"))
            }
        }
    }

    @Test
    fun `signup() should fail with RocketChatApiException if username is already in use`() {
        mockServer.expect()
                .post()
                .withPath("/api/v1/users.register")
                .andReturn(403, FAIL_USER_IN_USE)
                .once()

        runBlocking {
            try {
                sut.signup("test@email.com", "Test User", "testuser", "password")

                throw RuntimeException("unreachable code")
            } catch (exception: Exception) {
                assertThat(exception, isEqualTo(instanceOf(RocketChatApiException::class.java)))
                val ex = exception as RocketChatApiException
                assertThat(ex.errorType, isEqualTo("error-field-unavailable"))
                assertThat(ex.message, isEqualTo("<strong>testuser</strong> is already in use :( [error-field-unavailable]"))
            }
        }
    }

    @After
    fun shutdown() {
        mockServer.shutdown()
    }
}