package com.moonplayer.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.moonplayer.app.data.dao.PlaylistDao
import com.moonplayer.app.data.dao.SongDao
import com.moonplayer.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) {
    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()
    fun getFavorites(): Flow<List<Song>> = songDao.getFavorites()
    fun search(query: String): Flow<List<Song>> = songDao.search(query)
    fun getByArtist(artist: String): Flow<List<Song>> = songDao.getByArtist(artist)
    fun getByAlbum(albumId: Long): Flow<List<Song>> = songDao.getByAlbum(albumId)
    fun getPlaylists(): Flow<List<Playlist>> = playlistDao.getAll()
    fun getPlaylistSongs(id: Long): Flow<List<Song>> = playlistDao.getSongs(id)

    fun getArtists(): Flow<List<Artist>> = songDao.getAllSongs().map { songs ->
        songs.groupBy { it.artist }.map { (name, list) ->
            Artist(name, list.size, list.map { it.albumId }.distinct().size)
        }.sortedBy { it.name.lowercase() }
    }

    fun getAlbums(): Flow<List<Album>> = songDao.getAllSongs().map { songs ->
        songs.groupBy { it.albumId }.map { (id, list) ->
            val first = list.first()
            Album(id, first.album, first.artist, list.size, first.year)
        }.sortedBy { it.name.lowercase() }
    }

    fun getFolders(): Flow<List<Folder>> = songDao.getAllSongs().map { songs ->
        songs.groupBy { it.path.substringBeforeLast('/') }.map { (path, list) ->
            Folder(path, path.substringAfterLast('/'), list.size)
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun scanLibrary() {
        withContext(Dispatchers.IO) {
            val songs = mutableListOf<Song>()
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.MIME_TYPE
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            try {
                context.contentResolver.query(
                    collection, projection, selection, null, null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                    val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val path = cursor.getString(dataCol) ?: continue
                        val mime = cursor.getString(mimeCol) ?: ""
                        val format = when {
                            mime.contains("mpeg") || mime.contains("mp3") -> "MP3"
                            mime.contains("flac") -> "FLAC"
                            mime.contains("aac") || mime.contains("mp4") || mime.contains("m4a") -> "AAC/M4A"
                            mime.contains("ogg") || mime.contains("opus") -> "OGG/OPUS"
                            mime.contains("wav") -> "WAV"
                            else -> mime.substringAfterLast('/').uppercase()
                        }
                        songs.add(
                            Song(
                                id = id,
                                title = cursor.getString(titleCol) ?: path.substringAfterLast('/'),
                                artist = cursor.getString(artistCol) ?: "Artista desconhecido",
                                album = cursor.getString(albumCol) ?: "Álbum desconhecido",
                                albumId = cursor.getLong(albumIdCol),
                                duration = cursor.getLong(durationCol),
                                path = path,
                                uri = ContentUris.withAppendedId(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    id
                                ).toString(),
                                size = cursor.getLong(sizeCol),
                                dateAdded = cursor.getLong(dateCol),
                                year = cursor.getInt(yearCol),
                                trackNumber = cursor.getInt(trackCol) % 1000,
                                format = format
                            )
                        )
                    }
                }
                songDao.clearAll()
                if (songs.isNotEmpty()) {
                    songDao.insertAll(songs)
                }
                Unit
            } catch (e: Exception) {
                Log.e("MusicRepository", "Scan failed", e)
            }
        }
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) {
        songDao.setFavorite(id, favorite)
    }

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insert(Playlist(name = name))
    }

    suspend fun renamePlaylist(playlist: Playlist) {
        playlistDao.update(playlist)
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.clearSongs(playlist.id)
        playlistDao.delete(playlist)
    }

    suspend fun addToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.addSong(PlaylistSong(playlistId, songId))
    }

    suspend fun removeFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSong(playlistId, songId)
    }
}