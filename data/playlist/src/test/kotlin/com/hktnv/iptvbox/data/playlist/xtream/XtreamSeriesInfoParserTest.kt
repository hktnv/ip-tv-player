package com.hktnv.iptvbox.data.playlist.xtream

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class XtreamSeriesInfoParserTest {
    @Test
    fun parsesAddedTimestampFromBulkStreamPayload() {
        val entry = Json.parseToJsonElement(
            """
            {
              "stream_id": 42,
              "name": "Movie",
              "stream_icon": "http://image.test/movie.jpg",
              "rating": "7.4",
              "tmdb_id": 123,
              "added": "1710000000"
            }
            """.trimIndent(),
        ).jsonObject.toXtreamBulkEntry("stream_id", listOf("stream_icon"))

        requireNotNull(entry)
        assertEquals(42, entry.xtreamId)
        assertEquals("Movie", entry.title)
        assertEquals("http://image.test/movie.jpg", entry.posterUrl)
        assertEquals("7.4", entry.rating)
        assertEquals(123, entry.tmdbId)
        assertEquals(1_710_000_000L, entry.addedAtEpochSeconds)
    }

    @Test
    fun parsesSeasonEpisodeDetailsFromSeriesInfoPayload() {
        val payload = Json.parseToJsonElement(
            """
            {
              "info": {"plot": "Series plot", "backdrop_path": ["http://image.test/backdrop.jpg"]},
              "episodes": {
                "2": [
                  {
                    "episode_num": 3,
                    "title": "Episode title",
                    "info": {
                      "plot": "Episode plot",
                      "movie_image": "http://image.test/episode.jpg"
                    }
                  }
                ]
              }
            }
            """.trimIndent(),
        ).jsonObject.toSeriesInfoPayload()

        assertEquals("Series plot", payload.metadata.plot)
        assertEquals("http://image.test/backdrop.jpg", payload.metadata.backdropUrl)
        assertEquals(1, payload.episodes.size)
        assertEquals(2, payload.episodes.first().seasonNumber)
        assertEquals(3, payload.episodes.first().episodeNumber)
        assertEquals("Episode title", payload.episodes.first().title)
        assertEquals("Episode plot", payload.episodes.first().plot)
        assertEquals("http://image.test/episode.jpg", payload.episodes.first().imageUrl)
    }
}
