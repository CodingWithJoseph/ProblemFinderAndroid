package com.example.problemfinder.data.authentication.impl
import com.example.problemfinder.core.model.auth.AuthOperationException
import com.example.problemfinder.core.model.auth.AuthUser
import com.example.problemfinder.data.authentication.api.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private suspend fun <T> authCall(block: suspend () -> T): T =
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        throw AuthOperationException(SupabaseAuthErrorMapper.map(error), error)
    }

class SupabaseAuthRepository @Inject constructor(
    private val client: SupabaseClient
): AuthRepository {

    override suspend fun signIn(email: String, password: String): AuthUser = authCall {
        client.auth.signInWith(Email){
            this.email = email
            this.password = password
        }

        val user = checkNotNull(client.auth.currentUserOrNull()){
            "Sign-in completed without an active user"
        }

        return@authCall AuthUser(id = user.id)
    }

    override suspend fun createAccount(firstname: String, lastname: String, email: String, password: String) = authCall {
        client.auth.signUpWith(Email){
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("first_name", firstname)
                put("last_name", lastname)
            }
        }
        return@authCall
    }

    override suspend fun forgotPassword(email: String) = authCall {
        client.auth.resetPasswordForEmail(
            email=email,
            redirectUrl = "com.codingwithjoseph.immensity://auth/reset-password"
        )
    }


    override suspend fun resetPassword(password: String) {
        authCall {
            client.auth.updateUser {
                this.password = password
            }
        }
    }

    override suspend fun signOut() = authCall {
        client.auth.signOut()
    }
}
