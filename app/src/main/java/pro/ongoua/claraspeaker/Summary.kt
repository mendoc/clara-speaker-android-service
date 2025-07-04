// Dans votre fichier Summary.kt
package pro.ongoua.claraspeaker

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summaries")
data class Summary(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "summary_text")
    val text: String,

    @ColumnInfo(name = "is_played")
    var isPlayed: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "voice_model")
    var voiceModel: String?,

    @ColumnInfo(name = "audio_file_path")
    var audioFilePath: String?
)