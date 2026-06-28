/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gyabdev.livecollections.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.gyabdev.livecollections.data.CollectionRepository
import com.gyabdev.livecollections.ui.collection.CollectionUiState.Error
import com.gyabdev.livecollections.ui.collection.CollectionUiState.Loading
import com.gyabdev.livecollections.ui.collection.CollectionUiState.Success
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    val uiState: StateFlow<CollectionUiState> = collectionRepository
        .collections.map<List<String>, CollectionUiState>(::Success)
        .catch { emit(Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)

    fun addCollection(name: String) {
        viewModelScope.launch {
            collectionRepository.add(name)
        }
    }
}

sealed interface CollectionUiState {
    object Loading : CollectionUiState
    data class Error(val throwable: Throwable) : CollectionUiState
    data class Success(val data: List<String>) : CollectionUiState
}
