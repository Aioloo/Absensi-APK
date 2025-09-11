package com.example.absensiapk.modelview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.absensiapk.api.ApiService
import com.example.absensiapk.models.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import android.widget.Toast

class LoginViewModel(private val apiService: ApiService, private val context: Context) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = apiService.login(LoginRequest(username, password))
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        with(prefs.edit()) {
                            putString("jwt_token", loginResponse.token)
                            putInt("karyawan_id", loginResponse.karyawanId)
                            apply()
                        }
                        _loginState.value = LoginState.Success(loginResponse.token, loginResponse.karyawanId)
                        Toast.makeText(context, "Login berhasil", Toast.LENGTH_SHORT).show()
                    } else {
                        _loginState.value = LoginState.Error("Respons API kosong.")
                    }
                } else {
                    _loginState.value = LoginState.Error("Kredensial tidak valid.")
                }
            } catch (e: HttpException) {
                _loginState.value = LoginState.Error("Kesalahan HTTP: ${e.code()}")
            } catch (e: IOException) {
                _loginState.value = LoginState.Error("Kesalahan jaringan: ${e.message}")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Kesalahan tak terduga: ${e.message}")
            }
        }
    }

    companion object {
        fun Factory(apiService: ApiService, context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoginViewModel(apiService, context) as T
                }
            }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String, val karyawanId: Int) : LoginState()
    data class Error(val message: String) : LoginState()
}