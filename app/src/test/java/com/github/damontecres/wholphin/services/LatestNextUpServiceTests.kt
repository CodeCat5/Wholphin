@file:UseSerializers(
    UUIDSerializer::class,
    DateTimeSerializer::class,
)

package com.github.damontecres.wholphin.services

import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.dedupeBySeries
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.operations.TvShowsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.DisplayPreferencesDto
import org.jellyfin.sdk.model.api.ScrollDirection
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.UserItemDataDto
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.serializer.DateTimeSerializer
import org.jellyfin.sdk.model.serializer.UUIDSerializer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

class LatestNextUpServiceTests {
    private val testDispatcher = StandardTestDispatcher()

    private val userId = UUID.randomUUID()
    private val seriesId1 = UUID.randomUUID()
    private val seriesId2 = UUID.randomUUID()
    private val seriesId3 = UUID.randomUUID()

    private val mockApi = mockk<ApiClient>(relaxed = true)
    private val mockTvShowsApi = mockk<TvShowsApi>()
    private val mockDatePlayedService = mockk<DatePlayedService>()
    private val mockDisplayPreferencesService = mockk<DisplayPreferencesService>()
    private val mockFavoriteWatchManager = mockk<FavoriteWatchManager>(relaxed = true)

    private val latestNextUpService =
        LatestNextUpService(mockApi, mockDatePlayedService, mockDisplayPreferencesService, mockFavoriteWatchManager)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { mockApi.tvShowsApi } returns mockTvShowsApi
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Test nothing is filtered out`() =
        runTest {
            coEvery { mockTvShowsApi.getNextUp(any() as GetNextUpRequest) } returns mockResponse
            coEvery { mockDisplayPreferencesService.getDisplayPreferences(any(), any(), any()) } returns
                buildRemoved()
            val result = latestNextUpService.getNextUp(userId, 20, false, false, 0)
            Assert.assertEquals(3, result.size)
            val seriesIds = result.map { it.data.seriesId }
            Assert.assertTrue(seriesIds.containsAll(listOf(seriesId1, seriesId2, seriesId3)))
        }

    @Test
    fun `Test seriesId1 is filtered out`() =
        runTest {
            coEvery { mockTvShowsApi.getNextUp(any() as GetNextUpRequest) } returns mockResponse
            coEvery { mockDisplayPreferencesService.getDisplayPreferences(any()) } returns
                buildRemoved(seriesId1 to LocalDateTime.now().minusDays(1))
            val result = latestNextUpService.getNextUp(userId, 20, false, false, 0)
            Assert.assertEquals(2, result.size)
            val seriesIds = result.map { it.data.seriesId }
            Assert.assertTrue(seriesId1 !in seriesIds)
            Assert.assertTrue(seriesIds.containsAll(listOf(seriesId2, seriesId3)))
        }

    @Test
    fun `Test seriesId2 is filtered out`() =
        runTest {
            coEvery { mockTvShowsApi.getNextUp(any() as GetNextUpRequest) } returns mockResponse
            coEvery { mockDisplayPreferencesService.getDisplayPreferences(any()) } returns
                buildRemoved(seriesId2 to LocalDateTime.now().minusDays(1))
            val result = latestNextUpService.getNextUp(userId, 20, false, false, 0)
            Assert.assertEquals(2, result.size)
            val seriesIds = result.map { it.data.seriesId }
            Assert.assertTrue(seriesId2 !in seriesIds)
            Assert.assertTrue(seriesIds.containsAll(listOf(seriesId1, seriesId3)))
        }

    @Test
    fun `Test seriesId1 and seriesId2 are filtered out`() =
        runTest {
            coEvery { mockTvShowsApi.getNextUp(any() as GetNextUpRequest) } returns mockResponse
            coEvery { mockDisplayPreferencesService.getDisplayPreferences(any()) } returns
                buildRemoved(
                    seriesId1 to LocalDateTime.now().minusDays(1),
                    seriesId2 to LocalDateTime.now().minusDays(1),
                )
            val result = latestNextUpService.getNextUp(userId, 20, false, false, 0)
            Assert.assertEquals(1, result.size)
            val seriesIds = result.map { it.data.seriesId }
            Assert.assertTrue(seriesId1 !in seriesIds)
            Assert.assertTrue(seriesId2 !in seriesIds)
            Assert.assertTrue(seriesIds.containsAll(listOf(seriesId3)))
        }

    @Test
    fun `Test dedupeBySeries drops later episodes from the same series`() {
        val firstSeriesEpisodeId = UUID.randomUUID()
        val laterSameSeriesId = UUID.randomUUID()
        val otherSeriesId = UUID.randomUUID()

        val firstSeriesEpisode =
            mockk<BaseItemDto>(relaxed = true) {
                every { id } returns firstSeriesEpisodeId
                every { seriesId } returns seriesId1
            }
        val laterSameSeries =
            mockk<BaseItemDto>(relaxed = true) {
                every { id } returns laterSameSeriesId
                every { seriesId } returns seriesId1
            }
        val otherSeries =
            mockk<BaseItemDto>(relaxed = true) {
                every { id } returns otherSeriesId
                every { seriesId } returns seriesId2
            }

        val input =
            listOf(
                BaseItem(firstSeriesEpisode),
                BaseItem(laterSameSeries),
                BaseItem(otherSeries),
            )

        val resultIds = input.dedupeBySeries().map { it.id }
        Assert.assertEquals(2, resultIds.size)
        Assert.assertTrue(firstSeriesEpisodeId in resultIds)
        Assert.assertTrue(otherSeriesId in resultIds)
        Assert.assertTrue("Later episode for same series should be dropped", laterSameSeriesId !in resultIds)
    }

    @Test
    fun `Test dedupeBySeries preserves all episodes when each is from a different series`() {
        val episode1Id = UUID.randomUUID()
        val episode2Id = UUID.randomUUID()
        val episode3Id = UUID.randomUUID()

        val episode1 =
            mockk<BaseItemDto>(relaxed = true) {
                every { id } returns episode1Id
                every { seriesId } returns seriesId1
            }
        val episode2 =
            mockk<BaseItemDto>(relaxed = true) {
                every { id } returns episode2Id
                every { seriesId } returns seriesId2
            }
        val episode3 =
            mockk<BaseItemDto>(relaxed = true) {
                every { id } returns episode3Id
                every { seriesId } returns seriesId3
            }

        val input = listOf(BaseItem(episode1), BaseItem(episode2), BaseItem(episode3))
        val result = input.dedupeBySeries()
        Assert.assertEquals(input.map { it.id }, result.map { it.id })
    }

    fun buildRemoved(vararg values: Pair<UUID, LocalDateTime>): DisplayPreferencesDto =
        testDisplayPreferencesDto.copy(
            customPrefs =
                mutableMapOf<String, String?>().apply {
                    val str = Json.encodeToString(RemovedSeriesIds(values.toMap()))
                    put(LatestNextUpService.REMOVED_KEY, str)
                },
        )

    private val testUserItemDataDto =
        UserItemDataDto(
            playbackPositionTicks = 0L,
            playCount = 0,
            isFavorite = false,
            lastPlayedDate = null,
            played = false,
            key = "",
            itemId = UUID.randomUUID(),
        )

    private val mockResponse: Response<BaseItemDtoQueryResult> =
        Response<BaseItemDtoQueryResult>(
            content =
                BaseItemDtoQueryResult(
                    items =
                        listOf(
                            mockk<BaseItemDto>(relaxed = true) {
                                every { seriesId } returns seriesId1
                                every { userData } returns testUserItemDataDto.copy(lastPlayedDate = LocalDateTime.now().minusDays(7))
                            },
                            mockk<BaseItemDto>(relaxed = true) {
                                every { seriesId } returns seriesId2
                                every { userData } returns testUserItemDataDto.copy(lastPlayedDate = null)
                            },
                            mockk<BaseItemDto>(relaxed = true) {
                                every { seriesId } returns seriesId3
                                every { userData } returns testUserItemDataDto.copy(lastPlayedDate = LocalDateTime.now().plusDays(7))
                            },
                        ),
                    totalRecordCount = 3,
                    startIndex = 0,
                ),
            status = 200,
            headers = mapOf(),
        )
}

val testDisplayPreferencesDto =
    DisplayPreferencesDto(
        id = "default",
        viewType = null,
        sortBy = null,
        indexBy = null,
        rememberIndexing = false,
        primaryImageHeight = 0,
        primaryImageWidth = 0,
        customPrefs = mapOf(),
        scrollDirection = ScrollDirection.VERTICAL,
        showBackdrop = false,
        rememberSorting = false,
        sortOrder = SortOrder.ASCENDING,
        showSidebar = false,
        client = null,
    )
