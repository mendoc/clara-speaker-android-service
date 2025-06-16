package pro.ongoua.claraspeaker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summaries")
data class Summary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    var isPlayed: Boolean = false
)