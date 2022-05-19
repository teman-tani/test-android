package com.example.capstoneproject.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.raassh.dicodingstoryapp.data.database.RemoteKeyEntity

@Dao
interface RemoteKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemoteKeys(remoteKeys: List<RemoteKeyEntity>)

    @Query("SELECT * FROM remote_keys WHERE id = :id")
    suspend fun getRemoteKeyById(id: String): RemoteKeyEntity?

    @Query("DELETE FROM remote_keys")
    suspend fun deleteAllRemoteKeys()
}