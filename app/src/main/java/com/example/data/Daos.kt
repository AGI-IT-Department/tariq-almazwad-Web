package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface YouthUserDao {
    @Query("SELECT * FROM users ORDER BY createdAt ASC")
    fun observeAllUsers(): Flow<List<YouthUser>>

    @Query("SELECT * FROM users WHERE LOWER(TRIM(fullName)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findByName(name: String): YouthUser?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): YouthUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: YouthUser)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<YouthUser>)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: String)
}

@Dao
interface ChallengeTaskDao {
    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC")
    fun observeAllTasks(): Flow<List<ChallengeTask>>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun countTasks(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: ChallengeTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<ChallengeTask>)
}

@Dao
interface TaskSubmissionDao {
    @Query("SELECT * FROM submissions ORDER BY submittedAt DESC")
    fun observeAllSubmissions(): Flow<List<TaskSubmission>>

    @Query(
        "SELECT * FROM submissions WHERE userId = :userId AND taskId = :taskId AND periodKey = :periodKey LIMIT 1"
    )
    suspend fun findForPeriod(userId: String, taskId: String, periodKey: String): TaskSubmission?

    @Query("SELECT * FROM submissions WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TaskSubmission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(submission: TaskSubmission)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(submissions: List<TaskSubmission>)

    @Query(
        "UPDATE submissions SET status = 'approved', adminNote = 'تمت الموافقة الجماعية من الخادم', reviewedAt = :reviewedAt WHERE status = 'pending'"
    )
    suspend fun approveAllPendingLocal(reviewedAt: Long)

    @Query("UPDATE submissions SET userName = :newName WHERE userId = :userId")
    suspend fun updateUserName(userId: String, newName: String)

    @Query("DELETE FROM submissions WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    fun observeConfig(): Flow<AppConfigEntity?>

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): AppConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: AppConfigEntity)
}
