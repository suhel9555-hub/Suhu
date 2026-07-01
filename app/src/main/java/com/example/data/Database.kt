package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    fun getAllProfilesFlow(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE isUser = 0")
    fun getAllOtherProfilesFlow(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE isUser = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<Profile?>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Int): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    @Update
    suspend fun updateProfile(profile: Profile)

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY timestamp DESC")
    fun getAllMatchesFlow(): Flow<List<Match>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: Match): Long

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun deleteMatch(id: Int)

    @Query("DELETE FROM matches")
    suspend fun deleteAll()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE matchId = :matchId ORDER BY timestamp ASC")
    fun getMessagesForMatchFlow(matchId: Int): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}

@Dao
interface GroupChatDao {
    @Query("SELECT * FROM group_chats ORDER BY timestamp DESC")
    fun getAllGroupChatsFlow(): Flow<List<GroupChat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupChat(groupChat: GroupChat): Long

    @Query("DELETE FROM group_chats")
    suspend fun deleteAll()
}

@Dao
interface GroupMessageDao {
    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getMessagesForGroupFlow(groupId: Int): Flow<List<GroupMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMessage(groupMessage: GroupMessage): Long

    @Query("DELETE FROM group_messages")
    suspend fun deleteAll()
}

@Database(entities = [Profile::class, Match::class, Message::class, GroupChat::class, GroupMessage::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun matchDao(): MatchDao
    abstract fun messageDao(): MessageDao
    abstract fun groupChatDao(): GroupChatDao
    abstract fun groupMessageDao(): GroupMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "find_correct_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        // Preload profiles for a live matching experience
        suspend fun seedDatabase(profileDao: ProfileDao) {
            val count = profileDao.getAllProfilesFlow().toString() // Let's check if user profile exists
            // Since getAllProfilesFlow is cold, we'll do direct suspend query or populate if empty
        }
    }
}
