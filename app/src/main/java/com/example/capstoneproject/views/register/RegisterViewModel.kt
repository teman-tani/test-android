package com.example.capstoneproject.views.register

import androidx.lifecycle.*
import com.example.capstoneproject.data.repository.AuthRepository
import com.raassh.dicodingstoryapp.misc.Event
import com.raassh.dicodingstoryapp.misc.getErrorResponse
import kotlinx.coroutines.launch
import retrofit2.HttpException

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSuccess = MutableLiveData<Event<Boolean>>()
    val isSuccess: LiveData<Event<Boolean>> = _isSuccess

    private val _error = MutableLiveData<Event<String>>()
    val error: LiveData<Event<String>> = _error

    fun register(name: String, email: String, password: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                _isSuccess.value = Event(authRepository.register(name, email, password))
            } catch (httpEx: HttpException) {
                httpEx.response()?.errorBody()?.let {
                    val errorResponse = getErrorResponse(it)

                    _error.value = Event(errorResponse.message)
                }
            } catch (genericEx: Exception) {
                _error.value = Event(genericEx.localizedMessage ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val authRepository: AuthRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RegisterViewModel(authRepository) as T
        }
    }
}