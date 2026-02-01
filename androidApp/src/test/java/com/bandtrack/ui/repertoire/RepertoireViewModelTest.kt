package com.bandtrack.ui.repertoire

import com.bandtrack.data.models.Song
import com.bandtrack.data.repository.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepertoireViewModelTest {

    private lateinit var viewModel: RepertoireViewModel
    private lateinit var fakeRepository: SongRepository
    private val testDispatcher = UnconfinedTestDispatcher() // Unconfined est souvent plus simple pour les StateFlows

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        fakeRepository = object : SongRepository() {
            override fun observeGroupSongs(groupId: String) = flowOf(
                listOf(
                    Song(id = "1", title = "Zombie", artist = "Cranberries", masteryLevels = mapOf("user1" to 8)),
                    Song(id = "2", title = "Africa", artist = "Toto", masteryLevels = mapOf("user1" to 5)),
                    Song(id = "3", title = "Wonderwall", artist = "Oasis", masteryLevels = mapOf("user1" to 10))
                )
            )
        }
        
        viewModel = RepertoireViewModel(fakeRepository)
        viewModel.initialize("group1", "user1")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `searchQuery filters songs`() = runTest {
        viewModel.onSearchQueryChanged("Toto")
        // Pas besoin de advanceUntilIdle avec UnconfinedTestDispatcher normalement, mais flow combine peut demander un petit coup de pouce
        // Ici initialize lance une coroutine sur viewModelScope qui utilise Main (testDispatcher).
        
        val state = viewModel.uiState.value
        assert(state is RepertoireUiState.Success)
        assertEquals(1, (state as RepertoireUiState.Success).songs.size)
        assertEquals("Africa", state.songs.first().title)
    }

    @Test
    fun `sort by mastery desc works`() = runTest {
        viewModel.onSortOptionChanged(SortOption.MASTERY_DESC)
        
        val state = viewModel.uiState.value
        assert(state is RepertoireUiState.Success)
        val songs = (state as RepertoireUiState.Success).songs
        assertEquals("Wonderwall", songs[0].title) // 10
        assertEquals("Zombie", songs[1].title) // 8
        assertEquals("Africa", songs[2].title) // 5
    }
}
