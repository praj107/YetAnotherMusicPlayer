package com.yamp.ui.screen.search

import com.google.common.truth.Truth.assertThat
import com.yamp.domain.model.Track
import com.yamp.domain.usecase.search.SearchTracksUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private lateinit var searchTracksUseCase: SearchTracksUseCase
    private lateinit var viewModel: SearchViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        searchTracksUseCase = mockk()
        viewModel = SearchViewModel(searchTracksUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() {
        val state = viewModel.uiState.value
        assertThat(state.query).isEmpty()
        assertThat(state.results).isEmpty()
        assertThat(state.isSearching).isFalse()
    }

    @Test
    fun `onQueryChange updates query`() {
        viewModel.onQueryChange("test")
        assertThat(viewModel.uiState.value.query).isEqualTo("test")
    }

    @Test
    fun `blank query clears results`() = runTest {
        viewModel.onQueryChange("")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.results).isEmpty()
        assertThat(viewModel.uiState.value.isSearching).isFalse()
    }

    @Test
    fun `search debounces input`() = runTest {
        val track = createTrack(1, "Hello World")
        every { searchTracksUseCase("Hello") } returns flowOf(listOf(track))

        viewModel.onQueryChange("Hello")
        advanceTimeBy(100) // before debounce
        assertThat(viewModel.uiState.value.results).isEmpty()

        advanceTimeBy(300) // after debounce
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.results).hasSize(1)
    }

    private fun createTrack(id: Long, title: String) = Track(
        id = id, contentUri = "content://media/$id", title = title,
        artist = "Artist", album = "Album", albumArtUri = null, genre = "Rock",
        duration = 200000, trackNumber = null, year = null, mimeType = "audio/mpeg",
        folderPath = "/Music", metadataComplete = true
    )
}
